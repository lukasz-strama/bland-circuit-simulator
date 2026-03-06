package pl.polsl.bland.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public record SimulationRequest(
        @JsonProperty("schematicId") Long schematicId,
        @JsonProperty("analysisType") AnalysisType analysisType,
        @JsonProperty("parameters") Map<String, Double> parameters) {
    public enum AnalysisType {
        DC, TRANSIENT
    }
}
