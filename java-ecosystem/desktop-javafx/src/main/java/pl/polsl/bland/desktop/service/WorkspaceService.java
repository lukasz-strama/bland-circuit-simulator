package pl.polsl.bland.desktop.service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.Wire;
import pl.polsl.bland.models.*;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.SimulationResult;
import pl.polsl.bland.desktop.view.MainView;

/**
 * Główna logika edytora schematów.
 *
 * Odpowiada za:
 * - tworzenie elementów i przewodów,
 * - obliczanie pozycji pinów (z uwzględnieniem rotacji),
 * - mapowanie elementów edytora na model domenowy (CircuitSchematic),
 * - import schematu z backendu do formatu edytora,
 * - wykrywanie połączeń elektrycznych (Union-Find),
 * - generowanie nazw węzłów (N1, N2, ... oraz 0 dla masy).
 *
 * Klasa ta łączy UI z modelem symulacji.
 */
public class WorkspaceService {

    /**
     * Typy elementów dostępnych w edytorze.
     */
    public enum ElementType {
        RESISTOR, CAPACITOR, INDUCTOR, VOLTAGE, CURRENT, GROUND
    }

    /**
     * Reprezentuje pojedynczy pin elementu w przestrzeni roboczej.
     *
     * @param key nazwa pinu (np. A, B, POS, NEG, REF)
     * @param x współrzędna X pinu
     * @param y współrzędna Y pinu
     */
    public record Pin(String key, double x, double y) {}

    /**
     * Reprezentuje element umieszczony na płótnie edytora.
     *
     * @param id unikalny identyfikator (np. R1, C3)
     * @param type typ elementu
     * @param x pozycja X
     * @param y pozycja Y
     * @param value wartość elementu (np. 1000, 1e-6)
     * @param rotation rotacja w stopniach (0, 90, 180, 270)
     */
    public record WorkspaceElement(
            String id,
            ElementType type,
            double x,
            double y,
            String value,
            double rotation
    ) {
         /**
         * Zwraca listę pinów elementu po uwzględnieniu rotacji.
         *
         * @return lista pinów w globalnych współrzędnych
         */
        public List<Pin> pins() {
            double angle = rotation();
            double cx = x();
            double cy = y();

            List<Pin> basePins = switch (type) {
                case RESISTOR, INDUCTOR -> List.of(
                        new Pin("A", cx, cy),
                        new Pin("B", cx + 70, cy)
                );
                case CAPACITOR -> List.of(
                        new Pin("A", cx, cy),
                        new Pin("B", cx, cy + 40)
                );
                case VOLTAGE, CURRENT -> List.of(
                        new Pin("POS", cx, cy),
                        new Pin("NEG", cx, cy + 50)
                );
                case GROUND -> List.of(
                        new Pin("REF", cx, cy)
                );
            };

            List<Pin> rotated = new ArrayList<>();
            for (Pin p : basePins) {
                double[] r = rotatePoint(p.x(), p.y(), cx, cy, angle);
                rotated.add(new Pin(p.key(), r[0], r[1]));
            }

            return rotated;
        }
    }

     /**
     * Reprezentuje przewód łączący dwa piny elementów.
     *
     * @param id identyfikator przewodu
     * @param elementA pierwszy element
     * @param pinA pin pierwszego elementu
     * @param elementB drugi element
     * @param pinB pin drugiego elementu
     */
    public record WorkspaceWire(
            String id,
            String elementA,
            String pinA,
            String elementB,
            String pinB
    ) {}

    private final AtomicInteger idCounter = new AtomicInteger(1);

    /**
     * Tworzy początkowy zestaw elementów (np. R1, C1) widoczny po uruchomieniu aplikacji.
     *
     * @return mapa elementów
     */
    public Map<String, WorkspaceElement> createInitialWorkspace() {
        Map<String, WorkspaceElement> map = new LinkedHashMap<>();
        map.put("R1", new WorkspaceElement("R1", ElementType.RESISTOR, 200, 200, "1000", 0));
        map.put("C1", new WorkspaceElement("C1", ElementType.CAPACITOR, 400, 200, "1e-6", 0));
        return map;
    }

    /**
     * Tworzy pustą listę przewodów.
     *
     * @return mapa przewodów
     */
    public Map<String, WorkspaceWire> createInitialWires() {
        return new LinkedHashMap<>();
    }

    /**
     * Tworzy nowy element o podanym typie i pozycji.
     *
     * @param type typ elementu
     * @param x pozycja X
     * @param y pozycja Y
     * @return nowy element
     */
    public WorkspaceElement createElement(ElementType type, double x, double y) {
        String id = type.name().charAt(0) + String.valueOf(idCounter.getAndIncrement());
        return new WorkspaceElement(id, type, x, y, defaultValue(type), 0);
    }

    /**
     * Przesuwa element o zadany wektor.
     *
     * @param el element
     * @param dx przesunięcie X
     * @param dy przesunięcie Y
     * @return nowy element z przesuniętą pozycją
     */
    public WorkspaceElement moveElement(WorkspaceElement el, double dx, double dy) {
        return new WorkspaceElement(
                el.id(),
                el.type(),
                el.x() + dx,
                el.y() + dy,
                el.value(),
                el.rotation()
        );
    }

    /**
     * Obraca element o 90 stopni zgodnie z ruchem wskazówek zegara.
     *
     * @param el element
     * @return nowy element z obróconą rotacją
     */
    public WorkspaceElement rotateElement(WorkspaceElement el) {
        return new WorkspaceElement(
                el.id(),
                el.type(),
                el.x(),
                el.y(),
                el.value(),
                (el.rotation() + 90) % 360
        );
    }

    private static double[] rotatePoint(double px, double py, double cx, double cy, double angle) {
        double rad = Math.toRadians(angle);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        px -= cx;
        py -= cy;

        double xnew = px * cos - py * sin;
        double ynew = px * sin + py * cos;

        return new double[]{xnew + cx, ynew + cy};
    }

    private String defaultValue(ElementType type) {
        return switch (type) {
            case RESISTOR -> "1000";
            case CAPACITOR -> "1e-6";
            case INDUCTOR -> "1e-3";
            case VOLTAGE -> "5.0";
            case CURRENT -> "0.01";
            case GROUND -> "";
        };
    }

    private static class NodeRef {
    String elementId;
    String pinKey;

    NodeRef(String e, String p) {
        this.elementId = e;
        this.pinKey = p;
    }

    @Override
    public String toString() {
        return elementId + "." + pinKey;
    }
}

private static class UnionFind {
    int[] parent;

    UnionFind(int n) {
        parent = new int[n];
        for (int i = 0; i < n; i++) parent[i] = i;
    }

    int find(int x) {
        if (parent[x] != x) parent[x] = find(parent[x]);
        return parent[x];
    }

    void union(int a, int b) {
        int ra = find(a);
        int rb = find(b);
        if (ra != rb) parent[rb] = ra;
    }
}

/**
     * Eksportuje aktualny stan edytora do modelu domenowego CircuitSchematic.
     *
     * Wykonuje:
     * - analizę połączeń pinów (Union-Find),
     * - generowanie nazw węzłów,
     * - mapowanie elementów edytora na CircuitElement,
     * - mapowanie przewodów na Wire.
     *
     * @param elements mapa elementów edytora
     * @param wires mapa przewodów
     * @return obiekt CircuitSchematic gotowy do symulacji lub zapisu
     */
public CircuitSchematic exportToSchematic(
        Map<String, WorkspaceElement> elements,
        Map<String, WorkspaceWire> wires
) {
    // 1. Zbieramy wszystkie piny (łącznie z REF dla GND)
    Map<String, NodeRef> allPins = new LinkedHashMap<>();
    for (var el : elements.values()) {
        if (el.type() == ElementType.GROUND) {
            allPins.put(el.id() + ".REF", new NodeRef(el.id(), "REF"));
        } else {
            for (var p : el.pins()) {
                allPins.put(el.id() + "." + p.key(), new NodeRef(el.id(), p.key()));
            }
        }
    }

    // 2. Union-Find po pinach
    UnionFind uf = new UnionFind(allPins.size());
    List<String> pinKeys = new ArrayList<>(allPins.keySet());
    Map<String, Integer> pinIndex = new HashMap<>();
    for (int i = 0; i < pinKeys.size(); i++) {
        pinIndex.put(pinKeys.get(i), i);
    }

    for (var w : wires.values()) {
        String a = w.elementA() + "." + w.pinA();
        String b = w.elementB() + "." + w.pinB();

        if (elements.get(w.elementA()).type() == ElementType.GROUND) {
            a = w.elementA() + ".REF";
        }
        if (elements.get(w.elementB()).type() == ElementType.GROUND) {
            b = w.elementB() + ".REF";
        }

        uf.union(pinIndex.get(a), pinIndex.get(b));
    }

    // 3. Grupowanie pinów w nety
    Map<Integer, List<String>> groupPins = new HashMap<>();
    for (String pin : pinKeys) {
        int root = uf.find(pinIndex.get(pin));
        groupPins.computeIfAbsent(root, k -> new ArrayList<>()).add(pin);
    }

    Map<String, String> pinToNode = new HashMap<>();
    int counter = 1;
    for (var entry : groupPins.entrySet()) {
        List<String> pinsInGroup = entry.getValue();

        boolean isGroundGroup = pinsInGroup.stream().anyMatch(pin -> {
            String[] parts = pin.split("\\.");
            String elId = parts[0];
            String pinKey = parts[1];
            WorkspaceElement el = elements.get(elId);
            return el != null && el.type() == ElementType.GROUND && "REF".equals(pinKey);
        });

        String nodeName = isGroundGroup ? "0" : "N" + counter++;
        for (String pin : pinsInGroup) {
            pinToNode.put(pin, nodeName);
        }
    }

    // 4. Elementy
    List<CircuitElement> ce = new ArrayList<>();
    for (var el : elements.values()) {
        boolean isGnd = el.type() == ElementType.GROUND;

        if (isGnd) {
            // GND jako osobny typ, node1=node2=0, value=0
            ce.add(new CircuitElement(
                    el.id(),
                    CircuitElement.ElementType.GND,
                    "0",
                    "0",
                    0.0,
                    null, null,
                    (int) el.x(), (int) el.y(), (int) el.rotation()
            ));
            continue;
        }

        List<Pin> pins = el.pins();
        String node1 = pinToNode.get(el.id() + "." + pins.get(0).key());
        String node2 = (pins.size() >= 2)
                ? pinToNode.get(el.id() + "." + pins.get(1).key())
                : "0";

        if (node1 == null) node1 = "NC_" + el.id() + "_1";
        if (node2 == null) node2 = "NC_" + el.id() + "_2";

        ce.add(new CircuitElement(
                el.id(),
                mapType(el.type()),
                node1,
                node2,
                Double.parseDouble(el.value()),
                null, null,
                (int) el.x(), (int) el.y(), (int) el.rotation()
        ));
    }

    // 5. Wires – po pinach, nie po środkach elementów
    List<Wire> ww = new ArrayList<>();
    for (var w : wires.values()) {
        WorkspaceElement elA = elements.get(w.elementA());
        WorkspaceElement elB = elements.get(w.elementB());
        if (elA == null || elB == null) continue;

        Pin pinA = findPin(elA, w.pinA());
        Pin pinB = findPin(elB, w.pinB());
        if (pinA == null || pinB == null) continue;

        String keyA = w.elementA() + "." + w.pinA();
        String keyB = w.elementB() + "." + w.pinB();
        String node = pinToNode.getOrDefault(keyA, pinToNode.get(keyB));
        if (node == null) node = "?";

        List<int[]> pts = List.of(
                new int[]{(int) pinA.x(), (int) pinA.y()},
                new int[]{(int) pinB.x(), (int) pinB.y()}
        );

        ww.add(new Wire(w.id(), node, pts));
    }

    return new CircuitSchematic(
            1L,
            "schemat",
            1L,
            ce,
            ww,
            Instant.now()
    );
}

/**
     * Importuje schemat z backendu do formatu edytora.
     *
     * @param sc schemat z backendu
     * @param elements mapa elementów do uzupełnienia
     * @param wires mapa przewodów do uzupełnienia
     */
public void importFromSchematic(
        CircuitSchematic sc,
        Map<String, WorkspaceElement> elements,
        Map<String, WorkspaceWire> wires
) {
    elements.clear();
    wires.clear();

    for (var el : sc.elements()) {
            boolean isGnd = el.type() == CircuitElement.ElementType.GND;
            elements.put(el.id(), new WorkspaceElement(
                    el.id(),
                    // POPRAWKA: mapTypeReverse przyjmuje ElementType, nie CircuitElement
                    isGnd ? ElementType.GROUND : mapTypeReverse(el.type()),
                    el.x(), el.y(),
                    isGnd ? "" : String.valueOf(el.value()),
                    el.rotation()
            ));

}


    int counter = 1;
    for (var w : sc.wires()) {

      
        int[] p1 = w.points().get(0);
        int[] p2 = w.points().get(w.points().size() - 1);

    
        var nearestA = findNearestPin(elements, p1[0], p1[1]);
        var nearestB = findNearestPin(elements, p2[0], p2[1]);


        if (nearestA == null || nearestB == null) {
            wires.put(w.id(),
                    new WorkspaceWire(
                            w.id(),
                            "?", "?", "?", "?"
                    ));
            continue;
        }

        wires.put(w.id(),
                new WorkspaceWire(
                        w.id(),
                        nearestA.elementId(),
                        nearestA.pinKey(),
                        nearestB.elementId(),
                        nearestB.pinKey()
                ));
    }
}

private Pin findPin(WorkspaceElement el, String pinKey) {
    if (el == null) return null;
    return el.pins().stream()
             .filter(p -> p.key().equals(pinKey))
             .findFirst()
             .orElse(null);
}

private String resolveGndPin(Map<String, WorkspaceElement> elements, String elementId, String pinKey) {
        WorkspaceElement el = elements.get(elementId);
        if (el != null && el.type() == ElementType.GROUND) return elementId + ".REF";
        return elementId + "." + pinKey;
    }

private record PinHit(String elementId, String pinKey, double dist) {}

private PinHit findNearestPin(Map<String, WorkspaceElement> elements, double x, double y) {
    PinHit best = null;

    for (var el : elements.values()) {
        for (var p : el.pins()) {
            double dx = p.x() - x;
            double dy = p.y() - y;
            double d = Math.hypot(dx, dy);

            if (best == null || d < best.dist()) {
                best = new PinHit(el.id(), p.key(), d);
            }
        }
    }


    if (best != null && best.dist() < 30) return best;
    return null;
}


private CircuitElement.ElementType mapType(ElementType t) {
    return switch (t) {
        case RESISTOR -> CircuitElement.ElementType.R;
        case CAPACITOR -> CircuitElement.ElementType.C;
        case INDUCTOR -> CircuitElement.ElementType.L;
        case VOLTAGE -> CircuitElement.ElementType.V;
        case CURRENT -> CircuitElement.ElementType.I;
        case GROUND -> CircuitElement.ElementType.GND;
    };
}

private WorkspaceService.ElementType mapTypeReverse(CircuitElement.ElementType el) {
    return switch (el) {
        case R -> ElementType.RESISTOR;
        case C -> ElementType.CAPACITOR;
        case L -> ElementType.INDUCTOR;
        case V -> ElementType.VOLTAGE;
        case I -> ElementType.CURRENT;
        case GND -> ElementType.GROUND;
    };
}

}
