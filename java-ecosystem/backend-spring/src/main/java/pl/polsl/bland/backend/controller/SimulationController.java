package pl.polsl.bland.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.polsl.bland.backend.entity.SchematicEntity;
import pl.polsl.bland.backend.entity.SimulationResultEntity;
import pl.polsl.bland.backend.entity.UserEntity;
import pl.polsl.bland.backend.repository.SchematicRepository;
import pl.polsl.bland.backend.repository.SimulationResultRepository;
import pl.polsl.bland.backend.service.EngineClient;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects/{projectId}")
public class SimulationController {

    private final SchematicRepository schematicRepository;
    private final SimulationResultRepository simulationResultRepository;
    private final EngineClient engineClient;

    public SimulationController(SchematicRepository schematicRepository,
                                SimulationResultRepository simulationResultRepository,
                                EngineClient engineClient) {
        this.schematicRepository = schematicRepository;
        this.simulationResultRepository = simulationResultRepository;
        this.engineClient = engineClient;
    }

    @PostMapping("/simulate")
    public ResponseEntity<?> simulate(@PathVariable Long projectId,
                                      @RequestBody Map<String, Object> body,
                                      @AuthenticationPrincipal UserEntity user) {

        // 1. Znajdz projekt (tylko swoj)
        SchematicEntity project = schematicRepository.findByIdAndOwnerId(projectId, user.getId())
                .orElse(null);
        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Projekt o podanym ID nie istnieje"));
        }

        // 2. Walidacja body
        String netlistBcs = (String) body.get("netlist_bcs");
        if (netlistBcs == null || netlistBcs.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Pole 'netlist_bcs' jest wymagane"));
        }

        // 3. Wyslij do silnika C++
        String csv;
        try {
            csv = engineClient.simulate(netlistBcs);
        } catch (EngineClient.EngineException e) {
            int code = e.getStatusCode();
            if (code == 400 || code == 422) {
                return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                        .body(Map.of("error", e.getMessage()));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Błąd wewnętrzny silnika symulacji"));
        }

        // 4. Zapisz wynik w bazie
        SimulationResultEntity result = new SimulationResultEntity(project, "success", csv);
        SimulationResultEntity saved = simulationResultRepository.save(result);

        // 5. Zwroc odpowiedz
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "simulationId", saved.getId(),
                "timestamp", saved.getTimestamp().toString(),
                "data_csv", csv
        ));
    }

    @GetMapping("/simulations")
    public ResponseEntity<?> listSimulations(@PathVariable Long projectId,
                                             @AuthenticationPrincipal UserEntity user) {
        // Sprawdz czy projekt nalezy do usera
        if (schematicRepository.findByIdAndOwnerId(projectId, user.getId()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Projekt o podanym ID nie istnieje"));
        }

        List<SimulationResultEntity> simulations =
                simulationResultRepository.findByProjectIdOrderByTimestampDesc(projectId);

        return ResponseEntity.ok(simulations.stream().map(sim -> Map.of(
                "simulationId", sim.getId(),
                "timestamp", sim.getTimestamp().toString(),
                "status", sim.getStatus()
        )).toList());
    }

    @GetMapping("/simulations/{simulationId}")
    public ResponseEntity<?> getSimulation(@PathVariable Long projectId,
                                           @PathVariable Long simulationId,
                                           @AuthenticationPrincipal UserEntity user) {
        // Sprawdz czy projekt nalezy do usera
        if (schematicRepository.findByIdAndOwnerId(projectId, user.getId()).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Projekt o podanym ID nie istnieje"));
        }

        return simulationResultRepository.findByIdAndProjectId(simulationId, projectId)
                .map(sim -> ResponseEntity.ok((Object) Map.of(
                        "status", sim.getStatus(),
                        "simulationId", sim.getId(),
                        "timestamp", sim.getTimestamp().toString(),
                        "data_csv", sim.getDataCsv()
                )))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Symulacja o podanym ID nie istnieje")));
    }
}
