package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.SimulationResult;

import java.net.http.HttpClient;

@Service
public class BackendClient {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final String BACKEND_BASE_URL = "http://localhost:8080/api";

    public CircuitSchematic saveSchematic(CircuitSchematic schematic) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public SimulationResult runSimulation(SimulationRequest request) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
