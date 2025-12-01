package ro.micronikgrupm.mcp.server.tooling;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Runtime registry of ToolCallbacks grouped by plugin. Allows adding/removing
 * tools loaded from external JARs without restarting the server.
 *
 * Note: this class does NOT implement ToolCallbackProvider to avoid premature
 * coupling to Spring AI internals. Use {@link #listAllToolObjects()} to retrieve
 * the merged set if you need to expose them.
 */
@Slf4j
@Component
public class DynamicToolCallbackRegistry {

    private final Map<String, RegisteredPlugin> plugins = new ConcurrentHashMap<>();

    /**
     * Core (non-plugin) tool objects such as TestTimeMathService or DynamicExecutorService.
     * Keyed by class name to avoid duplicate registrations.
     */
    private final Map<String, Object> coreToolObjects = new ConcurrentHashMap<>();

    /** Merge all registered callbacks across core and plugins. */
    public List<Object> listAllToolObjects() {
        List<Object> all = new ArrayList<>(coreToolObjects.values());
        all.addAll(
                plugins.values().stream()
                        .flatMap(p -> p.toolObjects.stream())
                        .collect(Collectors.toList())
        );
        return all;
    }

    /** Register a core (non-plugin) tool object. Safe to call multiple times; de-duplicates by class name. */
    public void registerCore(Object toolObject) {
        Objects.requireNonNull(toolObject, "toolObject");
        coreToolObjects.computeIfAbsent(toolObject.getClass().getName(), k -> {
            log.info("Registered core tool object: {}", k);
            return toolObject;
        });
    }

    /** Register multiple core tool objects at once. */
    public void registerCores(List<Object> toolObjects) {
        if (toolObjects == null || toolObjects.isEmpty()) return;
        for (Object o : toolObjects) {
            registerCore(o);
        }
    }

    public void registerFromObject(String pluginId, Object toolObject) {
        Objects.requireNonNull(pluginId, "pluginId");
        Objects.requireNonNull(toolObject, "toolObject");
        plugins.compute(pluginId, (id, existing) -> {
            if (existing == null) existing = new RegisteredPlugin(id);
            existing.toolObjects.add(toolObject);
            return existing;
        });
        log.info("Registered tool object for plugin {} from class {}", pluginId, toolObject.getClass().getName());
    }

    public void registerObjects(String pluginId, List<Object> objects) {
        if (objects == null || objects.isEmpty()) return;
        plugins.compute(pluginId, (id, existing) -> {
            if (existing == null) existing = new RegisteredPlugin(id);
            existing.toolObjects.addAll(objects);
            return existing;
        });
        log.info("Registered {} tool object(s) for plugin {}", objects.size(), pluginId);
    }

    public void unregisterPlugin(String pluginId) {
        var removed = plugins.remove(pluginId);
        if (removed != null) {
            log.info("Unregistered plugin {} and {} tool object(s)", pluginId, removed.toolObjects.size());
        }
    }

    public List<RegisteredPlugin> listPlugins() {
        return new ArrayList<>(plugins.values());
    }

    @Data
    public static class RegisteredPlugin {
        private final String pluginId;
        private final List<Object> toolObjects = new ArrayList<>();
    }
}
