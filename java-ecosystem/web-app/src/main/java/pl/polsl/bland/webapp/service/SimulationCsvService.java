package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SimulationCsvService {
    public ParsedSimulation parse(String csv) {
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
            throw new IllegalArgumentException("Pierwsza kolumna CSV musi mieć nagłówek 'time'.");
        }

        List<Double> timePoints = new ArrayList<>();
        LinkedHashMap<String, List<Double>> seriesLists = new LinkedHashMap<>();
        for (int index = 1; index < headers.size(); index++) {
            seriesLists.put(headers.get(index), new ArrayList<>());
        }

        for (int lineIndex = 1; lineIndex < lines.size(); lineIndex++) {
            List<String> values = splitCsvLine(lines.get(lineIndex));
            if (values.size() != headers.size()) {
                throw new IllegalArgumentException("Wiersz CSV " + (lineIndex + 1) + " ma niepoprawną liczbę kolumn.");
            }

            timePoints.add(parseNumber(values.get(0), lineIndex + 1, headers.get(0)));
            for (int index = 1; index < headers.size(); index++) {
                seriesLists.get(headers.get(index)).add(parseNumber(values.get(index), lineIndex + 1, headers.get(index)));
            }
        }

        LinkedHashMap<String, double[]> series = new LinkedHashMap<>();
        for (Map.Entry<String, List<Double>> entry : seriesLists.entrySet()) {
            double[] values = new double[entry.getValue().size()];
            for (int index = 0; index < entry.getValue().size(); index++) {
                values[index] = entry.getValue().get(index);
            }
            series.put(entry.getKey(), values);
        }

        return new ParsedSimulation(toArray(timePoints), series, headers);
    }

    private List<String> splitCsvLine(String line) {
        return List.of(line.split("\\s*,\\s*"));
    }

    private double parseNumber(String rawValue, int lineNumber, String header) {
        try {
            return Double.parseDouble(rawValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Nie udało się odczytać wartości CSV w wierszu " + lineNumber + " dla kolumny " + header + ".");
        }
    }

    private double[] toArray(List<Double> values) {
        double[] result = new double[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    public record ParsedSimulation(
            double[] timePoints,
            Map<String, double[]> series,
            List<String> headers) {
        public int sampleCount() {
            return timePoints.length;
        }

        public boolean hasSeries(String name) {
            return series.containsKey(name);
        }

        public double[] seriesOrNull(String name) {
            return series.get(name);
        }

        public double[] zeroSeries() {
            return new double[sampleCount()];
        }
    }
}
