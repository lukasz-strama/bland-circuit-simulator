package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record Wire(
        @JsonProperty("id") String id,
        @JsonProperty("node") String node,
        @JsonProperty("points") List<int[]> points) {
}
