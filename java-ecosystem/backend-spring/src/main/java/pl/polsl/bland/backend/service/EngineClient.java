package pl.polsl.bland.backend.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Service
public class EngineClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String ENGINE_URL = "http://localhost:8081/api/v1/simulate";

    public String simulate(String netlist) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(ENGINE_URL))
                    .header("Content-Type", "text/plain; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(netlist))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new EngineException(response.statusCode(), response.body());
            }

            return response.body();
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
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
