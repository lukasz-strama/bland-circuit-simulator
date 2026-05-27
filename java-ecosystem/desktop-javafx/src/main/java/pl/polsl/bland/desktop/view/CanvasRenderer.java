package pl.polsl.bland.desktop.view;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.shape.ArcType;
import pl.polsl.bland.desktop.service.WorkspaceService;

import java.util.Collection;
import java.util.Map;

/**
 * Odpowiada za renderowanie całego schematu na płótnie (Canvas).
 *
 * Renderuje:
 * <ul>
 *     <li>siatkę pomocniczą,</li>
 *     <li>elementy (R, C, L, źródła, masa),</li>
 *     <li>piny elementów,</li>
 *     <li>przewody (proste lub ortogonalne),</li>
 *     <li>tekst identyfikatorów elementów.</li>
 * </ul>
 *
 * Klasa nie przechowuje stanu schematu, otrzymuje go z WorkspaceService.
 * Jest czysto graficzna i nie wykonuje żadnej logiki edytora.
 */
public class CanvasRenderer {

    /** Dostęp do logiki elementów i pinów. */
    private final WorkspaceService workspace;
    /** Aktualny tryb rysowania przewodów (prosty / ortogonalny). */
    private MainView.WireRoutingMode routingMode = MainView.WireRoutingMode.STRAIGHT;

     /**
     * Tworzy renderer powiązany z WorkspaceService.
     *
     * @param workspace logika elementów i pinów
     */
    public CanvasRenderer(WorkspaceService workspace) {
        this.workspace = workspace;
    }

     /**
     * Ustawia tryb rysowania przewodów.
     *
     * @param mode tryb routingu (STRAIGHT lub ORTHOGONAL)
     */
    public void setRoutingMode(MainView.WireRoutingMode mode) {
    this.routingMode = mode;
    }

    /**
     * Renderuje cały schemat na podanym GraphicsContext.
     *
     * @param gc kontekst graficzny płótna
     * @param elements mapa elementów do narysowania
     * @param wires kolekcja przewodów
     * @param unusedNetTopology (nieużywane — pozostawione dla zgodności)
     * @param zoom aktualny poziom powiększenia
     * @param selectedElement ID zaznaczonego elementu (opcjonalnie)
     * @param selectedWire ID zaznaczonego przewodu (opcjonalnie)
     */
    public void render(
            GraphicsContext gc,
            Map<String, WorkspaceService.WorkspaceElement> elements,
            Collection<WorkspaceService.WorkspaceWire> wires,
            Object unusedNetTopology,
            double zoom,
            String selectedElement,
            String selectedWire
    ) {

        gc.clearRect(0, 0,
        gc.getCanvas().getWidth(),
        gc.getCanvas().getHeight());
        
        gc.save();
        gc.scale(zoom, zoom);

        drawGrid(gc);
        drawWires(gc, elements, wires);
        drawElements(gc, elements);

        gc.restore();
    }

    /**
     * Rysuje siatkę pomocniczą co 20 px.
     */
    private void drawGrid(GraphicsContext gc) {
        gc.setStroke(Color.rgb(230, 230, 230));
        gc.setLineWidth(1);

        for (int x = 0; x < 2000; x += 20)
            gc.strokeLine(x, 0, x, 2000);

        for (int y = 0; y < 2000; y += 20)
            gc.strokeLine(0, y, 2000, y);
    }

    /**
     * Renderuje wszystkie elementy schematu wraz z pinami i etykietami.
     */
    private void drawElements(GraphicsContext gc, Map<String, WorkspaceService.WorkspaceElement> elements) {
        gc.setStroke(Color.BLACK);
        gc.setFill(Color.BLACK);
        gc.setLineWidth(2);

        for (var el : elements.values()) {

            gc.save();
            gc.translate(el.x(), el.y());
            gc.rotate(el.rotation());
            gc.translate(-el.x(), -el.y());

            switch (el.type()) {
                case RESISTOR -> drawResistor(gc, el.x(), el.y());
                case CAPACITOR -> drawCapacitor(gc, el.x(), el.y());
                case INDUCTOR -> drawInductor(gc, el.x(), el.y());
                case VOLTAGE -> drawVoltage(gc, el.x(), el.y());
                case CURRENT -> drawCurrent(gc, el.x(), el.y());
                case GROUND -> drawGround(gc, el.x(), el.y());
            }

            gc.restore();

            gc.fillText(el.id(), el.x() + 5, el.y() - 10);

            gc.setFill(Color.RED);
            for (var p : el.pins()) {
                gc.fillOval(p.x() - 3, p.y() - 3, 6, 6);
            }
            gc.setFill(Color.BLACK);
        }
    }

    /** Rysuje symbol rezystora. */
    private void drawResistor(GraphicsContext gc, double x, double y) {
        gc.strokeLine(x, y, x + 20, y);
        gc.strokeLine(x + 50, y, x + 70, y);
        gc.strokeRect(x + 20, y - 10, 30, 20);
    }

     /** Rysuje symbol kondensatora. */
    private void drawCapacitor(GraphicsContext gc, double x, double y) {
        gc.strokeLine(x, y, x, y + 15);
        gc.strokeLine(x, y + 25, x, y + 40);
        gc.strokeLine(x - 12, y + 15, x + 12, y + 15);
        gc.strokeLine(x - 12, y + 25, x + 12, y + 25);
    }

    /** Rysuje symbol cewki. */
    private void drawInductor(GraphicsContext gc, double x, double y) {
        gc.strokeLine(x, y, x + 10, y);
        gc.strokeLine(x + 55, y, x + 70, y);
        gc.strokeArc(x + 10, y - 10, 15, 20, 0, 180, ArcType.OPEN);
        gc.strokeArc(x + 25, y - 10, 15, 20, 0, 180, ArcType.OPEN);
        gc.strokeArc(x + 40, y - 10, 15, 20, 0, 180, ArcType.OPEN);
}

  /** Rysuje symbol źródła napięcia. */
    private void drawVoltage(GraphicsContext gc, double x, double y) {
    gc.strokeLine(x, y, x, y + 50);

    gc.strokeOval(x - 10, y + 15, 20, 20);

    gc.strokeLine(x, y + 18, x, y + 32); 
    gc.strokeLine(x, y + 18, x - 3, y + 22);  
    gc.strokeLine(x, y + 18, x + 3, y + 22);   
}

/** Rysuje symbol źródła prądu. */
private void drawCurrent(GraphicsContext gc, double x, double y) {

    gc.strokeLine(x, y, x, y + 50);
    gc.strokeOval(x - 10, y + 15, 20, 20);
    gc.strokeOval(x - 7, y + 18, 14, 14);
    gc.strokeLine(x, y + 19, x, y + 31);
    gc.strokeLine(x, y + 19, x - 3, y + 23);
    gc.strokeLine(x, y + 19, x + 3, y + 23);

}

 /** Rysuje symbol masy (GND). */
    private void drawGround(GraphicsContext gc, double x, double y) {
        gc.strokeLine(x - 10, y, x + 10, y);
        gc.strokeLine(x - 6, y + 5, x + 6, y + 5);
        gc.strokeLine(x - 3, y + 10, x + 3, y + 10);
    }

    /**
     * Renderuje przewody między pinami elementów.
     * Obsługuje tryb prosty i ortogonalny.
     */
    private void drawWires(
            GraphicsContext gc,
            Map<String, WorkspaceService.WorkspaceElement> elements,
            Collection<WorkspaceService.WorkspaceWire> wires
    ) {
        gc.setStroke(Color.BLUE);
        gc.setLineWidth(2);

        for (var w : wires) {
             var elA = elements.get(w.elementA());
        var elB = elements.get(w.elementB());
        if (elA == null || elB == null) continue;

        var pA = elA.pins().stream().filter(p -> p.key().equals(w.pinA())).findFirst().orElse(null);
        var pB = elB.pins().stream().filter(p -> p.key().equals(w.pinB())).findFirst().orElse(null);

        if (pA == null || pB == null) continue;

        double x1 = pA.x();
        double y1 = pA.y();
        double x2 = pB.x();
        double y2 = pB.y();

        
        if (routingMode == MainView.WireRoutingMode.STRAIGHT) {
            gc.strokeLine(x1, y1, x2, y2);
            continue;
        }

        boolean horizontalFirst = Math.abs(x2 - x1) > Math.abs(y2 - y1);

        if (horizontalFirst) {
      
            gc.strokeLine(x1, y1, x2, y1);
            gc.strokeLine(x2, y1, x2, y2);
        } else {
            
            gc.strokeLine(x1, y1, x1, y2);
            gc.strokeLine(x1, y2, x2, y2);
        }
    }
}
}
        
    
