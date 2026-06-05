package pl.polsl.bland.webapp.service;

import org.springframework.stereotype.Service;
import pl.polsl.bland.models.CircuitElement;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.Wire;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkspaceSchematicMapper {
    private static final double GRID_STEP = 16.0;
    private static final double STORED_POINT_PIN_TOLERANCE = GRID_STEP;
    private static final double GROUND_INFERENCE_PIN_TOLERANCE = GRID_STEP / 2.0;

    private final WorkspaceMockService workspaceMockService;

    public WorkspaceSchematicMapper(WorkspaceMockService workspaceMockService) {
        this.workspaceMockService = workspaceMockService;
    }

    public WorkspaceState importSchematic(CircuitSchematic schematic) {
        if (schematic == null) {
            throw new IllegalStateException("Backend nie zwrócił projektu do wczytania.");
        }

        LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements = importElements(schematic.elements());
        LinkedHashMap<WorkspaceMockService.PinRef, String> pinNodes = importPinNodes(schematic.elements());
        addInferredGrounds(schematic.wires(), elements, pinNodes);

        LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires = new LinkedHashMap<>();
        LinkedHashMap<String, String> wireNodes = new LinkedHashMap<>();
        int fallbackWireIndex = 1;
        for (Wire wire : nullToEmpty(schematic.wires())) {
            for (ImportedWire importedWire : importWires(wire, elements, wires, fallbackWireIndex, pinNodes)) {
                wires.put(importedWire.wire().id(), importedWire.wire());
                if (wire.node() != null && !wire.node().isBlank()) {
                    wireNodes.put(importedWire.wire().id(), wire.node().trim());
                }
                fallbackWireIndex++;
            }
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

    private LinkedHashMap<WorkspaceMockService.PinRef, String> importPinNodes(List<CircuitElement> storedElements) {
        LinkedHashMap<WorkspaceMockService.PinRef, String> pinNodes = new LinkedHashMap<>();
        for (CircuitElement element : nullToEmpty(storedElements)) {
            if (element.id() == null || element.id().isBlank() || element.type() == null) {
                continue;
            }
            switch (element.type()) {
                case R, L, C -> {
                    putPinNode(pinNodes, element.id(), "A", element.node1());
                    putPinNode(pinNodes, element.id(), "B", element.node2());
                }
                case V, I -> {
                    putPinNode(pinNodes, element.id(), "POS", element.node1());
                    putPinNode(pinNodes, element.id(), "NEG", element.node2());
                }
            }
        }
        return pinNodes;
    }

    private void putPinNode(
            LinkedHashMap<WorkspaceMockService.PinRef, String> pinNodes,
            String elementId,
            String pinKey,
            String nodeName) {
        if (nodeName != null && !nodeName.isBlank()) {
            pinNodes.put(new WorkspaceMockService.PinRef(elementId, pinKey), nodeName.trim());
        }
    }

    private void addInferredGrounds(
            List<Wire> storedWires,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<WorkspaceMockService.PinRef, String> pinNodes) {
        for (Wire wire : nullToEmpty(storedWires)) {
            if (!"0".equals(wire.node())) {
                continue;
            }
            for (CanvasPoint endpoint : endpointPoints(wire)) {
                if (resolveStoredPinAt(
                        elements,
                        endpoint,
                        wire.node(),
                        pinNodes,
                        GROUND_INFERENCE_PIN_TOLERANCE).isPresent()) {
                    continue;
                }
                WorkspaceMockService.WorkspaceElement ground =
                        workspaceMockService.createGroundAtRefPoint(elements.values(), endpoint.x(), endpoint.y());
                elements.put(ground.id(), ground);
                pinNodes.put(new WorkspaceMockService.PinRef(ground.id(), "REF"), "0");
            }
        }
    }

    private List<ImportedWire> importWires(
            Wire wire,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> existingWires,
            int fallbackWireIndex,
            Map<WorkspaceMockService.PinRef, String> pinNodes) {
        List<CanvasPoint> points = pathPoints(wire);
        if (points.size() < 2) {
            return List.of();
        }

        LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> importedAndExistingWires =
                new LinkedHashMap<>(existingWires);
        List<ImportedWire> importedWires = new ArrayList<>();
        int nextFallbackIndex = fallbackWireIndex;
        for (int index = 0; index < points.size() - 1; index++) {
            CanvasPoint from = points.get(index);
            CanvasPoint to = points.get(index + 1);
            WorkspaceMockService.WireEndpointRef start =
                    resolveStoredEndpointAt(elements, from, wire.node(), pinNodes);
            WorkspaceMockService.WireEndpointRef end =
                    resolveStoredEndpointAt(elements, to, wire.node(), pinNodes);
            if (start.equals(end)) {
                List<WorkspaceMockService.PinRef> collapsedPins =
                        resolveStoredPinsAt(elements, from, wire.node(), pinNodes, STORED_POINT_PIN_TOLERANCE);
                for (int collapsedIndex = 1; collapsedIndex < collapsedPins.size(); collapsedIndex++) {
                    WorkspaceMockService.PinRef collapsedStart = collapsedPins.get(0);
                    WorkspaceMockService.PinRef collapsedEnd = collapsedPins.get(collapsedIndex);
                    if (workspaceMockService.createWire(
                            elements,
                            importedAndExistingWires.values(),
                            collapsedStart,
                            collapsedEnd).isEmpty()) {
                        continue;
                    }
                    String storedId = importedWires.isEmpty() ? wire.id() : null;
                    String wireId = uniqueWireId(storedId, importedAndExistingWires, nextFallbackIndex);
                    WorkspaceMockService.WorkspaceWire importedWire =
                            new WorkspaceMockService.WorkspaceWire(wireId, collapsedStart, collapsedEnd);
                    importedAndExistingWires.put(importedWire.id(), importedWire);
                    importedWires.add(new ImportedWire(importedWire));
                    nextFallbackIndex++;
                }
                continue;
            }
            if (workspaceMockService.createWire(elements, importedAndExistingWires.values(), start, end).isEmpty()) {
                continue;
            }

            String storedId = importedWires.isEmpty() ? wire.id() : null;
            String wireId = uniqueWireId(storedId, importedAndExistingWires, nextFallbackIndex);
            WorkspaceMockService.WorkspaceWire importedWire =
                    new WorkspaceMockService.WorkspaceWire(wireId, start, end);
            importedAndExistingWires.put(importedWire.id(), importedWire);
            importedWires.add(new ImportedWire(importedWire));
            nextFallbackIndex++;
        }
        return importedWires;
    }

    private WorkspaceMockService.WireEndpointRef resolveStoredEndpointAt(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            CanvasPoint point,
            String nodeName,
            Map<WorkspaceMockService.PinRef, String> pinNodes) {
        return resolveStoredPinAt(elements, point, nodeName, pinNodes)
                .<WorkspaceMockService.WireEndpointRef>map(pinRef -> pinRef)
                .orElseGet(() -> workspaceMockService.createFreePoint(point.x(), point.y()));
    }

    private Optional<WorkspaceMockService.PinRef> resolveStoredPinAt(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            CanvasPoint point,
            String nodeName,
            Map<WorkspaceMockService.PinRef, String> pinNodes) {
        return resolveStoredPinAt(elements, point, nodeName, pinNodes, STORED_POINT_PIN_TOLERANCE);
    }

    private Optional<WorkspaceMockService.PinRef> resolveStoredPinAt(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            CanvasPoint point,
            String nodeName,
            Map<WorkspaceMockService.PinRef, String> pinNodes,
            double tolerance) {
        return resolveStoredPinsAt(elements, point, nodeName, pinNodes, tolerance).stream().findFirst();
    }

    private List<WorkspaceMockService.PinRef> resolveStoredPinsAt(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            CanvasPoint point,
            String nodeName,
            Map<WorkspaceMockService.PinRef, String> pinNodes,
            double tolerance) {
        String normalizedNodeName = nodeName == null ? "" : nodeName.trim();
        List<WorkspaceMockService.PinRef> nearbyPins =
                workspaceMockService.resolvePinsNear(elements, point.x(), point.y(), tolerance);
        if (!normalizedNodeName.isBlank()) {
            return nearbyPins.stream()
                    .filter(pinRef -> normalizedNodeName.equals(pinNodes.get(pinRef)))
                    .toList();
        }
        return nearbyPins;
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
        List<CanvasPoint> points = pathPoints(wire);
        if (points.size() < 2) {
            return List.of();
        }

        return List.of(points.get(0), points.get(points.size() - 1));
    }

    private List<CanvasPoint> pathPoints(Wire wire) {
        if (wire == null || wire.points() == null || wire.points().size() < 2) {
            return List.of();
        }

        List<CanvasPoint> points = new ArrayList<>();
        for (int[] rawPoint : wire.points()) {
            if (!isValidPoint(rawPoint)) {
                return List.of();
            }
            points.add(new CanvasPoint(gridToCanvas(rawPoint[0]), gridToCanvas(rawPoint[1])));
        }
        return points;
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
