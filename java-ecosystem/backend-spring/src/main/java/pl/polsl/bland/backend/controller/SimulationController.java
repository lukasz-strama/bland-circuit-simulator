package pl.polsl.bland.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.bland.models.SimulationRequest;

@RestController
@RequestMapping("/api/simulate")
public class SimulationController {

    @PostMapping(produces = "text/csv")
    public ResponseEntity<String> simulate(@RequestBody SimulationRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
