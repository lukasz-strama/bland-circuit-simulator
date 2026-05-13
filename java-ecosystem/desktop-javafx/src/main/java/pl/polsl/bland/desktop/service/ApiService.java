package pl.polsl.bland.desktop.service;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import pl.polsl.bland.models.CircuitSchematic;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Klient HTTP komunikujący się z backendem Spring Boot.
 * Domyślnie zakłada backend na localhost:8080.
 * Zmień BASE_URL jeśli backend działa na innym hoście/porcie.
 */
public class ApiService {

    private static final String BASE_URL =
            System.getProperty("bland.api.url", "http://localhost:8080/api");

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

 
        private String jwtToken;

        public void setJwtToken(String jwtToken) {
    this.jwtToken = jwtToken;
}

    public Long saveSchematic(String name, CircuitSchematic schematic) throws Exception {
    Map<String, Object> payload = new HashMap<>();
    payload.put("name", name);
    payload.put("elements", schematic.elements()); 
    payload.put("wires", schematic.wires());       

    String body = mapper.writeValueAsString(payload);

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/projects")) 
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + jwtToken)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        System.out.println("zapis");
System.out.println("STATUS = " + response.statusCode());
System.out.println("BODY   = " + response.body());

System.out.println("koniec zapisu");
        if (response.statusCode() >= 400) {
            throw new RuntimeException(
                    "Serwer zwrócił błąd " + response.statusCode() + ": " + response.body());
        }

        // Backend zwraca zapisany obiekt – odczytujemy id
        Map<String, Object> saved = mapper.readValue(response.body(), new TypeReference<>() {});
        Number id = (Number) saved.get("id");
        return id == null ? null : id.longValue();
    }

    public List<SchematicMeta> listSchematics() throws Exception {

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/projects"))
            .header("Authorization", "Bearer " + jwtToken)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

    HttpResponse<String> response =
            http.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
        throw new RuntimeException(
                "Nie można pobrać listy: "
                        + response.statusCode()
                        + " -> "
                        + response.body());
    }

    List<Map<String, Object>> raw =
            mapper.readValue(response.body(), new TypeReference<>() {});

    return raw.stream()
            .map(m -> new SchematicMeta(
                    ((Number) m.get("id")).longValue(),
                    (String) m.get("name"),
                    m.getOrDefault("createdAt", "").toString()
            ))
            .toList();
}

    public CircuitSchematic loadSchematic(long id) throws Exception {

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/projects/" + id))
            .header("Authorization", "Bearer " + jwtToken)
            .GET()
            .timeout(Duration.ofSeconds(10))
            .build();

    HttpResponse<String> response =
            http.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() == 404) {
        throw new RuntimeException("Projekt nie istnieje.");
    }

    if (response.statusCode() >= 400) {
        throw new RuntimeException(
                "Błąd wczytywania: "
                        + response.statusCode()
                        + " -> "
                        + response.body());
    }

    return mapper.readValue(response.body(), CircuitSchematic.class);
}

public void login(String username, String password) throws Exception {

    String body = mapper.writeValueAsString(Map.of(
            "username", username,
            "password", password
    ));

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .timeout(Duration.ofSeconds(10))
            .build();

    HttpResponse<String> response =
            http.send(request, HttpResponse.BodyHandlers.ofString());

    System.out.println("LOGIN STATUS = " + response.statusCode());
    System.out.println("LOGIN BODY = " + response.body());

    if (response.statusCode() >= 400) {
        throw new RuntimeException("Błąd logowania: " + response.body());
    }

    Map<String, Object> dto =
            mapper.readValue(response.body(), new TypeReference<>() {});

    jwtToken = (String) dto.get("token");

    if (jwtToken == null || jwtToken.isBlank()) {
        throw new RuntimeException("Backend nie zwrócił JWT.");
    }
}



  
    public record SchematicMeta(long id, String name, String createdAt) {
        @Override
        public String toString() {
            return name + (createdAt.isBlank() ? "" : "  [" + createdAt + "]");
        }
    }
}