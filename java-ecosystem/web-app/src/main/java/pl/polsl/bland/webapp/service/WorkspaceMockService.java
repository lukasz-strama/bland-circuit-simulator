package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class WorkspaceMockService {
    private static final double SHEET_WIDTH = 1280;
    private static final double SHEET_HEIGHT = 860;
    private static final double GRID_LEFT = 36;
    private static final double GRID_TOP = 36;
    private static final double GRID_RIGHT = 1244;
    private static final double GRID_BOTTOM = 806;
    private static final double GRID_STEP = 16;
    public static final String SOURCE_TYPE_DC = "dc";
    public static final String SOURCE_TYPE_SINE = "sine";

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
                "5.0",
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

        templates.put(ElementType.CURRENT, new ElementTemplate(
                "I",
                "Źródło prądu",
                "0.02",
                "IN",
                "OUT",
                "Pionowo",
                "Źródło prądowe wymuszające przepływ prądu od pinu dodatniego do ujemnego.",
                "V(%s)",
                "10,0 V",
                "0,0 V",
                "5,8 V",
                "0,00 ms",
                "Źródło prądowe wymusza zadany prąd, więc odpowiedź napięciowa zależy od obciążenia i topologii połączeń.",
                List.of(
                        new ResultRow("0,00 ms", "10,00 V", "20,0 mA", "Stan ustalony DC"),
                        new ResultRow("1,00 ms", "10,00 V", "20,0 mA", "Brak zmian"),
                        new ResultRow("4,00 ms", "10,00 V", "20,0 mA", "Prąd wymuszony")),
                """
                        * Netlista źródła prądowego
                        %s IN OUT type=dc val=0.02
                        R1 OUT 0 500
                        .probe V(OUT)
                        """,
                List.of(
                        "Wybrano element %s.",
                        "Ślad aktywny: V(OUT).",
                        "Model źródła prądowego jest obecnie mockowany po stronie frontendu.")));

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
        elements.put("R1", createSeedElement("R1", ElementType.RESISTOR, 270, 192));
        elements.put("L1", createSeedElement("L1", ElementType.INDUCTOR, 590, 192));
        elements.put("C1", createSeedElement("C1", ElementType.CAPACITOR, 892, 194));
        elements.put("V1", createSeedElement("V1", ElementType.VOLTAGE, 110, 194));
        elements.put("GND", createSeedElement("GND", ElementType.GROUND, 138, 514));
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
        String sourceType = defaultSourceType(type);
        return new WorkspaceElement(
                nextId,
                type,
                left,
                top,
                defaultValue(type),
                sourceType,
                defaultFrequency(type, sourceType));
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

    public Optional<WorkspaceWire> reconnectWire(
            Map<String, WorkspaceElement> elements,
            Collection<WorkspaceWire> existingWires,
            WorkspaceWire wire,
            WireEndpoint endpoint,
            PinRef replacementPin) {
        if (wire == null || endpoint == null) {
            return Optional.empty();
        }

        PinRef start = endpoint == WireEndpoint.START ? replacementPin : wire.start();
        PinRef end = endpoint == WireEndpoint.END ? replacementPin : wire.end();
        if (start.equals(end) || resolvePin(elements, start).isEmpty() || resolvePin(elements, end).isEmpty()) {
            return Optional.empty();
        }

        boolean duplicate = existingWires.stream()
                .filter(existing -> !existing.id().equals(wire.id()))
                .anyMatch(existing -> connectsSamePins(existing, start, end));
        if (duplicate) {
            return Optional.empty();
        }

        return Optional.of(new WorkspaceWire(wire.id(), start, end));
    }

    public WorkspaceElement moveElement(WorkspaceElement element, double deltaX, double deltaY) {
        double left = clamp(snap(element.left() + deltaX), GRID_LEFT, GRID_RIGHT - element.type().width());
        double top = clamp(snap(element.top() + deltaY), GRID_TOP, GRID_BOTTOM - element.type().height());
        return new WorkspaceElement(
                element.id(),
                element.type(),
                left,
                top,
                element.value(),
                element.sourceType(),
                element.frequency());
    }

    public WorkspaceElement updateElementValue(WorkspaceElement element, String value) {
        return new WorkspaceElement(
                element.id(),
                element.type(),
                element.left(),
                element.top(),
                normalizeValue(element.type(), value),
                element.sourceType(),
                element.frequency());
    }

    public String defaultValue(ElementType type) {
        ElementTemplate template = templates.get(type);
        return template == null ? "" : template.defaultValue();
    }

    public WorkspaceElement updateSourceType(WorkspaceElement element, String sourceType) {
        if (!element.isSource()) {
            return element;
        }

        String normalizedType = normalizeSourceType(sourceType);
        Double frequency = SOURCE_TYPE_SINE.equals(normalizedType)
                ? normalizeFrequencyValue(element.type(), element.frequency(), true)
                : null;
        return new WorkspaceElement(
                element.id(),
                element.type(),
                element.left(),
                element.top(),
                element.value(),
                normalizedType,
                frequency);
    }

    public WorkspaceElement updateSourceFrequency(WorkspaceElement element, Double frequency) {
        if (!element.isSource()) {
            return element;
        }

        String normalizedType = normalizeSourceType(element.sourceType());
        return new WorkspaceElement(
                element.id(),
                element.type(),
                element.left(),
                element.top(),
                element.value(),
                normalizedType,
                SOURCE_TYPE_SINE.equals(normalizedType)
                        ? normalizeFrequencyValue(element.type(), frequency, true)
                        : null);
    }

    public String defaultSourceType(ElementType type) {
        return switch (type) {
            case VOLTAGE -> SOURCE_TYPE_SINE;
            case CURRENT -> SOURCE_TYPE_DC;
            default -> null;
        };
    }

    public Double defaultFrequency(ElementType type, String sourceType) {
        if (!type.isSource()) {
            return null;
        }
        return SOURCE_TYPE_SINE.equals(normalizeSourceType(sourceType))
                ? defaultSineFrequency(type)
                : null;
    }

    public static String normalizeSourceType(String sourceType) {
        String candidate = sourceType == null ? "" : sourceType.trim().toLowerCase();
        return SOURCE_TYPE_SINE.equals(candidate) ? SOURCE_TYPE_SINE : SOURCE_TYPE_DC;
    }

    public static String formatSourceTypeLabel(String sourceType) {
        return SOURCE_TYPE_SINE.equals(normalizeSourceType(sourceType)) ? "sinus" : "dc";
    }

    public static String formatFrequencyLabel(Double frequency) {
        return frequency == null ? "-" : formatNumericValue(frequency) + " Hz";
    }

    public List<ResolvedWire> resolveWires(Map<String, WorkspaceElement> elements, Collection<WorkspaceWire> wires) {
        List<ResolvedWire> resolved = new ArrayList<>();
        for (WorkspaceWire wire : wires) {
            resolveWire(elements, wire).ifPresent(resolved::add);
        }
        return resolved;
    }

    public NetTopology resolveNetTopology(
            Map<String, WorkspaceElement> elements,
            Collection<WorkspaceWire> wires,
            Map<String, String> aliases) {
        LinkedHashMap<PinRef, PinPosition> pins = new LinkedHashMap<>();
        LinkedHashMap<PinRef, List<PinRef>> adjacency = new LinkedHashMap<>();

        for (WorkspaceElement element : elements.values()) {
            for (PinPosition pin : pinsForElement(element)) {
                PinRef pinRef = new PinRef(pin.elementId(), pin.pinKey());
                pins.put(pinRef, pin);
                adjacency.put(pinRef, new ArrayList<>());
            }
        }

        for (WorkspaceWire wire : wires) {
            if (!pins.containsKey(wire.start()) || !pins.containsKey(wire.end())) {
                continue;
            }
            adjacency.get(wire.start()).add(wire.end());
            adjacency.get(wire.end()).add(wire.start());
        }

        LinkedHashMap<PinRef, String> pinNetNames = new LinkedHashMap<>();
        LinkedHashMap<PinRef, String> pinNetKeys = new LinkedHashMap<>();
        LinkedHashMap<String, ResolvedNet> netsByKey = new LinkedHashMap<>();
        List<ResolvedNet> nets = new ArrayList<>();
        Set<PinRef> visited = new LinkedHashSet<>();
        Set<String> usedNames = new LinkedHashSet<>();
        int nextGenericIndex = 1;

        for (PinRef start : pins.keySet()) {
            if (visited.contains(start)) {
                continue;
            }

            List<PinRef> component = collectConnectedPins(start, adjacency, visited);
            String netKey = buildNetKey(component);
            String preferredName = preferredNetName(component, elements);
            String aliasName = aliases.get(netKey);
            String netName = aliasName == null || aliasName.isBlank() ? preferredName : aliasName.trim();
            if (netName == null || usedNames.contains(netName)) {
                do {
                    netName = "N%03d".formatted(nextGenericIndex++);
                } while (usedNames.contains(netName));
            }

            usedNames.add(netName);
            String resolvedNetName = netName;
            component.forEach(pin -> {
                pinNetNames.put(pin, resolvedNetName);
                pinNetKeys.put(pin, netKey);
            });
            ResolvedNet resolvedNet = createResolvedNet(netKey, resolvedNetName, component, pins);
            nets.add(resolvedNet);
            netsByKey.put(netKey, resolvedNet);
        }

        return new NetTopology(pinNetNames, pinNetKeys, netsByKey, nets);
    }

    public Optional<ElementDetails> describeElement(
            Map<String, WorkspaceElement> elements,
            NetTopology topology,
            WorkspaceElement element) {
        ElementTemplate template = templates.get(element.type());
        if (template == null) {
            return Optional.empty();
        }
        return Optional.of(template.describe(element, elements.values(), topology));
    }

    public Optional<WireDetails> describeWire(
            Map<String, WorkspaceElement> elements,
            NetTopology topology,
            WorkspaceWire wire) {
        return resolveWire(elements, wire).map(resolvedWire -> {
            String startPin = formatPin(wire.start());
            String endPin = formatPin(wire.end());
            String startNet = topology.netName(wire.start(), "?");
            String endNet = topology.netName(wire.end(), "?");
            String geometry = describeWireGeometry(resolvedWire);
            String description = startNet.equals(endNet)
                    ? "Przewód utrzymuje ciągłość netu " + startNet + " pomiędzy " + startPin + " i " + endPin + "."
                    : "Przewód łączy " + startPin + " z " + endPin + " i będzie ponownie przeliczony do wspólnego netu.";
            String netlist = "* Połączenie " + wire.id()
                    + System.lineSeparator()
                    + wire.id() + " " + startPin + " " + endPin + " ; net " + startNet;
            List<String> logs = List.of(
                    "Początek przewodu: " + startPin + " (" + startNet + ")",
                    "Koniec przewodu: " + endPin + " (" + endNet + ")",
                    "Geometria: " + geometry,
                    "Segmenty ścieżki: " + resolvedWire.segments().size());
            return new WireDetails(wire.id(), startPin, endPin, startNet, endNet, geometry, description, netlist, logs);
        });
    }

    public Optional<WorkspaceElement> firstElement(Map<String, WorkspaceElement> elements) {
        return elements.values().stream().findFirst();
    }

    public boolean isWireAttachedToElement(WorkspaceWire wire, String elementId) {
        return wire.start().elementId().equals(elementId) || wire.end().elementId().equals(elementId);
    }

    public String summarizeNets(NetTopology topology) {
        return summarizeNetNames(topology);
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

    private WorkspaceElement createSeedElement(String id, ElementType type, double left, double top) {
        String sourceType = defaultSourceType(type);
        return new WorkspaceElement(
                id,
                type,
                left,
                top,
                defaultValue(type),
                sourceType,
                defaultFrequency(type, sourceType));
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

    private List<PinRef> collectConnectedPins(
            PinRef start,
            Map<PinRef, List<PinRef>> adjacency,
            Set<PinRef> visited) {
        ArrayDeque<PinRef> queue = new ArrayDeque<>();
        List<PinRef> connected = new ArrayList<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            PinRef current = queue.removeFirst();
            connected.add(current);
            for (PinRef next : adjacency.getOrDefault(current, List.of())) {
                if (visited.add(next)) {
                    queue.addLast(next);
                }
            }
        }

        return connected;
    }

    private ResolvedNet createResolvedNet(String netKey, String netName, List<PinRef> component, Map<PinRef, PinPosition> pins) {
        PinPosition anchor = null;
        for (PinRef pinRef : component) {
            PinPosition candidate = pins.get(pinRef);
            if (candidate == null) {
                continue;
            }
            if (anchor == null
                    || candidate.y() < anchor.y()
                    || (sameCoordinate(candidate.y(), anchor.y()) && candidate.x() < anchor.x())) {
                anchor = candidate;
            }
        }

        double labelX = anchor == null ? GRID_LEFT : anchor.x() + 10;
        double labelY = anchor == null ? GRID_TOP : Math.max(GRID_TOP - 8, anchor.y() - 18);
        return new ResolvedNet(netKey, netName, List.copyOf(component), labelX, labelY);
    }

    private String buildNetKey(List<PinRef> component) {
        return component.stream()
                .map(pin -> pin.elementId() + ":" + pin.pinKey())
                .sorted()
                .reduce((left, right) -> left + "|" + right)
                .orElse("net-empty");
    }

    private String normalizeValue(ElementType type, String value) {
        String candidate = value == null ? "" : value.trim();
        return candidate.isBlank() ? defaultValue(type) : candidate;
    }

    private String formatPin(PinRef pinRef) {
        return pinRef.elementId() + ":" + pinRef.pinKey();
    }

    private String preferredNetName(List<PinRef> component, Map<String, WorkspaceElement> elements) {
        if (containsPin(component, elements, ElementType.GROUND, "REF")) {
            return "0";
        }
        if (containsPin(component, elements, ElementType.VOLTAGE, "POS")) {
            return "IN";
        }
        if (containsPin(component, elements, ElementType.CURRENT, "POS")) {
            return "ISRC";
        }
        if (containsPin(component, elements, ElementType.OPAMP, "OUT")) {
            return "OUT";
        }
        if (containsPin(component, elements, ElementType.OPAMP, "IN+")) {
            return "INP";
        }
        if (containsPin(component, elements, ElementType.OPAMP, "IN-")) {
            return "INN";
        }
        return null;
    }

    private boolean containsPin(List<PinRef> component, Map<String, WorkspaceElement> elements, ElementType type, String pinKey) {
        return component.stream().anyMatch(pinRef -> {
            WorkspaceElement element = elements.get(pinRef.elementId());
            return element != null && element.type() == type && pinRef.pinKey().equals(pinKey);
        });
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

    private String describeWireGeometry(ResolvedWire resolvedWire) {
        if (resolvedWire.segments().size() <= 1) {
            return "odcinek prosty";
        }
        return "łamany / " + resolvedWire.segments().size() + " segmenty";
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
            case VOLTAGE, CURRENT -> switch (pinKey) {
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

    private List<PinPosition> pinsForElement(WorkspaceElement element) {
        List<PinPosition> pins = new ArrayList<>();
        for (String pinKey : pinKeys(element.type())) {
            resolvePin(element, pinKey).ifPresent(pins::add);
        }
        return pins;
    }

    private List<String> pinKeys(ElementType type) {
        return switch (type) {
            case RESISTOR, INDUCTOR, CAPACITOR -> List.of("A", "B");
            case VOLTAGE, CURRENT -> List.of("POS", "NEG");
            case GROUND -> List.of("REF");
            case DIODE -> List.of("ANODE", "CATHODE");
            case OPAMP -> List.of("IN+", "IN-", "OUT");
        };
    }

    private static Optional<PinRef> primaryPinA(WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR -> Optional.of(new PinRef(element.id(), "A"));
            case VOLTAGE, CURRENT -> Optional.of(new PinRef(element.id(), "POS"));
            case GROUND -> Optional.of(new PinRef(element.id(), "REF"));
            case DIODE -> Optional.of(new PinRef(element.id(), "ANODE"));
            case OPAMP -> Optional.of(new PinRef(element.id(), "IN+"));
        };
    }

    private static Optional<PinRef> primaryPinB(WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR -> Optional.of(new PinRef(element.id(), "B"));
            case VOLTAGE, CURRENT -> Optional.of(new PinRef(element.id(), "NEG"));
            case GROUND -> Optional.empty();
            case DIODE -> Optional.of(new PinRef(element.id(), "CATHODE"));
            case OPAMP -> Optional.of(new PinRef(element.id(), "IN-"));
        };
    }

    private static String resolveNodeName(NetTopology topology, Optional<PinRef> pinRef, String fallback) {
        return pinRef.map(ref -> topology.netName(ref, fallback)).orElse(fallback);
    }

    private static String buildTraceName(WorkspaceElement element, NetTopology topology, String nodeA, String nodeB) {
        return switch (element.type()) {
            case RESISTOR, INDUCTOR, VOLTAGE, DIODE -> "I(" + element.id() + ")";
            case CAPACITOR -> "V(" + preferredVoltageNet(nodeA, nodeB) + ")";
            case CURRENT -> "V(" + preferredVoltageNet(nodeA, nodeB) + ")";
            case GROUND -> "V(0)";
            case OPAMP -> "V(" + topology.netName(new PinRef(element.id(), "OUT"), "OUT") + ")";
        };
    }

    private static String buildWorkspaceNetlist(
            Collection<WorkspaceElement> elements,
            NetTopology topology,
            WorkspaceElement focusedElement,
            String traceName) {
        StringBuilder builder = new StringBuilder();
        builder.append("* Mockowany netlist frontendowy\n");
        builder.append("* Aktywny element: ").append(focusedElement.id()).append('\n');
        builder.append("* Wykryte nety: ").append(summarizeNetNames(topology)).append('\n');

        for (WorkspaceElement element : elements) {
            if (element.id().equals(focusedElement.id())) {
                builder.append("* > ");
            }
            builder.append(formatNetlistLine(element, topology)).append('\n');
        }

        builder.append(".probe ").append(traceName).append('\n');
        builder.append(".simulate type=transient tstop=0.008 tstep=0.0001");
        return builder.toString();
    }

    private static String formatNetlistLine(WorkspaceElement element, NetTopology topology) {
        String nodeA = resolveNodeName(topology, primaryPinA(element), "NC_A");
        String nodeB = resolveNodeName(topology, primaryPinB(element), "NC_B");
        String value = element.value();

        return switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR, DIODE -> element.id() + " " + nodeA + " " + nodeB + " " + value;
            case VOLTAGE -> formatSourceNetlistLine("VSRC", element, nodeA, nodeB);
            case CURRENT -> formatSourceNetlistLine("ISRC", element, nodeA, nodeB);
            case GROUND -> "* " + element.id() + " -> net 0";
            case OPAMP -> "X" + element.id()
                    + " " + topology.netName(new PinRef(element.id(), "IN+"), "INP")
                    + " " + topology.netName(new PinRef(element.id(), "IN-"), "INN")
                    + " " + topology.netName(new PinRef(element.id(), "OUT"), "OUT")
                    + " " + value;
        };
    }

    private static String preferredVoltageNet(String nodeA, String nodeB) {
        if (!"0".equals(nodeA)) {
            return nodeA;
        }
        return nodeB;
    }

    private static String summarizeNetNames(NetTopology topology) {
        if (topology.nets().isEmpty()) {
            return "brak połączeń";
        }
        return topology.nets().stream()
                .map(ResolvedNet::name)
                .reduce((left, right) -> left + ", " + right)
                .orElse("brak połączeń");
    }

    private static String formatSourceNetlistLine(String keyword, WorkspaceElement element, String nodeA, String nodeB) {
        String normalizedSourceType = normalizedSourceType(element.sourceType());
        StringBuilder builder = new StringBuilder()
                .append(keyword)
                .append(' ')
                .append(element.id())
                .append(' ')
                .append(nodeA)
                .append(' ')
                .append(nodeB)
                .append(" type=")
                .append(normalizedSourceType)
                .append(" val=")
                .append(element.value());

        if (SOURCE_TYPE_SINE.equals(normalizedSourceType) && element.frequency() != null) {
            builder.append(" freq=").append(formatNumericValue(element.frequency()));
        }
        return builder.toString();
    }

    private static String normalizedSourceType(String sourceType) {
        if (sourceType != null && SOURCE_TYPE_SINE.equalsIgnoreCase(sourceType.trim())) {
            return SOURCE_TYPE_SINE;
        }
        return SOURCE_TYPE_DC;
    }

    private static String formatNumericValue(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }

    private static double defaultSineFrequency(ElementType type) {
        return type == ElementType.VOLTAGE ? 1000.0 : 50.0;
    }

    private static Double normalizeFrequencyValue(ElementType type, Double frequency, boolean fallbackToDefault) {
        if (frequency != null && frequency > 0) {
            return frequency;
        }
        return fallbackToDefault ? defaultSineFrequency(type) : null;
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
        VOLTAGE("V", "Źródło napięcia", 172, 334),
        CURRENT("I", "Źródło prądu", 172, 334),
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

        public boolean isSource() {
            return this == VOLTAGE || this == CURRENT;
        }
    }

    public record WorkspaceElement(
            String id,
            ElementType type,
            double left,
            double top,
            String value,
            String sourceType,
            Double frequency) {
        public boolean isSource() {
            return type.isSource();
        }
    }

    public record PinRef(String elementId, String pinKey) {
    }

    public record WorkspaceWire(String id, PinRef start, PinRef end) {
    }

    public enum WireEndpoint {
        START("Początek"),
        END("Koniec");

        private final String label;

        WireEndpoint(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
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

    public record ResolvedNet(
            String key,
            String name,
            List<PinRef> members,
            double labelX,
            double labelY) {
    }

    public record NetTopology(
            Map<PinRef, String> pinNetNames,
            Map<PinRef, String> pinNetKeys,
            Map<String, ResolvedNet> netsByKey,
            List<ResolvedNet> nets) {
        public static NetTopology empty() {
            return new NetTopology(Map.of(), Map.of(), Map.of(), List.of());
        }

        public String netName(PinRef pinRef, String fallback) {
            return pinNetNames.getOrDefault(pinRef, fallback);
        }

        public String netKey(PinRef pinRef) {
            return pinNetKeys.get(pinRef);
        }

        public Optional<ResolvedNet> findNet(String netKey) {
            return Optional.ofNullable(netsByKey.get(netKey));
        }

        public boolean hasNet(String netKey) {
            return netsByKey.containsKey(netKey);
        }
    }

    public record ElementDetails(
            String id,
            String symbol,
            String typeLabel,
            String value,
            String sourceType,
            String frequency,
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

    public record WireDetails(
            String id,
            String startPin,
            String endPin,
            String startNet,
            String endNet,
            String geometry,
            String description,
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

        private ElementDetails describe(
                WorkspaceElement element,
                Collection<WorkspaceElement> allElements,
                NetTopology topology) {
            String resolvedNodeA = resolveNodeName(topology, primaryPinA(element), nodeA);
            String resolvedNodeB = resolveNodeName(topology, primaryPinB(element), nodeB);
            String resolvedValue = element.value();
            String traceName = buildTraceName(element, topology, resolvedNodeA, resolvedNodeB);
            String netlist = buildWorkspaceNetlist(allElements, topology, element, traceName);
            List<String> logs = logsTemplate.stream()
                    .map(line -> format(line, element.id()))
                    .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
            logs.add("Aktualna wartość: " + resolvedValue + ".");
            if (element.isSource()) {
                logs.add("Typ źródła: " + formatSourceTypeLabel(element.sourceType()) + ".");
                logs.add("Częstotliwość: " + formatFrequencyLabel(element.frequency()) + ".");
            }
            logs.add("Połączenia aktywne: A=" + resolvedNodeA + ", B=" + resolvedNodeB + ".");
            logs.add("Dostępne nety: " + summarizeNetNames(topology) + ".");
            return new ElementDetails(
                    element.id(),
                    symbol,
                    typeLabel,
                    resolvedValue,
                    element.isSource() ? formatSourceTypeLabel(element.sourceType()) : "-",
                    element.isSource() ? formatFrequencyLabel(element.frequency()) : "-",
                    resolvedNodeA,
                    resolvedNodeB,
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
