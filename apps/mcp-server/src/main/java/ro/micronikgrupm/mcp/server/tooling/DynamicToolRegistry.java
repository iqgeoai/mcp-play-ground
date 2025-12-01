package ro.micronikgrupm.mcp.server.tooling;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory registry for dynamically managed tools.
 * NOTE: This class currently stores tools metadata and simple expressions only.
 * A subsequent step can wire these into Spring AI's ToolCallbackProvider if desired.
 */
@Slf4j
@Component
public class DynamicToolRegistry {

    private final Map<String, DynamicTool> tools = new ConcurrentHashMap<>();

    public boolean exists(String name) {
        return tools.containsKey(name);
    }

    public DynamicTool add(DynamicTool tool) {
        Objects.requireNonNull(tool, "tool");
        if (tool.getName() == null || tool.getName().isBlank()) {
            throw new IllegalArgumentException("Tool name is required");
        }
        if (tools.putIfAbsent(tool.getName(), tool.withTimestampsIfMissing()) != null) {
            throw new IllegalStateException("Tool with name '" + tool.getName() + "' already exists");
        }
        log.info("Dynamic tool added: {}", tool.getName());
        return tool;
    }

    public DynamicTool upsert(DynamicTool tool) {
        Objects.requireNonNull(tool, "tool");
        if (tool.getName() == null || tool.getName().isBlank()) {
            throw new IllegalArgumentException("Tool name is required");
        }
        tool.withTimestampsIfMissing();
        tools.put(tool.getName(), tool.touchUpdated());
        log.info("Dynamic tool upserted: {}", tool.getName());
        return tool;
    }

    public DynamicTool remove(String name) {
        DynamicTool removed = tools.remove(name);
        if (removed != null) {
            log.info("Dynamic tool removed: {}", name);
        }
        return removed;
    }

    public List<DynamicTool> list() {
        return Collections.unmodifiableList(new ArrayList<>(tools.values()));
    }

    @Data
    public static class DynamicTool {
        private String name;
        private String description;
        /**
         * Optional simple SpEL-like expression (e.g., "#a + #b") the tool can evaluate later.
         * This is metadata only at this stage.
         */
        private String expression;
        /**
         * Optional plugin integration: if set, the tool will invoke a method from an uploaded JAR
         * instead of evaluating an expression. Provide the pluginId returned by the upload API,
         * the fully-qualified className, and the methodName to invoke.
         */
        private String pluginId;
        private String className;
        private String methodName;
        /**
         * A declared list of parameters this tool expects, for UI rendering/help purposes.
         */
        private List<String> parameters = new ArrayList<>();

        private Instant createdAt;
        private Instant updatedAt;

        DynamicTool withTimestampsIfMissing() {
            Instant now = Instant.now();
            if (this.createdAt == null) this.createdAt = now;
            if (this.updatedAt == null) this.updatedAt = now;
            return this;
        }

        DynamicTool touchUpdated() {
            this.updatedAt = Instant.now();
            return this;
        }
    }
}
