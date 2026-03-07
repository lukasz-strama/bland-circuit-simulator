package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkspaceMockService {
    private static final double SHEET_WIDTH = 1280;
    private static final double SHEET_HEIGHT = 860;
    private static final double GRID_LEFT = 36;
    private static final double GRID_TOP = 36;
    private static final double GRID_RIGHT = 1244;
    private static final double GRID_BOTTOM = 806;
    private static final double GRID_STEP = 16;

    private final Map<ElementType, ElementTemplate> templates = new EnumMap<>(ElementType.class);

    public WorkspaceMockService() {
        templates.put(ElementType.RESISTOR, new ElementTemplate(
                "R",
                "Rezystor",
                "120 Ohm",
                "IN",
                "N001",
                "Poziomo",
                "Rezystor wejściowy ograniczający prąd w gałęzi filtru.",
                "I(%s)",
                "38,4 mA",
                "-36,1 mA",
                "23,7 mA",
                "1,22 ms",
                "Prąd wejściowy szybko rośnie po pobudzeniu, a potem stabilizuje się wraz z odpowiedzią całego toru RLC.",
                List.of(
                        new ResultRow("0,00 ms", "5,00 V", "0,0 mA", "Start źródła"),
                        new ResultRow("0,80 ms", "4,62 V", "31,2 mA", "Narastanie prądu"),
                        new ResultRow("1,22 ms", "4,41 V", "38,4 mA", "Wartość szczytowa"),
                        new ResultRow("2,40 ms", "3,88 V", "24,7 mA", "Przejście do stanu tłumionego")),
                """
                        * Filtr RLC - ćwiczenie 04
                        V1 IN 0 SIN(0 5 1k)
                        %s IN N001 120
                        .simulate type=transient tstop=0.008 tstep=0.0001
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: I(%s).",
                        "Dane pochodzą z mockowanej sesji frontendowej.")));

        templates.put(ElementType.INDUCTOR, new ElementTemplate(
                "L",
                "Cewka",
                "22 mH",
                "N001",
                "N002",
                "Poziomo",
                "Cewka magazynująca energię i budująca charakterystykę dynamiczną filtru.",
                "I(%s)",
                "32,8 mA",
                "-28,9 mA",
                "18,2 mA",
                "2,04 ms",
                "Cewka wprowadza opóźnienie odpowiedzi i wyraźny efekt tłumionych oscylacji po skoku pobudzenia.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "0,0 mA", "Stan początkowy"),
                        new ResultRow("1,10 ms", "2,92 V", "22,4 mA", "Narastanie energii"),
                        new ResultRow("2,04 ms", "3,25 V", "32,8 mA", "Maksimum prądu"),
                        new ResultRow("3,10 ms", "2,12 V", "17,5 mA", "Tłumienie przebiegu")),
                """
                        * Fragment netlisty dla %s
                        R1 IN N001 120
                        %s N001 N002 22m
                        C1 N002 0 4.7u
                        .probe I(%s)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: I(%s).",
                        "Przy kolejnej iteracji zostanie podpięty prawdziwy CSV parser.")));

        templates.put(ElementType.CAPACITOR, new ElementTemplate(
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
                "Napięcie na kondensatorze narasta z opóźnieniem i utrzymuje tłumione przeregulowanie w pierwszych milisekundach.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "-", "Stan rozładowany"),
                        new ResultRow("1,60 ms", "2,14 V", "-", "Ładowanie"),
                        new ResultRow("2,88 ms", "4,74 V", "-", "Przeregulowanie"),
                        new ResultRow("4,20 ms", "3,95 V", "-", "Wygaszanie")),
                """
                        * Fragment netlisty dla %s
                        L1 N001 N002 22m
                        %s N002 0 4.7u
                        .probe V(N002)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: V(N002).",
                        "To dobre miejsce na późniejsze wykresy napięcia.")));

        templates.put(ElementType.VOLTAGE, new ElementTemplate(
                "V",
                "Źródło napięcia",
                "SIN(0 5 1k)",
                "IN",
                "0",
                "Pionowo",
                "Sinusoidalne źródło pobudzenia całego układu testowego.",
                "I(%s)",
                "41,2 mA",
                "-40,5 mA",
                "0,2 mA",
                "1,14 ms",
                "Źródło pokazuje pełny cykl pobudzenia oraz pobór prądu przez układ w zależności od aktualnego stanu filtru.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "0,0 mA", "Start sinusoidy"),
                        new ResultRow("1,14 ms", "5,00 V", "41,2 mA", "Szczyt dodatni"),
                        new ResultRow("3,62 ms", "-4,96 V", "-40,5 mA", "Szczyt ujemny"),
                        new ResultRow("5,00 ms", "0,00 V", "0,3 mA", "Powrót do zera")),
                """
                        * Netlista źródła
                        %s IN 0 SIN(0 5 1k)
                        R1 IN N001 120
                        .probe I(%s)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: I(%s).",
                        "Integracja z backendem później zastąpi te wartości rzeczywistym przebiegiem.")));

        templates.put(ElementType.GROUND, new ElementTemplate(
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
                "Węzeł masy utrzymuje poziom odniesienia i pozwala interpretować napięcia wszystkich pozostałych elementów.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "-", "Węzeł odniesienia"),
                        new ResultRow("1,00 ms", "0,00 V", "-", "Brak zmian"),
                        new ResultRow("4,00 ms", "0,00 V", "-", "Brak zmian")),
                """
                        * Węzeł odniesienia
                        .ground %s
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: V(0).",
                        "Masa będzie później współdzielona z parserem netlisty.")));

        templates.put(ElementType.DIODE, new ElementTemplate(
                "D",
                "Dioda",
                "1N4148",
                "ANODE",
                "CATHODE",
                "Poziomo",
                "Podstawowa dioda sygnałowa używana tutaj jako element demonstracyjny biblioteki.",
                "I(%s)",
                "14,2 mA",
                "0,0 mA",
                "6,8 mA",
                "0,92 ms",
                "Dioda przewodzi impulsowo po przekroczeniu progu, dlatego przebieg jest wyraźnie nieliniowy.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "0,0 mA", "Stan odcięcia"),
                        new ResultRow("0,92 ms", "0,74 V", "14,2 mA", "Próg przewodzenia"),
                        new ResultRow("2,00 ms", "0,69 V", "8,6 mA", "Stan przewodzenia")),
                """
                        * Fragment netlisty dla %s
                        %s ANODE CATHODE 1N4148
                        .probe I(%s)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: I(%s).",
                        "Model diody jest na razie mockowany.")));

        templates.put(ElementType.OPAMP, new ElementTemplate(
                "OP",
                "Wzmacniacz operacyjny",
                "uA741",
                "IN+",
                "IN-",
                "Poziomo",
                "Makieta wzmacniacza operacyjnego przygotowana do późniejszej konfiguracji sprzężenia zwrotnego.",
                "V(OUT)",
                "8,20 V",
                "-8,00 V",
                "2,14 V",
                "1,76 ms",
                "Wzmacniacz szybko dochodzi do nasycenia, a potem przechodzi do ustalonego punktu pracy.",
                List.of(
                        new ResultRow("0,00 ms", "0,00 V", "-", "Stan początkowy"),
                        new ResultRow("0,44 ms", "6,10 V", "-", "Narastanie wyjścia"),
                        new ResultRow("1,76 ms", "8,20 V", "-", "Nasycenie dodatnie"),
                        new ResultRow("3,40 ms", "2,18 V", "-", "Powrót do punktu pracy")),
                """
                        * Fragment netlisty dla %s
                        %s IN+ IN- OUT VCC VEE uA741
                        .probe V(OUT)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: V(OUT).",
                        "Model wzmacniacza zostanie później spięty z konfiguracją bibliotek LTspice.")));
    }

    public LinkedHashMap<String, WorkspaceElement> createInitialWorkspace() {
        LinkedHashMap<String, WorkspaceElement> elements = new LinkedHashMap<>();
        elements.put("R1", new WorkspaceElement("R1", ElementType.RESISTOR, 270, 192));
        elements.put("L1", new WorkspaceElement("L1", ElementType.INDUCTOR, 590, 192));
        elements.put("C1", new WorkspaceElement("C1", ElementType.CAPACITOR, 892, 194));
        elements.put("V1", new WorkspaceElement("V1", ElementType.VOLTAGE, 110, 194));
        elements.put("GND", new WorkspaceElement("GND", ElementType.GROUND, 138, 514));
        return elements;
    }

    public LinkedHashMap<String, WorkspaceWire> createInitialWires() {
        LinkedHashMap<String, WorkspaceWire> wires = new LinkedHashMap<>();
        wires.put("W1", new WorkspaceWire("W1", new PinRef("V1", "POS"), new PinRef("R1", "A")));
        wires.put("W2", new WorkspaceWire("W2", new PinRef("R1", "B"), new PinRef("L1", "A")));
        wires.put("W3", new WorkspaceWire("W3", new PinRef("L1", "B"), new PinRef("C1", "A")));
        wires.put("W4", new WorkspaceWire("W4", new PinRef("C1", "B"), new PinRef("GND", "REF")));
        wires.put("W5", new WorkspaceWire("W5", new PinRef("V1", "NEG"), new PinRef("GND", "REF")));
        return wires;
    }

    public WorkspaceElement createElement(Collection<WorkspaceElement> existingElements, ElementType type, double canvasX, double canvasY) {
        int nextSequence = nextSequence(existingElements, type);
        String nextId = buildElementId(type, nextSequence);
        double left = clamp(snap(canvasX - type.width() / 2), GRID_LEFT, GRID_RIGHT - type.width());
        double top = clamp(snap(canvasY - type.height() / 2), GRID_TOP, GRID_BOTTOM - type.height());
        return new WorkspaceElement(nextId, type, left, top);
    }

    public Optional<WorkspaceWire> createWire(
            Map<String, WorkspaceElement> elements,
            Collection<WorkspaceWire> existingWires,
            PinRef start,
            PinRef end) {
        if (start.equals(end) || resolvePin(elements, start).isEmpty() || resolvePin(elements, end).isEmpty()) {
            return Optional.empty();
        }

        boolean duplicate = existingWires.stream().anyMatch(wire -> connectsSamePins(wire, start, end));
        if (duplicate) {
            return Optional.empty();
        }

        return Optional.of(new WorkspaceWire("W" + nextWireSequence(existingWires), start, end));
    }

    public WorkspaceElement moveElement(WorkspaceElement element, double deltaX, double deltaY) {
        double left = clamp(snap(element.left() + deltaX), GRID_LEFT, GRID_RIGHT - element.type().width());
        double top = clamp(snap(element.top() + deltaY), GRID_TOP, GRID_BOTTOM - element.type().height());
        return new WorkspaceElement(element.id(), element.type(), left, top);
    }

    public List<ResolvedWire> resolveWires(Map<String, WorkspaceElement> elements, Collection<WorkspaceWire> wires) {
        List<ResolvedWire> resolved = new ArrayList<>();
        for (WorkspaceWire wire : wires) {
            resolveWire(elements, wire).ifPresent(resolved::add);
        }
        return resolved;
    }

    public Optional<ElementDetails> describeElement(WorkspaceElement element) {
        ElementTemplate template = templates.get(element.type());
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(template.describe(element));
    }

    public Optional<WorkspaceElement> firstElement(Map<String, WorkspaceElement> elements) {
        return elements.values().stream().findFirst();
    }

    public boolean isWireAttachedToElement(WorkspaceWire wire, String elementId) {
        return wire.start().elementId().equals(elementId) || wire.end().elementId().equals(elementId);
    }

    private int nextSequence(Collection<WorkspaceElement> elements, ElementType type) {
        int max = 0;
        for (WorkspaceElement element : elements) {
            if (element.type() != type) {
                continue;
            }
            String suffix = element.id().substring(type.prefix().length());
            if (suffix.isBlank()) {
                max = Math.max(max, 1);
                continue;
            }
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                max = Math.max(max, 1);
            }
        }
        return max + 1;
    }

    private int nextWireSequence(Collection<WorkspaceWire> wires) {
        int max = 0;
        for (WorkspaceWire wire : wires) {
            String suffix = wire.id().substring(1);
            try {
                max = Math.max(max, Integer.parseInt(suffix));
            } catch (NumberFormatException ignored) {
                max = Math.max(max, 1);
            }
        }
        return max + 1;
    }

    private String buildElementId(ElementType type, int sequence) {
        if (type == ElementType.GROUND && sequence == 1) {
            return type.prefix();
        }
        return type.prefix() + sequence;
    }

    private Optional<ResolvedWire> resolveWire(Map<String, WorkspaceElement> elements, WorkspaceWire wire) {
        Optional<PinPosition> start = resolvePin(elements, wire.start());
        Optional<PinPosition> end = resolvePin(elements, wire.end());
        if (start.isEmpty() || end.isEmpty()) {
            return Optional.empty();
        }

        List<WirePoint> path = route(start.get(), end.get());
        List<WireSegment> segments = new ArrayList<>();
        for (int index = 0; index < path.size() - 1; index++) {
            WirePoint current = path.get(index);
            WirePoint next = path.get(index + 1);
            if (current.equals(next)) {
                continue;
            }
            segments.add(new WireSegment(current.x(), current.y(), next.x(), next.y()));
        }

        return Optional.of(new ResolvedWire(wire.id(), wire.start(), wire.end(), segments, path));
    }

    private List<WirePoint> route(PinPosition start, PinPosition end) {
        List<WirePoint> path = new ArrayList<>();
        appendPoint(path, new WirePoint(start.x(), start.y()));

        if (sameAxis(start, end)) {
            appendPoint(path, new WirePoint(end.x(), end.y()));
            return path;
        }

        double midX = snap((start.x() + end.x()) / 2.0);
        if (sameCoordinate(midX, start.x()) || sameCoordinate(midX, end.x())) {
            midX = snap(start.x() + (end.x() >= start.x() ? GRID_STEP * 2 : -GRID_STEP * 2));
        }

        appendPoint(path, new WirePoint(midX, start.y()));
        appendPoint(path, new WirePoint(midX, end.y()));
        appendPoint(path, new WirePoint(end.x(), end.y()));
        return path;
    }

    private Optional<PinPosition> resolvePin(Map<String, WorkspaceElement> elements, PinRef pinRef) {
        WorkspaceElement element = elements.get(pinRef.elementId());
        if (element == null) {
            return Optional.empty();
        }
        return resolvePin(element, pinRef.pinKey());
    }

    private Optional<PinPosition> resolvePin(WorkspaceElement element, String pinKey) {
        return switch (element.type()) {
            case RESISTOR -> switch (pinKey) {
                case "A" -> Optional.of(pin(element, pinKey, 22, 48));
                case "B" -> Optional.of(pin(element, pinKey, 198, 48));
                default -> Optional.empty();
            };
            case INDUCTOR -> switch (pinKey) {
                case "A" -> Optional.of(pin(element, pinKey, 22, 48));
                case "B" -> Optional.of(pin(element, pinKey, 226, 48));
                default -> Optional.empty();
            };
            case CAPACITOR -> switch (pinKey) {
                case "A" -> Optional.of(pin(element, pinKey, 48, 46));
                case "B" -> Optional.of(pin(element, pinKey, 48, 190));
                default -> Optional.empty();
            };
            case VOLTAGE -> switch (pinKey) {
                case "POS" -> Optional.of(pin(element, pinKey, 74, 46));
                case "NEG" -> Optional.of(pin(element, pinKey, 74, 326));
                default -> Optional.empty();
            };
            case GROUND -> switch (pinKey) {
                case "REF" -> Optional.of(pin(element, pinKey, 46, 6));
                default -> Optional.empty();
            };
            case DIODE -> switch (pinKey) {
                case "ANODE" -> Optional.of(pin(element, pinKey, 18, 48));
                case "CATHODE" -> Optional.of(pin(element, pinKey, 162, 48));
                default -> Optional.empty();
            };
            case OPAMP -> switch (pinKey) {
                case "IN+" -> Optional.of(pin(element, pinKey, 0, 54));
                case "IN-" -> Optional.of(pin(element, pinKey, 0, 94));
                case "OUT" -> Optional.of(pin(element, pinKey, 220, 74));
                default -> Optional.empty();
            };
        };
    }

    private static PinPosition pin(WorkspaceElement element, String pinKey, double relativeX, double relativeY) {
        return new PinPosition(
                element.id(),
                pinKey,
                element.left() + relativeX,
                element.top() + relativeY);
    }

    private static boolean connectsSamePins(WorkspaceWire wire, PinRef start, PinRef end) {
        return (wire.start().equals(start) && wire.end().equals(end))
                || (wire.start().equals(end) && wire.end().equals(start));
    }

    private static boolean sameAxis(PinPosition start, PinPosition end) {
        return sameCoordinate(start.x(), end.x()) || sameCoordinate(start.y(), end.y());
    }

    private static boolean sameCoordinate(double first, double second) {
        return Math.abs(first - second) < 0.01;
    }

    private static void appendPoint(List<WirePoint> path, WirePoint point) {
        if (path.isEmpty() || !path.get(path.size() - 1).equals(point)) {
            path.add(point);
        }
    }

    private static double snap(double value) {
        return Math.round(value / GRID_STEP) * GRID_STEP;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public enum ElementType {
        RESISTOR("R", "Rezystor", 220, 110),
        CAPACITOR("C", "Kondensator", 176, 234),
        INDUCTOR("L", "Cewka", 248, 110),
        VOLTAGE("V", "Źródło", 172, 334),
        GROUND("GND", "Masa", 136, 104),
        DIODE("D", "Dioda", 180, 110),
        OPAMP("U", "Wzmacniacz", 220, 180);

        private final String prefix;
        private final String label;
        private final double width;
        private final double height;

        ElementType(String prefix, String label, double width, double height) {
            this.prefix = prefix;
            this.label = label;
            this.width = width;
            this.height = height;
        }

        public String prefix() {
            return prefix;
        }

        public String label() {
            return label;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }
    }

    public record WorkspaceElement(String id, ElementType type, double left, double top) {
    }

    public record PinRef(String elementId, String pinKey) {
    }

    public record WorkspaceWire(String id, PinRef start, PinRef end) {
    }

    public record PinPosition(String elementId, String pinKey, double x, double y) {
    }

    public record WirePoint(double x, double y) {
    }

    public record WireSegment(double x1, double y1, double x2, double y2) {
    }

    public record ResolvedWire(
            String id,
            PinRef start,
            PinRef end,
            List<WireSegment> segments,
            List<WirePoint> nodes) {
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
            String simulationNote,
            List<ResultRow> rows,
            String netlist,
            List<String> logs) {
    }

    public record ResultRow(String time, String voltage, String current, String note) {
    }

    private record ElementTemplate(
            String symbol,
            String typeLabel,
            String defaultValue,
            String nodeA,
            String nodeB,
            String orientation,
            String description,
            String traceTemplate,
            String peak,
            String min,
            String rms,
            String timeOfPeak,
            String simulationNote,
            List<ResultRow> rows,
            String netlistTemplate,
            List<String> logsTemplate) {

        private ElementDetails describe(WorkspaceElement element) {
            String traceName = traceTemplate.formatted(element.id());
            String netlist = format(netlistTemplate, element.id());
            List<String> logs = logsTemplate.stream()
                    .map(line -> format(line, element.id()))
                    .toList();
            return new ElementDetails(
                    element.id(),
                    symbol,
                    typeLabel,
                    defaultValue,
                    nodeA,
                    nodeB,
                    orientation,
                    description,
                    traceName,
                    peak,
                    min,
                    rms,
                    timeOfPeak,
                    simulationNote,
                    rows,
                    netlist,
                    logs);
        }

        private static String format(String template, String elementId) {
            int placeholders = template.split("%s", -1).length - 1;
            return switch (placeholders) {
                case 0 -> template;
                case 1 -> template.formatted(elementId);
                case 2 -> template.formatted(elementId, elementId);
                case 3 -> template.formatted(elementId, elementId, elementId);
                default -> {
                    Object[] args = new Object[placeholders];
                    Arrays.fill(args, elementId);
                    yield template.formatted(args);
                }
            };
        }
    }
}
