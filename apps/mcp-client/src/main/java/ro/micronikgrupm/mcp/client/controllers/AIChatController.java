package ro.micronikgrupm.mcp.client.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
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

    public AIChatController(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @GetMapping("/api/chat")
    public String chat(@RequestParam(value = "prompt", defaultValue = "What is 10 plus 5 and what time is it?") String prompt) {

        log.debug("Received prompt: {}",prompt);
        String cleanMessage = prompt + ". Respond only with the factual answer, without any analysis or commentary on tools.";

        return chatClient.prompt()
                .user(cleanMessage)
                .call()
                .content();
    }
}
