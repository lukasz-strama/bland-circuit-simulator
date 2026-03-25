package pl.polsl.bland.desktop.service;

import java.util.*;
import java.util.stream.Collectors;

public class WorkspaceMockService {

  

    public enum ElementType {
        RESISTOR("R"),
        CAPACITOR("C"),
        INDUCTOR("L"),
        VOLTAGE("V"),
        CURRENT("I"),
        GROUND("GND");

        private final String prefix;

        ElementType(String prefix) {
            this.prefix = prefix;
        }

        public String prefix() {
            return prefix;
        }
    }

 

    public record WorkspaceElement(
            String id,
            ElementType type,
            double x,
            double y,
            double rotation,
            String value
    ) {
        public List<Pin> pins() {
            return switch (type) {
                case RESISTOR, CAPACITOR, INDUCTOR ->
                        List.of(
                                new Pin("A", x, y),
                                new Pin("B", x + 70, y)
                        );
                case VOLTAGE, CURRENT ->
                        List.of(
                                new Pin("POS", x, y),
                                new Pin("NEG", x + 80, y)
                        );
                case GROUND ->
                        List.of(
                                new Pin("REF", x, y)
                        );
            };
        }
    }

    public record WorkspaceWire(
            String id,
            String elementA,
            String pinA,
            String elementB,
            String pinB
    ) {}

    public record Pin(String key, double x, double y) {}

    public record WorkspaceSnapshot(
            Map<String, WorkspaceElement> elements,
            Map<String, WorkspaceWire> wires,
            Map<String, String> aliases
    ) {}

    public record NetTopology(
            Map<String, Integer> pinToNet,
            Map<Integer, Set<String>> netToPins
    ) {
        public static NetTopology empty() {
            return new NetTopology(Map.of(), Map.of());
        }
    }

  

    private int counter = 1;

    public Map<String, WorkspaceElement> createInitialWorkspace() {
        Map<String, WorkspaceElement> map = new LinkedHashMap<>();
        map.put("R1", new WorkspaceElement("R1", ElementType.RESISTOR, 200, 200, 0, "1000"));
        map.put("C1", new WorkspaceElement("C1", ElementType.CAPACITOR, 400, 200, 0, "1e-6"));
        return map;
    }

    public Map<String, WorkspaceWire> createInitialWires() {
        return new LinkedHashMap<>();
    }

    public WorkspaceElement createElement(ElementType type, double x, double y) {
        String id = type.prefix() + counter++;
        return new WorkspaceElement(id, type, x, y, 0, defaultValue(type));
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

    public WorkspaceElement moveElement(WorkspaceElement el, double dx, double dy) {
        return new WorkspaceElement(
                el.id(), el.type(),
                el.x() + dx, el.y() + dy,
                el.rotation(), el.value()
        );
    }


    public NetTopology resolveNetTopology(
            Map<String, WorkspaceElement> elements,
            Collection<WorkspaceWire> wires,
            Map<String, String> aliases
    ) {
        Map<String, Integer> pinToNet = new HashMap<>();
        Map<Integer, Set<String>> netToPins = new HashMap<>();

        int netCounter = 1;

        for (WorkspaceWire w : wires) {
            String pA = w.elementA() + ":" + w.pinA();
            String pB = w.elementB() + ":" + w.pinB();

            Integer netA = pinToNet.get(pA);
            Integer netB = pinToNet.get(pB);

            if (netA == null && netB == null) {
                int net = netCounter++;
                pinToNet.put(pA, net);
                pinToNet.put(pB, net);
                netToPins.put(net, new HashSet<>(List.of(pA, pB)));
            } else if (netA != null && netB == null) {
                pinToNet.put(pB, netA);
                netToPins.get(netA).add(pB);
            } else if (netA == null && netB != null) {
                pinToNet.put(pA, netB);
                netToPins.get(netB).add(pA);
            } else if (!netA.equals(netB)) {
                int min = Math.min(netA, netB);
                int max = Math.max(netA, netB);
                for (var entry : pinToNet.entrySet()) {
                    if (entry.getValue() == max) {
                        entry.setValue(min);
                        netToPins.get(min).add(entry.getKey());
                    }
                }
                netToPins.remove(max);
            }
        }

        return new NetTopology(pinToNet, netToPins);
    }

 

    public Collection<WorkspaceWire> resolveWires(
            Map<String, WorkspaceElement> elements,
            Collection<WorkspaceWire> wires,
            WireRoutingMode mode
    ) {
        return wires;
    }


    public WorkspaceSnapshot snapshot(
            Map<String, WorkspaceElement> elements,
            Map<String, WorkspaceWire> wires,
            Map<String, String> aliases
    ) {
        return new WorkspaceSnapshot(
                new LinkedHashMap<>(elements),
                new LinkedHashMap<>(wires),
                new LinkedHashMap<>(aliases)
        );
    }

    public boolean isWireAttachedToElement(WorkspaceWire w, String elementId) {
        return w.elementA().equals(elementId) || w.elementB().equals(elementId);
    }



    public enum WireRoutingMode {
        STRAIGHT("Odcinek prosty");

        private final String label;

        WireRoutingMode(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }
}
