package pl.polsl.bland.desktop.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.SimulationResult;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ApiClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BASE_URL = "http://localhost:8080/api";

    public CircuitSchematic saveSchematic(CircuitSchematic schematic) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public SimulationResult runSimulation(SimulationRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
