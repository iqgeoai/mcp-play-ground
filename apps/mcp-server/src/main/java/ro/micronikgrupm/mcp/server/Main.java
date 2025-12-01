package ro.micronikgrupm.mcp.server;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ro.micronikgrupm.mcp.server.service.TestTimeMathService;
import ro.micronikgrupm.mcp.server.tooling.DynamicExecutorService;
import ro.micronikgrupm.mcp.server.tooling.DynamicToolCallbackRegistry;

import java.util.List;

@SpringBootApplication(scanBasePackages = "ro.micronikgrupm.mcp")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    /**
     * Register core (non-plugin) tool objects into the dynamic registry at startup.
     * This keeps a single source of truth for all tool objects (core + plugins).
     */
    @Bean
    public ApplicationRunner registerCoreTools(DynamicToolCallbackRegistry dynamicRegistry,
                                               TestTimeMathService testTimeMathService)
//                                               DynamicExecutorService dynamicExecutorService)
    {
//        return args -> dynamicRegistry.registerCores(List.of(testTimeMathService, dynamicExecutorService));
        return args -> dynamicRegistry.registerCores(List.of(testTimeMathService));
    }

    @Bean
    public ToolCallbackProvider tools(DynamicToolCallbackRegistry dynamicRegistry) {
        // Build callbacks fresh each time so newly uploaded plugins (and any core registrations) appear without restart
        return () -> MethodToolCallbackProvider.builder()
                .toolObjects(dynamicRegistry.listAllToolObjects().toArray())
                .build()
                .getToolCallbacks();
    }
}
