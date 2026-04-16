package pl.polsl.bland.desktop.service;

import java.util.*;

public class SimulationCsvService {

    public record Point(double time, Map<String, Double> values) {}


    public record ParsedSimulation(List<Point> points, List<String> signals) {}

    
    public ParsedSimulation parse(String csv) {
        if (csv == null || csv.isBlank()) {
            return new ParsedSimulation(List.of(), List.of());
        }

        String[] lines = csv.split("\\R");
        if (lines.length < 2) {
            return new ParsedSimulation(List.of(), List.of());
        }

        // Nagłówki
        String[] headers = lines[0].split(",");
        List<String> signals = new ArrayList<>();
        for (int i = 1; i < headers.length; i++) {
            signals.add(headers[i].trim());
        }

        // Punkty
        List<Point> points = new ArrayList<>();

        for (int i = 1; i < lines.length; i++) {
            if (lines[i].isBlank()) continue;

            String[] cols = lines[i].split(",");
            double time = Double.parseDouble(cols[0].trim());

            Map<String, Double> values = new LinkedHashMap<>();
            for (int c = 1; c < cols.length; c++) {
                double v = Double.parseDouble(cols[c].trim());
                values.put(signals.get(c - 1), v);
            }

            points.add(new Point(time, values));
        }

        return new ParsedSimulation(points, signals);
    }
}
