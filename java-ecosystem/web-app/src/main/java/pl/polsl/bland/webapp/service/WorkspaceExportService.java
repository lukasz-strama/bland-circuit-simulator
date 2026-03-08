package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.models.Wire;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceExportService {
    private static final double GRID_STEP = 16.0;
    private static final double DEFAULT_TSTOP = 0.008;
    private static final double DEFAULT_TSTEP = 0.0001;

    public CircuitSchematic exportSchematic(
            String name,
            Map<String, WorkspaceMockService.WorkspaceElement> elements,
            Collection<WorkspaceMockService.ResolvedWire> resolvedWires,
            WorkspaceMockService.NetTopology topology) {
        List<CircuitElement> exportedElements = new ArrayList<>();
        for (WorkspaceMockService.WorkspaceElement element : elements.values()) {
            CircuitElement exported = exportElement(element, topology);
            if (exported != null) {
                exportedElements.add(exported);
            }
        }

        List<Wire> exportedWires = resolvedWires.stream()
                .map(wire -> exportWire(wire, topology))
                .toList();

        return new CircuitSchematic(
                null,
                name == null || name.isBlank() ? "Schemat web-app" : name,
                null,
                exportedElements,
                exportedWires,
                Instant.now());
    }

    public String buildEngineNetlist(CircuitSchematic schematic, SimulationRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append("* Bland Circuit Simulator / web-app\n");
        builder.append("* name=").append(schematic.name()).append('\n');
        for (CircuitElement element : schematic.elements()) {
            builder.append(formatElement(element)).append('\n');
        }
        builder.append(formatDirective(request));
        return builder.toString();
    }

    public Map<String, Double> defaultParameters(SimulationRequest.AnalysisType analysisType) {
        if (analysisType == SimulationRequest.AnalysisType.TRANSIENT) {
            Map<String, Double> parameters = new LinkedHashMap<>();
            parameters.put("tstop", DEFAULT_TSTOP);
            parameters.put("tstep", DEFAULT_TSTEP);
            return parameters;
        }
        return Map.of();
    }

    private CircuitElement exportElement(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.NetTopology topology) {
        return switch (element.type()) {
            case RESISTOR -> createElement(element, topology, CircuitElement.ElementType.R, parseValue(element, element.value()));
            case INDUCTOR -> createElement(element, topology, CircuitElement.ElementType.L, parseValue(element, element.value()));
            case CAPACITOR -> createElement(element, topology, CircuitElement.ElementType.C, parseValue(element, element.value()));
            case VOLTAGE -> createSource(element, topology, CircuitElement.ElementType.V);
            case CURRENT -> createSource(element, topology, CircuitElement.ElementType.I);
            case GROUND -> null;
            case DIODE, OPAMP -> throw new IllegalArgumentException(
                    "Element " + element.id() + " (" + element.type().label() + ") nie jest jeszcze obsługiwany przez silnik symulacji.");
        };
    }

    private CircuitElement createElement(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.NetTopology topology,
            CircuitElement.ElementType exportedType,
            double value) {
        return new CircuitElement(
                element.id(),
                exportedType,
                resolveNodeName(topology, pinA(element)),
                resolveNodeName(topology, pinB(element)),
                value,
                null,
                null,
                toGrid(element.left()),
                toGrid(element.top()),
                0);
    }

    private CircuitElement createSource(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.NetTopology topology,
            CircuitElement.ElementType exportedType) {
        String sourceType = WorkspaceMockService.normalizeSourceType(element.sourceType());
        Double frequency = WorkspaceMockService.SOURCE_TYPE_SINE.equals(sourceType) ? element.frequency() : null;
        if (WorkspaceMockService.SOURCE_TYPE_SINE.equals(sourceType) && (frequency == null || frequency <= 0)) {
            throw new IllegalArgumentException("Źródło " + element.id() + " ma ustawiony przebieg sinus, ale nie ma poprawnej częstotliwości.");
        }

        return new CircuitElement(
                element.id(),
                exportedType,
                resolveNodeName(topology, pinA(element)),
                resolveNodeName(topology, pinB(element)),
                parseValue(element, element.value()),
                sourceType,
                frequency,
                toGrid(element.left()),
                toGrid(element.top()),
                0);
    }

    private Wire exportWire(WorkspaceMockService.ResolvedWire wire, WorkspaceMockService.NetTopology topology) {
        String nodeName = resolveNodeName(topology, wire.start());
        List<int[]> points = wire.nodes().stream()
                .map(point -> new int[]{toGrid(point.x()), toGrid(point.y())})
                .toList();
        return new Wire(wire.id(), nodeName, points);
    }

    private String formatElement(CircuitElement element) {
        return switch (element.type()) {
            case R -> "RES " + element.id() + " " + element.node1() + " " + element.node2() + " val=" + formatNumber(element.value());
            case L -> "IND " + element.id() + " " + element.node1() + " " + element.node2() + " val=" + formatNumber(element.value());
            case C -> "CAP " + element.id() + " " + element.node1() + " " + element.node2() + " val=" + formatNumber(element.value());
            case V -> formatSource(element, "VSRC");
            case I -> formatSource(element, "ISRC");
        };
    }

    private String formatSource(CircuitElement element, String keyword) {
        String sourceType = WorkspaceMockService.normalizeSourceType(element.sourceType());
        StringBuilder builder = new StringBuilder()
                .append(keyword)
                .append(' ')
                .append(element.id())
                .append(' ')
                .append(element.node1())
                .append(' ')
                .append(element.node2())
                .append(" type=")
                .append(sourceType)
                .append(" val=")
                .append(formatNumber(element.value()));
        if (WorkspaceMockService.SOURCE_TYPE_SINE.equals(sourceType) && element.frequency() != null) {
            builder.append(" freq=").append(formatNumber(element.frequency()));
        }
        return builder.toString();
    }

    private String formatDirective(SimulationRequest request) {
        if (request.analysisType() == null || request.analysisType() == SimulationRequest.AnalysisType.DC) {
            return ".SIMULATE type=dc";
        }

        Map<String, Double> parameters = request.parameters() == null ? Map.of() : request.parameters();
        double tstop = parameters.getOrDefault("tstop", DEFAULT_TSTOP);
        double tstep = parameters.getOrDefault("tstep", DEFAULT_TSTEP);
        return ".SIMULATE type=trans tstop=" + formatNumber(tstop) + " tstep=" + formatNumber(tstep);
    }

    private String resolveNodeName(WorkspaceMockService.NetTopology topology, WorkspaceMockService.PinRef pinRef) {
        String netKey = topology.netKey(pinRef);
        if (netKey != null) {
            var net = topology.findNet(netKey).orElse(null);
            if (net != null && net.members().stream().anyMatch(this::isGroundPin)) {
                return "0";
            }
        }
        return topology.netName(pinRef, pinRef.elementId() + "_" + pinRef.pinKey());
    }

    private boolean isGroundPin(WorkspaceMockService.PinRef pinRef) {
        return "REF".equals(pinRef.pinKey()) && pinRef.elementId().startsWith("GND");
    }

    private WorkspaceMockService.PinRef pinA(WorkspaceMockService.WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR -> new WorkspaceMockService.PinRef(element.id(), "A");
            case VOLTAGE, CURRENT -> new WorkspaceMockService.PinRef(element.id(), "POS");
            case GROUND -> new WorkspaceMockService.PinRef(element.id(), "REF");
            case DIODE -> new WorkspaceMockService.PinRef(element.id(), "ANODE");
            case OPAMP -> new WorkspaceMockService.PinRef(element.id(), "IN+");
        };
    }

    private WorkspaceMockService.PinRef pinB(WorkspaceMockService.WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR -> new WorkspaceMockService.PinRef(element.id(), "B");
            case VOLTAGE, CURRENT -> new WorkspaceMockService.PinRef(element.id(), "NEG");
            case GROUND -> new WorkspaceMockService.PinRef(element.id(), "REF");
            case DIODE -> new WorkspaceMockService.PinRef(element.id(), "CATHODE");
            case OPAMP -> new WorkspaceMockService.PinRef(element.id(), "IN-");
        };
    }

    private double parseValue(WorkspaceMockService.WorkspaceElement element, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException("Element " + element.id() + " nie ma ustawionej wartości.");
        }

        String normalized = rawValue.trim()
                .toLowerCase()
                .replace(",", ".")
                .replace(" ", "");

        String unitSuffix = switch (element.type()) {
            case RESISTOR -> "ohm";
            case INDUCTOR -> "h";
            case CAPACITOR -> "f";
            case VOLTAGE -> "v";
            case CURRENT -> "a";
            default -> "";
        };

        if ("ohm".equals(unitSuffix) && normalized.endsWith("ohm")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        } else if (!unitSuffix.isBlank() && normalized.endsWith(unitSuffix)) {
            normalized = normalized.substring(0, normalized.length() - unitSuffix.length());
        }

        String numberPart = normalized;
        String prefix = "";
        int splitIndex = findPrefixIndex(normalized);
        if (splitIndex >= 0) {
            numberPart = normalized.substring(0, splitIndex);
            prefix = normalized.substring(splitIndex);
        }

        double baseValue;
        try {
            baseValue = Double.parseDouble(numberPart);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Nie udało się odczytać wartości elementu " + element.id() + ": " + rawValue);
        }

        return baseValue * prefixMultiplier(prefix);
    }

    private int findPrefixIndex(String value) {
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if ((character < '0' || character > '9') && character != '.' && character != '-' && character != '+' && character != 'e') {
                return index;
            }
        }
        return -1;
    }

    private double prefixMultiplier(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return 1.0;
        }
        return switch (prefix) {
            case "f" -> 1e-15;
            case "p" -> 1e-12;
            case "n" -> 1e-9;
            case "u", "µ" -> 1e-6;
            case "m" -> 1e-3;
            case "k" -> 1e3;
            case "meg" -> 1e6;
            case "g" -> 1e9;
            default -> throw new IllegalArgumentException("Nieznany prefiks jednostki: " + prefix);
        };
    }

    private int toGrid(double coordinate) {
        return (int) Math.round(coordinate / GRID_STEP);
    }

    private String formatNumber(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0000001) {
            return Long.toString(Math.round(value));
        }
        return Double.toString(value);
    }
}
