package pl.polsl.bland.desktop.service;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkspaceService {

    public enum ElementType {
        RESISTOR, CAPACITOR, INDUCTOR, VOLTAGE, CURRENT, GROUND
    }

    public record Pin(String key, double x, double y) {}

    public record WorkspaceElement(
            String id,
            ElementType type,
            double x,
            double y,
            String value,
            double rotation
    ) {
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

    public record WorkspaceWire(
            String id,
            String elementA,
            String pinA,
            String elementB,
            String pinB
    ) {}

    private final AtomicInteger idCounter = new AtomicInteger(1);

    public Map<String, WorkspaceElement> createInitialWorkspace() {
        Map<String, WorkspaceElement> map = new LinkedHashMap<>();
        map.put("R1", new WorkspaceElement("R1", ElementType.RESISTOR, 200, 200, "1000", 0));
        map.put("C1", new WorkspaceElement("C1", ElementType.CAPACITOR, 400, 200, "1e-6", 0));
        return map;
    }

    public Map<String, WorkspaceWire> createInitialWires() {
        return new LinkedHashMap<>();
    }

    public WorkspaceElement createElement(ElementType type, double x, double y) {
        String id = type.name().charAt(0) + String.valueOf(idCounter.getAndIncrement());
        return new WorkspaceElement(id, type, x, y, defaultValue(type), 0);
    }

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
}
