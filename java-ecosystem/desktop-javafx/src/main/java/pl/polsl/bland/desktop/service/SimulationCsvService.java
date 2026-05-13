package pl.polsl.bland.desktop.service;

import java.util.*;

public class SimulationCsvService {

    public record ParsedSimulation(
            double[] timePoints,
            Map<String, double[]> series,
            List<String> headers
    ) {
        public int sampleCount() {
            return timePoints.length;
        }

        public boolean hasSeries(String name) {
            return series.containsKey(name);
        }

        public double[] seriesOrNull(String name) {
            return series.get(name);
        }
    }

    public ParsedSimulation parse(String csv) {
        // walidacja jak w wersji webowej
        if (csv == null || csv.isBlank()) {
            throw new IllegalArgumentException("Backend zwrócił pusty wynik CSV.");
        }

        List<String> lines = csv.lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();

        if (lines.isEmpty()) {
            throw new IllegalArgumentException("Brak danych w odpowiedzi CSV.");
        }

        List<String> headers = splitCsvLine(lines.get(0));
        if (headers.isEmpty() || !"time".equalsIgnoreCase(headers.get(0))) {
            throw new IllegalArgumentException(
                    "Pierwsza kolumna CSV musi mieć nagłówek 'time', a ma: "
                    + (headers.isEmpty() ? "<brak>" : headers.get(0)));
        }

        double[] timePoints = new double[lines.size() - 1];
        Map<String, double[]> series = new LinkedHashMap<>();
        for (int i = 1; i < headers.size(); i++) {
            series.put(headers.get(i), new double[lines.size() - 1]);
        }

        for (int lineIdx = 1; lineIdx < lines.size(); lineIdx++) {
            List<String> values = splitCsvLine(lines.get(lineIdx));
            if (values.size() != headers.size()) {
                throw new IllegalArgumentException(
                        "Wiersz CSV " + (lineIdx + 1) + " ma " + values.size()
                        + " kolumn, oczekiwano " + headers.size() + ".");
            }

            int dataIdx = lineIdx - 1;
            timePoints[dataIdx] = parseDouble(values.get(0), lineIdx + 1, headers.get(0));
            for (int c = 1; c < headers.size(); c++) {
                series.get(headers.get(c))[dataIdx] =
                        parseDouble(values.get(c), lineIdx + 1, headers.get(c));
            }
        }

        return new ParsedSimulation(timePoints, series, headers);
    }

    private List<String> splitCsvLine(String line) {
        return List.of(line.split("\\s*,\\s*"));
    }

    private double parseDouble(String raw, int lineNumber, String column) {
        try {
            return Double.parseDouble(raw);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Nie udało się odczytać wartości CSV w wierszu "
                    + lineNumber + " dla kolumny '" + column + "': " + raw);
        }
    }
}