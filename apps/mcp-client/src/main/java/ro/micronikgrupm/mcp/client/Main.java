package ro.micronikgrupm.mcp.client;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Main {
    public static void main(String[] args) {
        SpringApplication.run(Main.class, args);
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ToolCallbackProvider mcpToolProvider) {
        SystemMessage systemMessage = new SystemMessage(
                "You are a helpful and concise assistant. Only respond with the final answer. " +
                        "DO NOT include any internal commentary, analysis, or explanations about your thought process, tools, or knowledge source."
        );
        return builder
                .defaultSystem(systemMessage.getText()) // Or use a PromptTemplate for more complex prompts
                .defaultToolCallbacks(mcpToolProvider.getToolCallbacks())
                .build();
    }
}
