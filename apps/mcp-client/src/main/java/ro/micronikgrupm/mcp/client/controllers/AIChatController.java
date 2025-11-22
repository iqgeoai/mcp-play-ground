package ro.micronikgrupm.mcp.client.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller to interact with the LLM and its tools.
 */
@Slf4j
@RestController
public class AIChatController {

    private final ChatClient chatClient;
    private final SyncMcpToolCallbackProvider mcpToolProvider;

    /**
     * Injects the MCP Tool Provider configured from application.properties (mcp-playground-server).
     * The ChatClient is built to include all tools provided by the MCP Client.
     */
    public AIChatController(ChatClient.Builder chatClientBuilder,
                            @Qualifier("mcp-playground-server-callback-tool-provider") SyncMcpToolCallbackProvider mcpToolProvider) {

        this.mcpToolProvider = mcpToolProvider;

        // Build the ChatClient, making the tools available to the LLM.
        // The deprecated SystemPromptAdvisor is replaced by the standard .defaultSystem() on the builder.
        this.chatClient = chatClientBuilder
                // Use the modern .defaultSystem() to set global system instructions
                .defaultSystem("You are a helpful assistant. Use your available tools if necessary.")
                // Add the tools discovered from the MCP Server to the ChatClient
                .defaultTools(mcpToolProvider)
                .build();
    }

    @GetMapping("/api/chat")
    public String chat(@RequestParam(value = "prompt", defaultValue = "What is 10 plus 5 and what time is it?") String prompt) {

        log.debug("Received prompt: {}",prompt);

        // Call the LLM with the prompt. The system prompt set in the constructor is
        // automatically included here.
        return chatClient.prompt()
                .user(prompt)
                .call()
                .content();
    }
}
