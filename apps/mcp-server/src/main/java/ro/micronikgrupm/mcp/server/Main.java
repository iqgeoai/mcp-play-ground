package ro.micronikgrupm.mcp.server;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ro.micronikgrupm.mcp.server.service.TestTimeMathService;
import ro.micronikgrupm.mcp.server.tooling.DynamicExecutorService;

@SpringBootApplication(scanBasePackages = "ro.micronikgrupm.mcp")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ToolCallbackProvider tools(TestTimeMathService testTimeMathService,
                                     DynamicExecutorService dynamicExecutorService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(testTimeMathService, dynamicExecutorService) // Takes the objects with the @Tool methods
                .build();
    }
}
