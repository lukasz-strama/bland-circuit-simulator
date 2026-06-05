package pl.polsl.bland.desktop.view;

import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import pl.polsl.bland.desktop.service.WorkspaceService;


import java.util.Map;
import java.util.function.Consumer;

/**
 * Kontroler logiki edytora schematów.
 *
 * Odpowiada za:
 * <ul>
 *     <li>obsługę myszy na płótnie (kliknięcia, przeciąganie, puszczenie),</li>
 *     <li>wybieranie elementów,</li>
 *     <li>przesuwanie elementów,</li>
 *     <li>rysowanie przewodów między pinami,</li>
 *     <li>usuwanie elementów i przewodów,</li>
 *     <li>informowanie MainView o zmianach (refresh, onSelect).</li>
 * </ul>
 *
 * Klasa odpowiada za interakcje użytkownika z Canvasem.
 */
public class EditorController {

    /** Logika elementów i pinów. */
    private final WorkspaceService workspace;
    /** Mapa elementów aktualnie znajdujących się na płótnie. */
    private final Map<String, WorkspaceService.WorkspaceElement> elements;
    /** Mapa przewodów między elementami. */
    private final Map<String, WorkspaceService.WorkspaceWire> wires;
    /** Płótno, na którym użytkownik wykonuje operacje. */
    private final Canvas canvas;
    /** Funkcja odświeżająca widok (wywoływana po każdej zmianie). */
    private final Runnable refresh;
    /** Callback wywoływany przy zaznaczeniu elementu. */
    private final Consumer<WorkspaceService.WorkspaceElement> onSelect;
    /** Referencja do MainView (potrzebna do flag przeciągania). */
    private final MainView mainView;
    /** Aktualnie wybrane narzędzie (zaznaczanie, przewód, usuwanie). */
    private WorkspaceTool tool = WorkspaceTool.SELECT;
    /** Dane startowe do rysowania przewodu. */
    private String wireStartElement = null;
    private String wireStartPin = null;

    /** Dane do przeciągania elementu. */
    private String draggingElement = null;
    private double dragStartX;
    private double dragStartY;


    /**
     * Tworzy kontroler edytora i podłącza obsługę zdarzeń myszy.
     *
     * @param workspace logika elementów i pinów
     * @param elements mapa elementów
     * @param wires mapa przewodów
     * @param canvas płótno edytora
     * @param refresh funkcja odświeżająca widok
     * @param onSelect callback przy zaznaczeniu elementu
     * @param mainView widok główny
     */
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

    /**
     * Ustawia aktywne narzędzie edytora.
     *
     * @param tool narzędzie (SELECT, WIRE, DELETE)
     */
    public void setTool(WorkspaceTool tool) {
        this.tool = tool;
    }

    /**
     * Podłącza obsługę zdarzeń myszy do płótna.
     */
    private void init() {
        canvas.addEventHandler(MouseEvent.MOUSE_PRESSED,  this::onPress);
        canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED,  this::onDrag);
        canvas.addEventHandler(MouseEvent.MOUSE_RELEASED, this::onRelease);
    }

    /**
     * Obsługuje kliknięcie myszy.
     * Deleguje do odpowiedniej metody zależnie od aktywnego narzędzia.
     */
    private void onPress(MouseEvent e) {
       
        if (e.getButton() != MouseButton.PRIMARY) return;

        switch (tool) {
            case SELECT -> handleSelectPress(e);
            case WIRE   -> handleWirePress(e);
            case DELETE -> handleDeletePress(e);
        }
    }

    /**
     * Obsługuje przeciąganie elementu.
     */
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

    /**
     * Obsługuje puszczenie przycisku myszy.
     */
    private void onRelease(MouseEvent e) {
    if (e.getButton() != MouseButton.PRIMARY) return;

    draggingElement = null;
    mainView.draggingElement = false;

    if (mainView.dragHappened) {
        mainView.dragHappened = false;
        return;  
    }
}

    /**
     * Obsługuje kliknięcie w trybie zaznaczania.
     * Wybiera element lub rozpoczyna przeciąganie.
     */
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

     /**
     * Obsługuje kliknięcie w trybie rysowania przewodów.
     * Kliknięcie pinu - start przewodu.
     * Kliknięcie drugiego pinu - zakończenie przewodu.
     */
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

    /**
     * Obsługuje kliknięcie w trybie usuwania.
     * Usuwa przewód lub element.
     */
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

    /**
     * Sprawdza, czy kliknięto w obszar elementu.
     */
    public boolean isClickOnElement(double x, double y) {
    for (var el : elements.values()) {
        if (hit(el, x, y)) {
            return true;
        }
    }
    return false;
}

 /**
     * Sprawdza, czy punkt znajduje się w prostokącie elementu.
     */
    private boolean hit(WorkspaceService.WorkspaceElement el, double x, double y) {
        return Math.abs(x - el.x()) < 40 && Math.abs(y - el.y()) < 40;
    }

    /** Odległość między dwoma punktami. */
    private double distance(double x1, double y1, double x2, double y2) {
        return Math.hypot(x1 - x2, y1 - y2);
    }

    /**
     * Odległość punktu od odcinka — używane do wykrywania kliknięcia przewodu.
     */
    private double pointToSegment(double px, double py,
                                   double x1, double y1, double x2, double y2) {
        double dx = x2 - x1, dy = y2 - y1;
        if (dx == 0 && dy == 0) return distance(px, py, x1, y1);
        double t = Math.max(0, Math.min(1,
                ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy)));
        return distance(px, py, x1 + t * dx, y1 + t * dy);
    }
  
     /**
     * Zwraca element znajdujący się pod podanymi współrzędnymi.
     *
     * @param x współrzędna X
     * @param y współrzędna Y
     * @return element lub null
     */
    public WorkspaceService.WorkspaceElement findElementAt(double x, double y) {
    for (var el : elements.values()) {
        if (hit(el, x, y)) {
            return el;
        }
    }
    return null;
}


}