package ro.micronikgrupm.mcp.client.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ro.micronikgrupm.mcp.client.services.DynamicToolCatalogClient;
import org.springframework.ai.tool.ToolCallbackProvider;
// import removed; we don't need to introspect callback names at compile time

import java.util.stream.Collectors;

/**
 * REST Controller to interact with the LLM and its tools.
 */
@Slf4j
@RestController
public class AIChatController {

    private final ChatClient chatClient;
    private final DynamicToolCatalogClient toolCatalogClient;
    private final ToolCallbackProvider mcpToolProvider;

    public AIChatController(ChatClient chatClient, DynamicToolCatalogClient toolCatalogClient, ToolCallbackProvider mcpToolProvider) {
        this.chatClient = chatClient;
        this.toolCatalogClient = toolCatalogClient;
        this.mcpToolProvider = mcpToolProvider;
    }

    @GetMapping("/api/chat")
    public String chat(@RequestParam(value = "prompt", defaultValue = "What is 10 plus 5 and what time is it?") String prompt) {
        log.debug("--- New /api/chat request ---");
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

        log.debug("Dynamic tools summary: {}", dynamicToolsSummary);
        
        var toolsPlugins = toolCatalogClient.listPlugins();
        String pluginsSummary = toolsPlugins.isEmpty() ?
                "No plugins are currently loaded." :
                toolsPlugins.stream()
                        .map(p -> "- " + p.getId() + " v" + p.getJar())
                        .collect(Collectors.joining("\n"));

        log.debug("Plugins summary: {}", pluginsSummary);

        // Build a summary of DIRECT tools available right now (static + plugin) via server catalog
        var directTools = toolCatalogClient.listDirectTools();
        String directToolsSummary = (directTools == null || directTools.isEmpty()) ?
                "No direct tools are currently available." :
                directTools.stream()
                        .map(DynamicToolCatalogClient.ToolInfo::getName)
                        .distinct()
                        .sorted()
                        .collect(Collectors.joining(", "));

        log.debug("Direct tools available: {}", directToolsSummary);

        // Guidance: prefer direct tools by name (static + plugin). Use executeDynamic only for the dynamic list.
//        String helperInstruction = "Tool calling rules:\n" +
//                "1) If the capability you need matches a DIRECT tool name, call that tool directly (do NOT use executeDynamic).\n" +
//                "   Examples: addNumbers(a,b), getCurrentTime().\n" +
//                "2) Use `executeDynamic` ONLY for tools that appear in the Dynamic tools list below. Call it with:\n" +
//                "{\n  toolName: '<dynamic tool name>',\n  args: { <parameters shown for that tool> }\n}\n" +
//                "Example: If a dynamic tool named 'mul' expects [a,b], call `executeDynamic` with args {a:3,b:7}.";
//
//        String systemBlock = "Direct tools available now: " + directToolsSummary +
//                "\n\nDynamic tools available (managed at runtime via /api/tools):\n" + dynamicToolsSummary + " and loaded plugins:\n" + pluginsSummary +
//                "\n\nNote: Direct tools from the server (static + uploaded plugins) are also available and should be called by their exact names (e.g., greetUser, helloWorld, addNumbers, getCurrentTime). Do NOT use executeDynamic for those.\n\n"
//                + helperInstruction;
//
//        String cleanMessage = prompt + ". Respond only with the final factual answer.";

        String helperInstruction = "When you need to use a dynamic tool, call the tool `executeDynamic` with:\n" +
                "{\n  toolName: '<one of the dynamic tools names>',\n  args: { <use the parameters listed for that tool> }\n}\n" +
                "Examples: to multiply with a dynamic tool named 'mul' expecting [a,b], call executeDynamic with args {a: 3, b: 7}.\n" +
                "Static tools like `addNumbers(a,b)` are available directly as well.";

//        String systemBlock = "Dynamic tools available (managed at runtime):\n" + dynamicToolsSummary + "\n\n" + helperInstruction;
//
//        String cleanMessage = prompt + ". Respond only with the final factual answer.";

        String cleanMessage = prompt + ". Respond only with the factual answer, without any analysis or commentary on tools.";

        // IMPORTANT: refresh tool callbacks on every request to pick up newly uploaded plugin tools
        return chatClient.prompt()
//                .system(systemBlock)
                .user(cleanMessage)
//                .toolCallbacks(mcpToolProvider.getToolCallbacks())
                .call()
                .content();
    }
}
