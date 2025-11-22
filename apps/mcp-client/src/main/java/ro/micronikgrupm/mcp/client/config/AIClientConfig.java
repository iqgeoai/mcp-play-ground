package ro.micronikgrupm.mcp.client.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Minimal configuration to provide a ChatClient.Builder bean backed by Ollama.
 *
 * This satisfies the "Consider defining a bean of type ChatClient.Builder" requirement
 * and allows other components (e.g., AIChatController) to build a ChatClient.
 */
@Configuration
public class AIClientConfig {

    // With Spring AI Boot starters on the classpath, ChatModel and ChatClient.Builder
    // are auto-configured. This class intentionally contains no explicit bean definitions
    // to avoid conflicting with auto-configuration while keeping a placeholder for
    // future customizations if needed.
}
