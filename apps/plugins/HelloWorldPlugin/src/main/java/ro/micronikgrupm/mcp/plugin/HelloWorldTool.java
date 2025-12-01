package ro.micronikgrupm.mcp.plugin;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class HelloWorldTool {

    // Log immediately when the plugin class is instantiated by the server's PluginLoaderService
    public HelloWorldTool() {
        log.debug("HelloWorldTool loaded (instance created)");
    }

    @Tool(name = "helloWorld", description = "A simple tool that returns a hello world message.")
    public String helloWorld() {
        log.debug("helloWorld called");
        System.out.println(
                "HelloWorldTool: helloWorld method invoked"
        );
        return "Hello, World!";
    }

    @Tool(name = "greetUser", description = "Greets the user with the provided name.")
    public String greetUser(String name) {
        log.debug("greetUser called with name: {}", name);
        System.out.println("greetUser called with name: " + name);
        return "Hello, " + name + "!";
    }

    @Tool(name = "multiplyNumbers", description = "Multiplies two integer numbers together.")
    public int multiplyNumbers(int a, int b) {
        log.debug("multiplyNumbers called with a: {}, b: {}", a, b);
        System.out.println("multiplyNumbers called with a: " + a + ", b: " + b);
        return a * b;
    }

}
