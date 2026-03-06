package pl.polsl.bland.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.polsl.bland.models.CircuitSchematic;

import java.util.List;

@RestController
@RequestMapping("/api/schematics")
public class SchematicController {

    @GetMapping
    public ResponseEntity<List<CircuitSchematic>> list() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CircuitSchematic> get(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PostMapping
    public ResponseEntity<CircuitSchematic> create(@RequestBody CircuitSchematic schematic) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<CircuitSchematic> update(@PathVariable Long id, @RequestBody CircuitSchematic schematic) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
