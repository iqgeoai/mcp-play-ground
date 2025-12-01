package ro.micronikgrupm.mcp.server.tooling;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Loads plugin JARs, discovers classes that expose @Tool methods, instantiates them,
 * and registers the instances into the DynamicToolCallbackRegistry so their tools
 * become available immediately through the MCP server.
 */
@Slf4j
@Service
public class PluginLoaderService {

    private final DynamicToolCallbackRegistry registry;

    private final Map<String, LoadedPlugin> loaded = new ConcurrentHashMap<>();

    private final Path pluginsDir;

    public PluginLoaderService(@Value("${mcp.plugins.dir:./data/plugins}") String dir,
                               DynamicToolCallbackRegistry registry) throws IOException {
        this.registry = registry;
        this.pluginsDir = Path.of(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.pluginsDir);
        log.info("Plugins directory set to {}", this.pluginsDir);
    }

    public Path getPluginsDir() {
        return pluginsDir;
    }

    public Mono<PluginDescriptor> uploadAndLoad(FilePart filePart) {
        String original = filePart.filename();
        String pluginId = sanitizeId(original) + "-" + UUID.randomUUID().toString().substring(0, 8);
        Path target = pluginsDir.resolve(pluginId + ".jar");

        return filePart.transferTo(target)
                .then(Mono.fromCallable(() -> loadFromPath(pluginId, target)))
                .onErrorResume(e -> {
                    log.error("Failed to upload/load plugin {}: {}", original, e.getMessage(), e);
                    return Mono.error(e);
                });
    }

    public PluginDescriptor loadFromPath(String pluginId, Path jarPath) throws IOException {
        if (!Files.exists(jarPath)) {
            throw new IOException("JAR not found: " + jarPath);
        }
        if (loaded.containsKey(pluginId)) {
            throw new IllegalStateException("Plugin already loaded: " + pluginId);
        }
        URLClassLoader cl = new URLClassLoader(new URL[]{jarPath.toUri().toURL()}, this.getClass().getClassLoader());
        List<Class<?>> candidateClasses = findClassesWithToolMethods(jarPath, cl);
        List<Object> instances = new ArrayList<>();
        for (Class<?> clazz : candidateClasses) {
            try {
                Constructor<?> ctor = clazz.getDeclaredConstructor();
                ctor.setAccessible(true);
                Object obj = ctor.newInstance();
                instances.add(obj);
            } catch (NoSuchMethodException ex) {
                log.warn("Skipping class {} (no default constructor)", clazz.getName());
            } catch (Throwable t) {
                log.warn("Failed to instantiate {}: {}", clazz.getName(), t.getMessage());
            }
        }
        if (!instances.isEmpty()) {
            registry.registerObjects(pluginId, instances);
        }
        LoadedPlugin lp = new LoadedPlugin(pluginId, jarPath, cl, instances, Instant.now());
        loaded.put(pluginId, lp);
        log.info("Loaded plugin {} from {} with {} tool object(s)", pluginId, jarPath.getFileName(), instances.size());
        return toDescriptor(lp);
    }

    public List<PluginDescriptor> list() {
        List<PluginDescriptor> list = new ArrayList<>();
        for (LoadedPlugin lp : loaded.values()) {
            list.add(toDescriptor(lp));
        }
        list.sort(Comparator.comparing(PluginDescriptor::getLoadedAt));
        return list;
    }

    public boolean unload(String pluginId, boolean deleteJar) {
        LoadedPlugin lp = loaded.remove(pluginId);
        if (lp == null) return false;
        try {
            registry.unregisterPlugin(pluginId);
        } catch (Exception e) {
            log.warn("Error unregistering plugin {}: {}", pluginId, e.getMessage());
        }
        try {
            lp.classLoader.close();
        } catch (IOException e) {
            log.warn("Error closing classloader for plugin {}: {}", pluginId, e.getMessage());
        }
        if (deleteJar) {
            try {
                Files.deleteIfExists(lp.jarPath);
            } catch (IOException e) {
                log.warn("Error deleting JAR for plugin {}: {}", pluginId, e.getMessage());
            }
        }
        log.info("Unloaded plugin {}", pluginId);
        return true;
    }

    private List<Class<?>> findClassesWithToolMethods(Path jarPath, ClassLoader cl) throws IOException {
        List<Class<?>> classes = new ArrayList<>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry e = entries.nextElement();
                if (e.isDirectory()) continue;
                String name = e.getName();
                if (!name.endsWith(".class")) continue;
                String className = name.replace('/', '.').substring(0, name.length() - 6);
                try {
                    Class<?> clazz = Class.forName(className, false, cl);
                    if (hasToolMethods(clazz)) {
                        classes.add(clazz);
                    }
                } catch (NoClassDefFoundError | ClassNotFoundException ex) {
                    log.debug("Cannot load class {} from plugin {}: {}", className, jarPath.getFileName(), ex.getMessage());
                }
            }
        }
        return classes;
    }

    private boolean hasToolMethods(Class<?> clazz) {
        for (Method m : clazz.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Tool.class)) return true;
        }
        return false;
    }

    private String sanitizeId(String filename) {
        String base = filename == null ? "plugin" : filename.replaceAll("\\.jar$", "");
        return base.replaceAll("[^a-zA-Z0-9-_]", "-");
    }

    @Data
    public static class PluginDescriptor {
        private final String id;
        private final String jar;
        private final int objectCount;
        private final Instant loadedAt;
    }

    @Data
    private static class LoadedPlugin {
        private final String id;
        private final Path jarPath;
        private final URLClassLoader classLoader;
        private final List<Object> instances;
        private final Instant loadedAt;
    }

    private PluginDescriptor toDescriptor(LoadedPlugin lp) {
        return new PluginDescriptor(lp.id, lp.jarPath.getFileName().toString(), lp.instances.size(), lp.loadedAt);
    }
}
