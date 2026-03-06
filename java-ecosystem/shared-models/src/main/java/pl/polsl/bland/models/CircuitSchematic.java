package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

public record CircuitSchematic(
        @JsonProperty("id") Long id,
        @JsonProperty("name") String name,
        @JsonProperty("ownerId") Long ownerId,
        @JsonProperty("elements") List<CircuitElement> elements,
        @JsonProperty("wires") List<Wire> wires,
        @JsonProperty("createdAt") Instant createdAt) {
}
