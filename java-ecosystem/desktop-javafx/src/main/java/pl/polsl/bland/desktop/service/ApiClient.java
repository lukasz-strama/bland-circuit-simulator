package pl.polsl.bland.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

public class ApiClient {

    private final HttpClient http = HttpClient.newHttpClient();

    private static final String URL = "https://bland-circuit-engine.onrender.com/api/v1/simulate";

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
