package ro.micronikgrupm.mcp.client.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.micronikgrupm.mcp.client.services.DynamicToolCatalogClient;

import java.util.stream.Collectors;

/**
 * REST Controller to interact with the LLM and its tools.
 */
@Slf4j
@RestController
public class AIChatController {

    private final ChatClient chatClient;
    private final DynamicToolCatalogClient toolCatalogClient;

    public AIChatController(ChatClient chatClient, DynamicToolCatalogClient toolCatalogClient) {
        this.chatClient = chatClient;
        this.toolCatalogClient = toolCatalogClient;
    }

    @GetMapping("/api/chat")
    public String chat(@RequestParam(value = "prompt", defaultValue = "What is 10 plus 5 and what time is it?") String prompt) {

        log.debug("Received prompt: {}", prompt);

        // Fetch dynamic tools so the LLM knows how to use executeDynamic
        var tools = toolCatalogClient.listTools();
        String dynamicToolsSummary = tools.isEmpty() ?
                "No dynamic tools are currently registered." :
                tools.stream()
                        .map(t -> {
                            String params = (t.getParameters() == null || t.getParameters().isEmpty()) ?
                                    "(no params)" : String.join(", ", t.getParameters());
                            String desc = t.getDescription() == null ? "" : (" - " + t.getDescription());
                            return "- " + t.getName() + desc + " | params: [" + params + "]";
                        })
                        .collect(Collectors.joining("\n"));

        String helperInstruction = "When you need to use a dynamic tool, call the tool `executeDynamic` with:\n" +
                "{\n  toolName: '<one of the dynamic tools names>',\n  args: { <use the parameters listed for that tool> }\n}\n" +
                "Examples: to multiply with a dynamic tool named 'mul' expecting [a,b], call executeDynamic with args {a: 3, b: 7}.\n" +
                "Static tools like `addNumbers(a,b)` are available directly as well.";

        String systemBlock = "Dynamic tools available (managed at runtime):\n" + dynamicToolsSummary + "\n\n" + helperInstruction;

        String cleanMessage = prompt + ". Respond only with the final factual answer.";

        return chatClient.prompt()
                .system(systemBlock)
                .user(cleanMessage)
                .call()
                .content();
    }
}
