package pl.polsl.bland.desktop.editor;

import java.util.ArrayList;
import java.util.List;
import pl.polsl.bland.models.Wire;

public class DrawableWire {
    private Wire wire;

    public DrawableWire(Wire wire) {
        this.wire = wire;
    }

    public Wire getWire() {
        return wire;
    }

    // Gettery dla punków przewodu
    public List<int[]> getPoints() {
        return wire.points();
    }

    public int[] getStartPoint() {
        return wire.points().get(0);
    }

    public int[] getEndPoint() {
        return wire.points().get(wire.points().size() - 1);
    }

    // Dodawanie punktu do przewodu
    public void addPoints(int x, int y){
        List<int[]> points = new ArrayList<>(wire.points());
        points.add(new int[]{x, y});

        this.wire = new Wire(
            wire.id(),
            wire.node(),
            points
        );
    }

    // Usuwanie ostatniego punktu z przewodu
    public void setEndPoint(int x, int y){
        List<int[]> points = new ArrayList<>(wire.points());
        if (!points.isEmpty()) {
            points.set(points.size() - 1, new int[]{x, y});
        }

        this.wire = new Wire(
            wire.id(),
            wire.node(),
            points
        );
    }

};
