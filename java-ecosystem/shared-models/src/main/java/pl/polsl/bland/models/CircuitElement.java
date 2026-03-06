package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CircuitElement(
        @JsonProperty("id") String id,
        @JsonProperty("type") ElementType type,
        @JsonProperty("node1") String node1,
        @JsonProperty("node2") String node2,
        @JsonProperty("value") double value,
        @JsonProperty("sourceType") String sourceType,
        @JsonProperty("frequency") Double frequency,
        @JsonProperty("x") int x,
        @JsonProperty("y") int y,
        @JsonProperty("rotation") int rotation) {
    public enum ElementType {
        R, L, C, V, I
    }
}
