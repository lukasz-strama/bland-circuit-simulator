package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkspaceMockService {
    private final Map<String, ElementDetails> elements = Map.of(
            "R1", new ElementDetails(
                    "R1",
                    "R",
                    "Rezystor",
                    "120 Ohm",
                    "IN",
                    "N001",
                    "Poziomo",
                    "Rezystor wejściowy ograniczający prąd w gałęzi filtru.",
                    "I(R1)",
                    "38,4 mA",
                    "-36,1 mA",
                    "23,7 mA",
                    "1,22 ms",
                    "Analiza przejściowa",
                    "Prąd wejściowy szybko rośnie po pobudzeniu, a potem stabilizuje się wraz z odpowiedzią całego toru RLC.",
                    List.of(
                            new ResultRow("0,00 ms", "5,00 V", "0,0 mA", "Start źródła"),
                            new ResultRow("0,80 ms", "4,62 V", "31,2 mA", "Narastanie prądu"),
                            new ResultRow("1,22 ms", "4,41 V", "38,4 mA", "Wartość szczytowa"),
                            new ResultRow("2,40 ms", "3,88 V", "24,7 mA", "Przejście do stanu tłumionego")),
                    """
                    * Filtr RLC - ćwiczenie 04
                    V1 IN 0 SIN(0 5 1k)
                    R1 IN N001 120
                    L1 N001 N002 22m
                    C1 N002 0 4.7u
                    .simulate type=transient tstop=0.008 tstep=0.0001
                    """,
                    List.of(
                            "Wybrano element R1.",
                            "Ślad aktywny: I(R1).",
                            "Dane pochodzą z mockowanej sesji frontendowej.")),
            "L1", new ElementDetails(
                    "L1",
                    "L",
                    "Cewka",
                    "22 mH",
                    "N001",
                    "N002",
                    "Poziomo",
                    "Cewka magazynująca energię i budująca charakterystykę dynamiczną filtru.",
                    "I(L1)",
                    "32,8 mA",
                    "-28,9 mA",
                    "18,2 mA",
                    "2,04 ms",
                    "Analiza przejściowa",
                    "Cewka wprowadza opóźnienie odpowiedzi i wyraźny efekt tłumionych oscylacji po skoku pobudzenia.",
                    List.of(
                            new ResultRow("0,00 ms", "0,00 V", "0,0 mA", "Stan początkowy"),
                            new ResultRow("1,10 ms", "2,92 V", "22,4 mA", "Narastanie energii"),
                            new ResultRow("2,04 ms", "3,25 V", "32,8 mA", "Maksimum prądu"),
                            new ResultRow("3,10 ms", "2,12 V", "17,5 mA", "Tłumienie przebiegu")),
                    """
                    * Fragment netlisty dla L1
                    R1 IN N001 120
                    L1 N001 N002 22m
                    C1 N002 0 4.7u
                    .probe I(L1)
                    """,
                    List.of(
                            "Wybrano element L1.",
                            "Ślad aktywny: I(L1).",
                            "Przy kolejnej iteracji zostanie podpięty prawdziwy CSV parser.")),
            "C1", new ElementDetails(
                    "C1",
                    "C",
                    "Kondensator",
                    "4,7 uF",
                    "N002",
                    "0",
                    "Pionowo",
                    "Kondensator do masy odpowiadający za filtrację i wygładzanie odpowiedzi napięciowej.",
                    "V(N002)",
                    "4,74 V",
                    "0,00 V",
                    "2,61 V",
                    "2,88 ms",
                    "Analiza przejściowa",
                    "Napięcie na kondensatorze narasta z opóźnieniem i utrzymuje tłumione przeregulowanie w pierwszych milisekundach.",
                    List.of(
                            new ResultRow("0,00 ms", "0,00 V", "-", "Stan rozładowany"),
                            new ResultRow("1,60 ms", "2,14 V", "-", "Ładowanie"),
                            new ResultRow("2,88 ms", "4,74 V", "-", "Przeregulowanie"),
                            new ResultRow("4,20 ms", "3,95 V", "-", "Wygaszanie")),
                    """
                    * Fragment netlisty dla C1
                    L1 N001 N002 22m
                    C1 N002 0 4.7u
                    .probe V(N002)
                    """,
                    List.of(
                            "Wybrano element C1.",
                            "Ślad aktywny: V(N002).",
                            "To dobre miejsce na późniejsze wykresy napięcia.")),
            "V1", new ElementDetails(
                    "V1",
                    "V",
                    "Źródło napięcia",
                    "SIN(0 5 1k)",
                    "IN",
                    "0",
                    "Pionowo",
                    "Sinusoidalne źródło pobudzenia całego układu testowego.",
                    "I(V1)",
                    "41,2 mA",
                    "-40,5 mA",
                    "0,2 mA",
                    "1,14 ms",
                    "Analiza przejściowa",
                    "Źródło pokazuje pełny cykl pobudzenia oraz pobór prądu przez układ w zależności od aktualnego stanu filtru.",
                    List.of(
                            new ResultRow("0,00 ms", "0,00 V", "0,0 mA", "Start sinusoidy"),
                            new ResultRow("1,14 ms", "5,00 V", "41,2 mA", "Szczyt dodatni"),
                            new ResultRow("3,62 ms", "-4,96 V", "-40,5 mA", "Szczyt ujemny"),
                            new ResultRow("5,00 ms", "0,00 V", "0,3 mA", "Powrót do zera")),
                    """
                    * Netlista źródła
                    V1 IN 0 SIN(0 5 1k)
                    R1 IN N001 120
                    .probe I(V1)
                    """,
                    List.of(
                            "Wybrano element V1.",
                            "Ślad aktywny: I(V1).",
                            "Integracja z backendem później zastąpi te wartości rzeczywistym przebiegiem.")),
            "GND", new ElementDetails(
                    "GND",
                    "0",
                    "Masa",
                    "Węzeł odniesienia",
                    "0",
                    "-",
                    "Poziomo",
                    "Wspólny punkt odniesienia napięć dla całego schematu.",
                    "V(0)",
                    "0,00 V",
                    "0,00 V",
                    "0,00 V",
                    "0,00 ms",
                    "Analiza przejściowa",
                    "Węzeł masy utrzymuje poziom odniesienia i pozwala interpretować napięcia wszystkich pozostałych elementów.",
                    List.of(
                            new ResultRow("0,00 ms", "0,00 V", "-", "Węzeł odniesienia"),
                            new ResultRow("1,00 ms", "0,00 V", "-", "Brak zmian"),
                            new ResultRow("4,00 ms", "0,00 V", "-", "Brak zmian")),
                    """
                    * Węzeł odniesienia
                    .ground 0
                    """,
                    List.of(
                            "Wybrano element GND.",
                            "Ślad aktywny: V(0).",
                            "Masa będzie później współdzielona z parserem netlisty.")));

    public ElementDetails defaultElement() {
        return elements.get("R1");
    }

    public Optional<ElementDetails> findElement(String elementId) {
        return Optional.ofNullable(elements.get(elementId));
    }

    public record ElementDetails(
            String id,
            String symbol,
            String typeLabel,
            String value,
            String nodeA,
            String nodeB,
            String orientation,
            String description,
            String traceName,
            String peak,
            String min,
            String rms,
            String timeOfPeak,
            String analysisLabel,
            String simulationNote,
            List<ResultRow> rows,
            String netlist,
            List<String> logs) {
    }

    public record ResultRow(String time, String voltage, String current, String note) {
    }
}
