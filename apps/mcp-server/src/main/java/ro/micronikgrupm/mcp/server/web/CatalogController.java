package ro.micronikgrupm.mcp.server.web;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ro.micronikgrupm.mcp.server.tooling.DynamicToolCallbackRegistry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Unified catalog endpoint to expose all DIRECT tools that the MCP server provides
 * via core beans and uploaded plugins. Does not include the dynamic-expression tools
 * from DynamicToolRegistry (those are listed under /api/tools).
 */
@Slf4j
@RestController
@RequestMapping(path = "/api/catalog", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CatalogController {

    private final DynamicToolCallbackRegistry registry;

    @GetMapping("/tools")
    public List<ToolInfo> listAllDirectTools() {
        List<ToolInfo> result = new ArrayList<>();

        // Core tool objects
        for (Object obj : registry.listAllToolObjects()) {
            // Determine source: core or plugin (best-effort by scanning plugin lists)
            String source = isFromPlugin(obj) ? findPluginIdFor(obj) : "core";
            for (Method m : obj.getClass().getDeclaredMethods()) {
                Tool ann = m.getAnnotation(Tool.class);
                if (ann != null) {
                    ToolInfo info = new ToolInfo();
                    info.setName(ann.name());
                    info.setDescription(ann.description());
                    info.setClassName(obj.getClass().getName());
                    info.setMethodName(m.getName());
                    info.setSource(source);
                    result.add(info);
                }
            }
        }

        return result;
    }

    private boolean isFromPlugin(Object obj) {
        return registry.listPlugins().stream().anyMatch(p -> p.getToolObjects().contains(obj));
    }

    private String findPluginIdFor(Object obj) {
        return registry.listPlugins().stream()
                .filter(p -> p.getToolObjects().contains(obj))
                .map(DynamicToolCallbackRegistry.RegisteredPlugin::getPluginId)
                .findFirst().orElse("core");
    }

    @Data
    public static class ToolInfo {
        private String name;
        private String description;
        private String className;
        private String methodName;
        /**
         * "core" or "plugin:<id>" (best-effort; if plugin id is known, it's returned as just the id)
         */
        private String source;
    }
}
