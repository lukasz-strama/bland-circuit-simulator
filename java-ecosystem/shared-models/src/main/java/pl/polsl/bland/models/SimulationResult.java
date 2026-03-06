package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SimulationResult(
        @JsonProperty("timePoints") double[] timePoints,
        @JsonProperty("voltages") Map<String, double[]> voltages,
        @JsonProperty("currents") Map<String, double[]> currents) {
}
