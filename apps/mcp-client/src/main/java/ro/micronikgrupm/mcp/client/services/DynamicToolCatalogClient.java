package ro.micronikgrupm.mcp.client.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@Service
public class DynamicToolCatalogClient {

    private final WebClient webClient;

    public DynamicToolCatalogClient(@Value("${mcp.server.base-url:http://localhost:8080}") String serverBaseUrl) {
        this.webClient = WebClient.builder()
                .baseUrl(serverBaseUrl)
                .build();
    }

    public List<DynamicTool> listTools() {
        try {
            return webClient.get()
                    .uri("/api/tools")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .bodyToFlux(DynamicTool.class)
                    .collectList()
                    .onErrorResume(e -> Mono.just(Collections.emptyList()))
                    .block();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Data
    public static class DynamicTool {
        private String name;
        private String description;
        private String expression;
        private List<String> parameters;
        private String createdAt;
        private String updatedAt;
    }
}
