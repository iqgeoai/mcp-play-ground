package ro.micronikgrupm.mcp.server.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalTime;

@Slf4j
@Service
public class TestTimeMathService {

    /**
     * Tool function to get the current local time.
     * The description is essential as it guides the LLM on when to call the tool.
     * @return The current time in HH:mm:ss format.
     */
    @Tool(name = "getCurrentTime", description = "Get the current local time in the user's location, helpful when the user asks for the time.")
    public String getCurrentTime() {
        log.debug("getCurrentTime called");
        return LocalTime.now().toString();
    }

    /**
     * Tool function for adding two numbers.
     * @param a The first number.
     * @param b The second number.
     * @return The sum of the two numbers.
     */
    @Tool(name = "addNumbers", description = "Adds two integer numbers together. Use this tool for simple arithmetic problems.")
    public int addNumbers(int a, int b) {
        log.debug("addNumbers called");
        return a + b;
    }
}
