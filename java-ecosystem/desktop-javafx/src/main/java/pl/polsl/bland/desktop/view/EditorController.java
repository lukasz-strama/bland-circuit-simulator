package pl.polsl.bland.desktop.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import pl.polsl.bland.desktop.service.WorkspaceService;

import java.util.Map;
import java.util.function.Consumer;

public class EditorController {

    private final WorkspaceService workspace;
    private final Map<String, WorkspaceService.WorkspaceElement> elements;
    private final Map<String, WorkspaceService.WorkspaceWire> wires;

    private final Canvas canvas;
    private final Runnable refresh;
    private final Consumer<WorkspaceService.WorkspaceElement> onSelect;
    private final MainView mainView;

    private WorkspaceTool tool = WorkspaceTool.SELECT;

    private String wireStartElement = null;
    private String wireStartPin = null;

    private String draggingElement = null;
    private double dragStartX;
    private double dragStartY;

    public EditorController(
            WorkspaceService workspace,
            Map<String, WorkspaceService.WorkspaceElement> elements,
            Map<String, WorkspaceService.WorkspaceWire> wires,
            Canvas canvas,
            Runnable refresh,
            Consumer<WorkspaceService.WorkspaceElement> onSelect,
            MainView mainView) {
        this.workspace = workspace;
        this.elements = elements;
        this.wires = wires;
        this.canvas = canvas;
        this.refresh = refresh;
        this.onSelect = onSelect;
        this.mainView = mainView;
        init();
    }

    public void setTool(WorkspaceTool tool) {
        this.tool = tool;
    }

    private void init() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,  this::onPress);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,  this::onDrag);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onRelease);
    }


    private void onPress(MouseEvent e) {
       
        if (e.getButton() != MouseButton.PRIMARY) return;

        switch (tool) {
            case SELECT -> handleSelectPress(e);
            case WIRE   -> handleWirePress(e);
            case DELETE -> handleDeletePress(e);
        }
    }

    private void onDrag(MouseEvent e) {
        
        
        if (!e.isPrimaryButtonDown()) return;

        mainView.dragHappened = true;

        if (draggingElement != null) {
            var el = elements.get(draggingElement);
            if (el == null) { draggingElement = null; return; }

            double dx = e.getX() - dragStartX;
            double dy = e.getY() - dragStartY;

            elements.put(draggingElement, workspace.moveElement(el, dx, dy));

            dragStartX = e.getX();
            dragStartY = e.getY();

            refresh.run();
        }
    }

    private void onRelease(MouseEvent e) {
    if (e.getButton() != MouseButton.PRIMARY) return;

    draggingElement = null;
    mainView.draggingElement = false;

    if (mainView.dragHappened) {
        mainView.dragHappened = false;
        return;  
    }
}


    private void handleSelectPress(MouseEvent e) {
        for (var el : elements.values()) {
            if (hit(el, e.getX(), e.getY())) {
                draggingElement = el.id();
                mainView.draggingElement = true;
                dragStartX = e.getX();
                dragStartY = e.getY();
                onSelect.accept(el);
                refresh.run();
                return;
            }
        }
        // Kliknięto puste miejsce
        onSelect.accept(null);
        mainView.draggingElement = false;
        refresh.run();
    }

    
    private void handleWirePress(MouseEvent e) {
        for (var el : elements.values()) {
            for (var p : el.pins()) {
                if (distance(p.x(), p.y(), e.getX(), e.getY()) < 10) {
                    if (wireStartElement == null) {
                        wireStartElement = el.id();
                        wireStartPin = p.key();
                        return;
                    }

                    String id = "W" + (wires.size() + 1);
                    wires.put(id, new WorkspaceService.WorkspaceWire(
                            id,
                            wireStartElement, wireStartPin,
                            el.id(), p.key()));

                    wireStartElement = null;
                    wireStartPin = null;
                    refresh.run();
                    return;
                }
            }
        }
    }

    private void handleDeletePress(MouseEvent e) {
      
        for (var w : wires.values()) {
            var elA = elements.get(w.elementA());
            var elB = elements.get(w.elementB());
            if (elA == null || elB == null) continue;

            var pA = elA.pins().stream().filter(p -> p.key().equals(w.pinA())).findFirst().orElse(null);
            var pB = elB.pins().stream().filter(p -> p.key().equals(w.pinB())).findFirst().orElse(null);

            if (pA != null && pB != null
                    && pointToSegment(e.getX(), e.getY(), pA.x(), pA.y(), pB.x(), pB.y()) < 5) {
                wires.remove(w.id());
                refresh.run();
                return;
            }
        }

        for (var el : elements.values()) {
            if (hit(el, e.getX(), e.getY())) {
                String id = el.id();
                elements.remove(id);
                wires.entrySet().removeIf(w ->
                        w.getValue().elementA().equals(id) || w.getValue().elementB().equals(id));
                refresh.run();
                return;
            }
        }
    }

    public boolean isClickOnElement(double x, double y) {
    for (var el : elements.values()) {
        if (hit(el, x, y)) {
            return true;
        }
    }
    return false;
}



    private boolean hit(WorkspaceService.WorkspaceElement el, double x, double y) {
        return Math.abs(x - el.x()) < 40 && Math.abs(y - el.y()) < 40;
    }

    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    private double pointToSegment(double px, double py,
                                   double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0 && dy == 0) return distance(px, py, x1, y1);
        double t = Math.max(0, Math.min(1,
                ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        return distance(px, py, x1 + t * dx, y1 + t * dy);
    }

    public WorkspaceService.WorkspaceElement findElementAt(double x, double y) {
    for (var el : elements.values()) {
        if (hit(el, x, y)) {
            return el;
        }
    }
    return null;
}


}