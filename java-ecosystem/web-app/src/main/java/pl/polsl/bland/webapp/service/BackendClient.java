package pl.polsl.bland.webapp.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.UserDto;
import pl.polsl.bland.models.Wire;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class BackendClient {
    private static final String JSON_CONTENT_TYPE = "application/json; charset=utf-8";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper;
    private final BackendAuthSession backendAuthSession;
    private final String backendBaseUrl;

    public BackendClient(
            ObjectMapper objectMapper,
            BackendAuthSession backendAuthSession,
            @Value("${app.backend.base-url:http://localhost:8080/api}") String backendBaseUrl) {
        this.objectMapper = objectMapper;
        this.backendAuthSession = backendAuthSession;
        this.backendBaseUrl = backendBaseUrl;
    }

    public boolean isAuthenticated() {
        return backendAuthSession.isAuthenticated();
    }

    public UserDto currentUser() {
        return backendAuthSession.user();
    }

    public UserDto login(String username, String password) {
        AuthResponse authResponse = sendJson(
                buildJsonRequest("/auth/login")
                        .POST(jsonBody(Map.of(
                                "username", requireText(username, "Podaj login użytkownika."),
                                "password", requireText(password, "Podaj hasło."))))
                        .build(),
                AuthResponse.class,
                "Nie udało się zalogować do backendu.");

        if (authResponse.token() == null || authResponse.token().isBlank() || authResponse.user() == null) {
            throw new IllegalStateException("Backend nie zwrócił poprawnego tokenu logowania.");
        }

        backendAuthSession.authenticate(authResponse.token(), authResponse.user());
        return authResponse.user();
    }

    public UserDto registerAndLogin(String username, String email, String password) {
        sendExpectingSuccess(
                buildJsonRequest("/auth/register")
                        .POST(jsonBody(Map.of(
                                "username", requireText(username, "Podaj login użytkownika."),
                                "email", requireText(email, "Podaj adres e-mail do rejestracji."),
                                "password", requireText(password, "Podaj hasło."))))
                        .build(),
                201,
                "Nie udało się założyć konta w backendzie.");

        return login(username, password);
    }

    public void logout() {
        backendAuthSession.clear();
    }

    public List<CircuitSchematic> listProjects() {
        ensureAuthenticated();

        ProjectResponse[] projectResponses = sendJson(
                buildAuthorizedJsonRequest("/projects")
                        .GET()
                        .build(),
                ProjectResponse[].class,
                "Nie udało się pobrać listy projektów z backendu.");

        return List.of(projectResponses).stream()
                .map(this::toCircuitSchematic)
                .toList();
    }

    public CircuitSchematic loadProject(Long projectId) {
        ensureAuthenticated();
        if (projectId == null) {
            throw new IllegalStateException("Wybierz projekt do wczytania.");
        }

        ProjectResponse projectResponse = sendJson(
                buildAuthorizedJsonRequest("/projects/" + projectId)
                        .GET()
                        .build(),
                ProjectResponse.class,
                "Nie udało się wczytać projektu z backendu.");

        return toCircuitSchematic(projectResponse);
    }

    public CircuitSchematic saveProject(Long projectId, CircuitSchematic schematic) {
        ensureAuthenticated();

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("name", schematic.name() == null || schematic.name().isBlank() ? "Schemat web-app" : schematic.name());
        requestBody.put("elements", schematic.elements() == null ? List.of() : schematic.elements());
        requestBody.put("wires", schematic.wires() == null ? List.of() : schematic.wires());

        if (projectId == null) {
            return createProject(requestBody);
        }

        HttpResponse<String> updateResponse = send(
                buildAuthorizedJsonRequest("/projects/" + projectId)
                        .PUT(jsonBody(requestBody))
                        .build(),
                "Nie udało się zaktualizować projektu w backendzie.");

        if (updateResponse.statusCode() == 404) {
            return createProject(requestBody);
        }

        return readProjectResponse(updateResponse, 200, "Nie udało się zaktualizować projektu w backendzie.");
    }

    public SimulationExecution runSimulation(Long projectId, String netlistBcs) {
        ensureAuthenticated();
        if (projectId == null) {
            throw new IllegalStateException("Najpierw zapisz projekt w backendzie, zanim uruchomisz symulację.");
        }

        HttpResponse<String> response = send(
                buildAuthorizedJsonRequest("/projects/" + projectId + "/simulate")
                        .POST(jsonBody(Map.of("netlist_bcs", requireText(netlistBcs, "Netlista symulacji jest pusta."))))
                        .build(),
                "Nie udało się uruchomić symulacji w backendzie.");

        if (response.statusCode() != 200) {
            throw protectedRequestFailure(response, "Backend odrzucił żądanie symulacji.");
        }

        SimulationResponse simulationResponse = parseResponseBody(
                response.body(),
                SimulationResponse.class,
                "Backend zwrócił nieprawidłową odpowiedź symulacji.");

        if (simulationResponse.dataCsv() == null || simulationResponse.dataCsv().isBlank()) {
            throw new IllegalStateException("Backend nie zwrócił danych CSV z symulacji.");
        }

        return new SimulationExecution(
                simulationResponse.status(),
                simulationResponse.simulationId(),
                simulationResponse.timestamp(),
                simulationResponse.dataCsv());
    }

    private CircuitSchematic createProject(Map<String, Object> requestBody) {
        HttpResponse<String> response = send(
                buildAuthorizedJsonRequest("/projects")
                        .POST(jsonBody(requestBody))
                        .build(),
                "Nie udało się zapisać projektu w backendzie.");
        return readProjectResponse(response, 201, "Nie udało się zapisać projektu w backendzie.");
    }

    private CircuitSchematic readProjectResponse(HttpResponse<String> response, int expectedStatus, String failurePrefix) {
        if (response.statusCode() != expectedStatus && response.statusCode() != 200) {
            throw protectedRequestFailure(response, failurePrefix);
        }

        ProjectResponse projectResponse = parseResponseBody(
                response.body(),
                ProjectResponse.class,
                "Backend zwrócił nieprawidłową odpowiedź projektu.");

        return toCircuitSchematic(projectResponse);
    }

    private CircuitSchematic toCircuitSchematic(ProjectResponse projectResponse) {
        return new CircuitSchematic(
                projectResponse.id(),
                projectResponse.name(),
                null,
                projectResponse.elements() == null ? List.of() : projectResponse.elements(),
                projectResponse.wires() == null ? List.of() : projectResponse.wires(),
                projectResponse.createdAt());
    }

    private void ensureAuthenticated() {
        if (!backendAuthSession.isAuthenticated()) {
            throw new IllegalStateException("Zaloguj się do backendu, zanim zapiszesz projekt albo uruchomisz symulację.");
        }
    }

    private HttpRequest.Builder buildJsonRequest(String path) {
        return HttpRequest.newBuilder()
                .uri(URI.create(backendBaseUrl + path))
                .header("Content-Type", JSON_CONTENT_TYPE)
                .header("Accept", "application/json");
    }

    private HttpRequest.Builder buildAuthorizedJsonRequest(String path) {
        return buildJsonRequest(path)
                .header("Authorization", "Bearer " + backendAuthSession.token());
    }

    private HttpRequest.BodyPublisher jsonBody(Object body) {
        try {
            return HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Nie udało się przygotować JSON-a dla backendu.", exception);
        }
    }

    private <T> T sendJson(HttpRequest request, Class<T> responseType, String failurePrefix) {
        HttpResponse<String> response = send(request, failurePrefix);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw requestFailure(response, failurePrefix, false);
        }
        return parseResponseBody(response.body(), responseType, failurePrefix);
    }

    private void sendExpectingSuccess(HttpRequest request, int expectedStatus, String failurePrefix) {
        HttpResponse<String> response = send(request, failurePrefix);
        if (response.statusCode() != expectedStatus && response.statusCode() != 200) {
            throw requestFailure(response, failurePrefix, false);
        }
    }

    private HttpResponse<String> send(HttpRequest request, String failurePrefix) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            throw new IllegalStateException(failurePrefix + " Brak połączenia z backendem.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(failurePrefix + " Komunikacja z backendem została przerwana.", exception);
        }
    }

    private <T> T parseResponseBody(String body, Class<T> responseType, String failureMessage) {
        try {
            return objectMapper.readValue(body, responseType);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(failureMessage, exception);
        }
    }

    private IllegalStateException protectedRequestFailure(HttpResponse<String> response, String failurePrefix) {
        return requestFailure(response, failurePrefix, true);
    }

    private IllegalStateException requestFailure(HttpResponse<String> response, String failurePrefix, boolean clearSessionOnUnauthorized) {
        if (clearSessionOnUnauthorized && (response.statusCode() == 401 || response.statusCode() == 403)) {
            backendAuthSession.clear();
            return new IllegalStateException("Sesja backendu wygasła albo token jest nieprawidłowy. Zaloguj się ponownie.");
        }

        return new IllegalStateException(failurePrefix + " " + extractErrorMessage(response));
    }

    private String extractErrorMessage(HttpResponse<String> response) {
        String body = response.body();
        if (body == null || body.isBlank()) {
            return "Kod HTTP: " + response.statusCode() + ".";
        }

        try {
            JsonNode node = objectMapper.readTree(body);
            if (node.hasNonNull("error")) {
                return node.get("error").asText();
            }
        } catch (JsonProcessingException ignored) {
            // The backend may sometimes return plain text.
        }

        return body;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
        return value;
    }

    public record SimulationExecution(
            String status,
            Long simulationId,
            Instant timestamp,
            String csv) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AuthResponse(
            String token,
            UserDto user) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ProjectResponse(
            Long id,
            String name,
            List<CircuitElement> elements,
            List<Wire> wires,
            Instant createdAt) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SimulationResponse(
            String status,
            Long simulationId,
            Instant timestamp,
            @JsonProperty("data_csv") String dataCsv) {
    }
}
