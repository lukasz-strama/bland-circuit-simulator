package pl.polsl.bland.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Klient HTTP komunikujący się z silnikiem symulacyjnym (C++).
 * Wysyła netlistę w formacie tekstowym i odbiera wynik w formacie CSV.
 *
 * Ten klient NIE obsługuje JWT ani użytkowników — służy wyłącznie do symulacji.
 */
public class ApiClient {

    /** Klient HTTP używany do komunikacji z silnikiem. */
    private final HttpClient http = HttpClient.newHttpClient();

    /** Adres endpointu silnika symulacyjnego. */
    private static final String URL = "https://bland-circuit-engine.onrender.com/api/v1/simulate";

    /**
     * Wysyła netlistę do silnika symulacyjnego i zwraca wynik w formacie CSV.
     *
     * @param netlist pełna netlista wygenerowana przez NetlistParser
     * @return wynik symulacji jako tekst CSV
     * @throws Exception gdy silnik zwróci błąd lub wystąpi problem z połączeniem
     */
    public String runNetlist(String netlist) throws Exception {

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(netlist))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            throw new RuntimeException("Błąd symulacji: " + resp.body());
        }

        return resp.body();
    }
}
