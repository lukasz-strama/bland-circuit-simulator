package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.Wire;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkspaceSchematicMapper {
    private static final double GRID_STEP = 16.0;

    private final WorkspaceMockService workspaceMockService;

    public WorkspaceSchematicMapper(WorkspaceMockService workspaceMockService) {
        this.workspaceMockService = workspaceMockService;
    }

    public WorkspaceState importSchematic(CircuitSchematic schematic) {
        if (schematic == null) {
            throw new IllegalStateException("Backend nie zwrócił projektu do wczytania.");
        }

        LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements = importElements(schematic.elements());
        addInferredGrounds(schematic.wires(), elements);

        LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires = new LinkedHashMap<>();
        LinkedHashMap<String, String> wireNodes = new LinkedHashMap<>();
        int fallbackWireIndex = 1;
        for (Wire wire : nullToEmpty(schematic.wires())) {
            ImportedWire importedWire = importWire(wire, elements, wires, fallbackWireIndex);
            if (importedWire == null) {
                continue;
            }
            wires.put(importedWire.wire().id(), importedWire.wire());
            if (wire.node() != null && !wire.node().isBlank()) {
                wireNodes.put(importedWire.wire().id(), wire.node().trim());
            }
            fallbackWireIndex++;
        }

        LinkedHashMap<String, String> netAliases = restoreNetAliases(elements, wires, wireNodes);
        return new WorkspaceState(elements, wires, netAliases);
    }

    private LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> importElements(List<CircuitElement> storedElements) {
        LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements = new LinkedHashMap<>();
        for (CircuitElement element : nullToEmpty(storedElements)) {
            if (element.id() == null || element.id().isBlank()) {
                throw new IllegalStateException("Backend zwrócił element bez ID.");
            }
            if (elements.containsKey(element.id())) {
                throw new IllegalStateException("Backend zwrócił duplikat ID elementu: " + element.id() + ".");
            }

            WorkspaceMockService.ElementType type = mapElementType(element.type());
            WorkspaceMockService.Orientation orientation = orientationFromRotation(element.rotation());
            String sourceType = type.isSource()
                    ? WorkspaceMockService.normalizeSourceType(element.sourceType())
                    : null;
            Double frequency = WorkspaceMockService.SOURCE_TYPE_SINE.equals(sourceType)
                    ? element.frequency()
                    : null;

            elements.put(element.id(), new WorkspaceMockService.WorkspaceElement(
                    element.id(),
                    type,
                    gridToCanvas(element.x()),
                    gridToCanvas(element.y()),
                    orientation,
                    formatStoredValue(element.value()),
                    sourceType,
                    frequency));
        }
        return elements;
    }

    private void addInferredGrounds(
            List<Wire> storedWires,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements) {
        for (Wire wire : nullToEmpty(storedWires)) {
            if (!"0".equals(wire.node())) {
                continue;
            }
            for (CanvasPoint endpoint : endpointPoints(wire)) {
                if (workspaceMockService.resolvePinAt(elements, endpoint.x(), endpoint.y()).isPresent()) {
                    continue;
                }
                WorkspaceMockService.WorkspaceElement ground =
                        workspaceMockService.createGroundAtRefPoint(elements.values(), endpoint.x(), endpoint.y());
                elements.put(ground.id(), ground);
            }
        }
    }

    private ImportedWire importWire(
            Wire wire,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> existingWires,
            int fallbackWireIndex) {
        List<CanvasPoint> endpoints = endpointPoints(wire);
        if (endpoints.size() < 2) {
            return null;
        }

        WorkspaceMockService.WireEndpointRef start =
                workspaceMockService.resolveEndpointAt(elements, endpoints.get(0).x(), endpoints.get(0).y());
        WorkspaceMockService.WireEndpointRef end =
                workspaceMockService.resolveEndpointAt(elements, endpoints.get(1).x(), endpoints.get(1).y());
        if (start.equals(end)) {
            return null;
        }

        String wireId = uniqueWireId(wire.id(), existingWires, fallbackWireIndex);
        return new ImportedWire(new WorkspaceMockService.WorkspaceWire(wireId, start, end));
    }

    private LinkedHashMap<String, String> restoreNetAliases(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires,
            LinkedHashMap<String, String> wireNodes) {
        LinkedHashMap<String, String> netAliases = new LinkedHashMap<>();
        WorkspaceMockService.NetTopology topology =
                workspaceMockService.resolveNetTopology(elements, wires.values(), Map.of());

        for (Map.Entry<String, String> entry : wireNodes.entrySet()) {
            WorkspaceMockService.WorkspaceWire wire = wires.get(entry.getKey());
            if (wire == null) {
                continue;
            }
            String netKey = topology.netKey(wire.start());
            if (netKey != null) {
                netAliases.putIfAbsent(netKey, entry.getValue());
            }
        }
        return netAliases;
    }

    private List<CanvasPoint> endpointPoints(Wire wire) {
        if (wire == null || wire.points() == null || wire.points().size() < 2) {
            return List.of();
        }

        int[] first = wire.points().get(0);
        int[] last = wire.points().get(wire.points().size() - 1);
        if (!isValidPoint(first) || !isValidPoint(last)) {
            return List.of();
        }

        return List.of(
                new CanvasPoint(gridToCanvas(first[0]), gridToCanvas(first[1])),
                new CanvasPoint(gridToCanvas(last[0]), gridToCanvas(last[1])));
    }

    private WorkspaceMockService.ElementType mapElementType(CircuitElement.ElementType type) {
        if (type == null) {
            throw new IllegalStateException("Backend zwrócił element bez typu.");
        }
        return switch (type) {
            case R -> WorkspaceMockService.ElementType.RESISTOR;
            case L -> WorkspaceMockService.ElementType.INDUCTOR;
            case C -> WorkspaceMockService.ElementType.CAPACITOR;
            case V -> WorkspaceMockService.ElementType.VOLTAGE;
            case I -> WorkspaceMockService.ElementType.CURRENT;
        };
    }

    private WorkspaceMockService.Orientation orientationFromRotation(int rotation) {
        int normalized = Math.floorMod(rotation, 360);
        return switch (normalized) {
            case 90 -> WorkspaceMockService.Orientation.DEG_90;
            case 180 -> WorkspaceMockService.Orientation.DEG_180;
            case 270 -> WorkspaceMockService.Orientation.DEG_270;
            default -> WorkspaceMockService.Orientation.DEG_0;
        };
    }

    private String uniqueWireId(
            String storedId,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> existingWires,
            int fallbackWireIndex) {
        String candidate = storedId == null || storedId.isBlank() ? "W" + fallbackWireIndex : storedId.trim();
        int index = fallbackWireIndex;
        while (existingWires.containsKey(candidate)) {
            candidate = "W" + index++;
        }
        return candidate;
    }

    private String formatStoredValue(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private boolean isValidPoint(int[] point) {
        return point != null && point.length >= 2;
    }

    private double gridToCanvas(int coordinate) {
        return coordinate * GRID_STEP;
    }

    private <T> List<T> nullToEmpty(List<T> value) {
        return value == null ? List.of() : value;
    }

    public record WorkspaceState(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires,
            LinkedHashMap<String, String> netAliases) {
    }

    private record ImportedWire(WorkspaceMockService.WorkspaceWire wire) {
    }

    private record CanvasPoint(double x, double y) {
    }
}
