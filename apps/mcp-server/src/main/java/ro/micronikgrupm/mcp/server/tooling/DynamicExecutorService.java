package ro.micronikgrupm.mcp.server.tooling;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Provides a generic dynamic-execution tool that evaluates simple expressions
 * registered at runtime via DynamicToolRegistry. This gives the LLM a stable
 * tool surface while allowing admins to add new dynamic tools without code changes.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicExecutorService {

    private final DynamicToolRegistry registry;
    private final ExpressionParser parser = new SpelExpressionParser();

    @Tool(name = "executeDynamic", description = "Execute a dynamically registered tool by name using provided arguments. Useful for runtime-extensible operations.")
    public Object executeDynamic(String toolName, Map<String, Object> args) {
        if (toolName == null || toolName.isBlank()) {
            throw new IllegalArgumentException("toolName is required");
        }
        var tool = registry.exists(toolName) ? registry.list().stream()
                .filter(t -> t.getName().equals(toolName))
                .findFirst().orElse(null) : null;
        if (tool == null) {
            throw new IllegalArgumentException("Unknown dynamic tool: " + toolName);
        }

        // For now support expression-based tools using SpEL.
        if (tool.getExpression() == null || tool.getExpression().isBlank()) {
            throw new IllegalStateException("Dynamic tool has no expression to execute");
        }

        StandardEvaluationContext context = new StandardEvaluationContext();
        if (args != null) {
            args.forEach(context::setVariable);
        }
        Expression expression = parser.parseExpression(tool.getExpression());
        Object result = expression.getValue(context);
        log.debug("executeDynamic '{}' with args {} => {}", toolName, args, result);
        return result;
    }
}
