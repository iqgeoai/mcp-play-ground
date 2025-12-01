package ro.micronikgrupm.mcp.server.web;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ro.micronikgrupm.mcp.server.tooling.PluginLoaderService;

import java.util.List;

@RestController
@RequestMapping(path = "/api/plugins", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PluginController {

    private final PluginLoaderService loaderService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PluginLoaderService.PluginDescriptor> upload(@RequestPart("file") FilePart file) {
        return loaderService.uploadAndLoad(file);
    }

    @GetMapping
    public List<PluginLoaderService.PluginDescriptor> list() {
        return loaderService.list();
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unload(@PathVariable("id") String id, @RequestParam(name = "deleteJar", defaultValue = "false") boolean deleteJar) {
        loaderService.unload(id, deleteJar);
    }
}
