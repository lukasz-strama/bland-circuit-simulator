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
 * Serwis odpowiedzialny za komunikację z backendem Spring Boot.
 * Obsługuje:
 * - logowanie i rejestrację użytkowników,
 * - zapis schematów do bazy danych,
 * - odczyt schematów,
 * - tryb gościa (bez możliwości zapisu/odczytu).
 *
 * Serwis działa jako singleton — instancja dostępna przez {@link ApiService#get()}.
 */
public class ApiService {

    /** Bazowy URL backendu Spring Boot. */
    private static final String BASE_URL = "http://localhost:8080/api";
    
    /** Klient HTTP używany do komunikacji z backendem. */
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** Mapper JSON obsługujący daty i konwersję obiektów. */
    private final ObjectMapper mapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        /** Token JWT aktualnie zalogowanego użytkownika. */
        private String jwtToken;

        public void setJwtToken(String jwtToken) {
    this.jwtToken = jwtToken;
}

    /**
     * Zapisuje schemat w bazie danych backendu.
     *
     * @param name nazwa schematu
     * @param schematic obiekt schematu
     * @return ID zapisanego schematu
     * @throws Exception gdy użytkownik jest w trybie gościa lub backend zwróci błąd
     */
    public Long saveSchematic(String name, CircuitSchematic schematic) throws Exception {
        if (guestMode) {
    throw new RuntimeException("Tryb gościa - zapis wraz z odczytem są zablokowane.");
}

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

    /**
     * Pobiera listę metadanych wszystkich schematów użytkownika.
     *
     * @return lista metadanych schematów
     * @throws Exception gdy backend zwróci błąd
     */
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

    /**
     * Wczytuje schemat o podanym ID.
     *
     * @param id identyfikator schematu
     * @return obiekt CircuitSchematic
     * @throws Exception gdy użytkownik jest w trybie gościa lub backend zwróci błąd
     */
    public CircuitSchematic loadSchematic(long id) throws Exception {
        if (guestMode) {
    throw new RuntimeException("Tryb gościa - zapis wraz z odczytem są zablokowane.");
}


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

/**
     * Loguje użytkownika w backendzie i zapisuje token JWT.
     *
     * @param username nazwa użytkownika
     * @param password hasło użytkownika
     * @throws Exception gdy backend zwróci błąd logowania
     */
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

/** Flaga trybu gościa — blokuje zapis i odczyt schematów. */
private boolean guestMode = false;

/** Singleton instancji ApiService. */
public static ApiService INSTANCE = new ApiService();

/**
     * Zwraca globalną instancję ApiService.
     *
     * @return singleton ApiService
     */
public static ApiService get() { return INSTANCE; }

/**
     * Włącza tryb gościa — blokuje zapis i odczyt schematów.
     */
public void setGuestMode() {
    this.guestMode = true;
    this.jwtToken = null;
}

/**
     * @return true jeśli aplikacja działa w trybie gościa
     */
public boolean isGuest() {
    return guestMode;
}

/**
     * Rejestruje nowego użytkownika w backendzie.
     *
     * @param username nazwa użytkownika
     * @param email adres e-mail
     * @param password hasło
     * @throws Exception gdy backend zwróci błąd rejestracji
     */
public void register(String username, String email, String password) throws Exception {
    String body = mapper.writeValueAsString(Map.of(
            "username", username,
            "email", email,
            "password", password
    ));

    HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + "/auth/register"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() >= 400) {
        throw new RuntimeException("Błąd rejestracji: " + response.body());
    }
}

/**
     * Prosta struktura metadanych schematu zwracana przez backend.
     *
     * @param id identyfikator schematu
     * @param name nazwa schematu
     * @param createdAt data utworzenia
     */
    public record SchematicMeta(long id, String name, String createdAt) {
        @Override
        public String toString() {
            return name + (createdAt.isBlank() ? "" : "  [" + createdAt + "]");
        }
    }
}