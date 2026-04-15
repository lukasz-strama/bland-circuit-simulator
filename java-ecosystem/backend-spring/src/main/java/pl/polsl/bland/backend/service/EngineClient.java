package pl.polsl.bland.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class EngineClient {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    private final String engineUrl;
    private final String apiKey;

    public EngineClient(
            @Value("${engine.url:http://localhost:8081/api/v1/simulate}") String engineUrl,
            @Value("${engine.api-key:supersecretkey123}") String apiKey) {
        this.engineUrl = engineUrl;
        this.apiKey = apiKey;
    }

    public String simulate(String netlist) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(engineUrl))
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .header("X-Engine-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(60))
                    .POST(HttpRequest.BodyPublishers.ofString(netlist))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new EngineException(response.statusCode(), response.body());
            }

            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new EngineException(500, "Engine request interrupted: " + e.getMessage());
        } catch (IOException e) {
            throw new EngineException(500, "Engine unreachable: " + e.getMessage());
        }
    }

    public static class EngineException extends RuntimeException {
        private final int statusCode;

        public EngineException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
