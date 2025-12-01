package ro.micronikgrupm.mcp.server.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ro.micronikgrupm.mcp.server.tooling.DynamicExecutorService;
import ro.micronikgrupm.mcp.server.tooling.DynamicToolRegistry;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping(path = "/api/tools", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DynamicToolController {

    private final DynamicToolRegistry registry;
    private final DynamicExecutorService executorService;

    @GetMapping
    public List<DynamicToolRegistry.DynamicTool> list() {
        return registry.list();
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public DynamicToolRegistry.DynamicTool add(@RequestBody DynamicToolRegistry.DynamicTool tool,
                                               @RequestParam(name = "mode", defaultValue = "add") String mode) {
        if ("upsert".equalsIgnoreCase(mode)) {
            return registry.upsert(tool);
        }
        return registry.add(tool);
    }

    @DeleteMapping(path = "/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable("name") String name) {
        registry.remove(name);
    }

    @PostMapping(path = "/{name}/execute", consumes = MediaType.APPLICATION_JSON_VALUE)
    public Object execute(@PathVariable("name") String name, @RequestBody(required = false) Map<String, Object> args) {
        return executorService.executeDynamic(name, args == null ? Map.of() : args);
    }
}
