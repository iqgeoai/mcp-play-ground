package ro.micronikgrupm.mcp.server;

import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ro.micronikgrupm.mcp.server.service.TestTimeMathService;

@SpringBootApplication(scanBasePackages = "ro.micronikgrupm.mcp")
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ToolCallbackProvider tools(TestTimeMathService testTimeMathService) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(testTimeMathService) // Takes the object with the @Tool methods
                .build();
    }
}
