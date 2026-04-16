package pl.polsl.bland.backend.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pl.polsl.bland.backend.entity.SchematicEntity;
import pl.polsl.bland.backend.entity.UserEntity;
import pl.polsl.bland.backend.repository.SchematicRepository;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final SchematicRepository schematicRepository;
    private final ObjectMapper objectMapper;

    public ProjectController(SchematicRepository schematicRepository, ObjectMapper objectMapper) {
        this.schematicRepository = schematicRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<?> list(@AuthenticationPrincipal UserEntity user) {
        List<SchematicEntity> projects = schematicRepository.findByOwnerId(user.getId());
        return ResponseEntity.ok(projects.stream().map(this::toResponseMap).toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id, @AuthenticationPrincipal UserEntity user) {
        return schematicRepository.findByIdAndOwnerId(id, user.getId())
                .map(project -> ResponseEntity.ok((Object) toResponseMap(project)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Projekt o podanym ID nie istnieje")));
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> body,
                                    @AuthenticationPrincipal UserEntity user) {
        String name = (String) body.get("name");
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Pole 'name' jest wymagane"));
        }

        String elementsJson = toJson(body.get("elements"));
        String wiresJson = toJson(body.get("wires"));

        SchematicEntity project = new SchematicEntity(name, elementsJson, wiresJson, user);
        SchematicEntity saved = schematicRepository.save(project);

        return ResponseEntity.status(HttpStatus.CREATED).body(toResponseMap(saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody Map<String, Object> body,
                                    @AuthenticationPrincipal UserEntity user) {
        return schematicRepository.findByIdAndOwnerId(id, user.getId())
                .map(project -> {
                    String name = (String) body.get("name");
                    if (name != null && !name.isBlank()) {
                        project.setName(name);
                    }
                    if (body.containsKey("elements")) {
                        project.setElementsJson(toJson(body.get("elements")));
                    }
                    if (body.containsKey("wires")) {
                        project.setWiresJson(toJson(body.get("wires")));
                    }
                    SchematicEntity saved = schematicRepository.save(project);
                    return ResponseEntity.ok((Object) toResponseMap(saved));
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Projekt o podanym ID nie istnieje")));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, @AuthenticationPrincipal UserEntity user) {
        return schematicRepository.findByIdAndOwnerId(id, user.getId())
                .map(project -> {
                    schematicRepository.delete(project);
                    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
                })
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Projekt o podanym ID nie istnieje")));
    }

    private Map<String, Object> toResponseMap(SchematicEntity project) {
        return Map.of(
                "id", project.getId(),
                "name", project.getName(),
                "elements", parseJson(project.getElementsJson()),
                "wires", parseJson(project.getWiresJson()),
                "createdAt", project.getCreatedAt().toString(),
                "updatedAt", project.getUpdatedAt().toString()
        );
    }

    private String toJson(Object obj) {
        try {
            return obj == null ? "[]" : objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private Object parseJson(String json) {
        try {
            return json == null ? List.of() : objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
