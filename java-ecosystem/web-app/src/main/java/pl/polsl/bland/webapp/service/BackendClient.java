package pl.polsl.bland.webapp.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Service
public class BackendClient {
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final String backendBaseUrl;

    public BackendClient(
            ObjectMapper objectMapper,
            @Value("${app.backend.base-url:http://localhost:8080/api}") String backendBaseUrl) {
        this.objectMapper = objectMapper;
        this.backendBaseUrl = backendBaseUrl;
    }

    public CircuitSchematic saveSchematic(CircuitSchematic schematic) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendBaseUrl + "/schematics"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(schematic), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 201 && response.statusCode() != 200) {
                throw new IllegalStateException("Backend odrzucił zapis schematu: " + response.body());
            }
            return objectMapper.readValue(response.body(), CircuitSchematic.class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nie udało się zserializować schematu do JSON.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się skontaktować z backendem podczas zapisu schematu.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Przerwano zapis schematu do backendu.", exception);
        }
    }

    public String runSimulationCsv(SimulationRequest requestPayload) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(backendBaseUrl + "/simulate"))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "text/csv")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestPayload), StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                String body = response.body() == null || response.body().isBlank()
                        ? "Backend nie zwrócił opisu błędu."
                        : response.body();
                throw new IllegalStateException("Backend nie uruchomił symulacji: " + body);
            }
            return response.body();
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nie udało się zserializować żądania symulacji.", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Nie udało się skontaktować z backendem podczas symulacji.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Przerwano komunikację z backendem podczas symulacji.", exception);
        }
    }
}
