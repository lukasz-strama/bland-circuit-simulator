package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.DomEvent;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchematicPreview extends Div {
    private static final String CLICKED_ELEMENT_DATA =
            "event.target.closest('[data-element-id]') ? event.target.closest('[data-element-id]').dataset.elementId : ''";
    private static final String CANVAS_X_DATA = "event.offsetX";
    private static final String CANVAS_Y_DATA = "event.offsetY";

    private final Div dynamicLayer = area("sheet-layer is-dynamic", 0, 0, 1280, 860);
    private final Map<String, Div> selectableParts = new LinkedHashMap<>();

    public SchematicPreview(InteractionHandler interactionHandler) {
        addClassName("sheet-stage");
        add(createSheetNote(), createSheetCanvas(interactionHandler));
    }

    public void renderWorkspace(Collection<WorkspaceMockService.WorkspaceElement> elements) {
        selectableParts.clear();
        dynamicLayer.removeAll();
        elements.forEach(element -> dynamicLayer.add(createElementPart(element)));
    }

    public void setSelectedElement(String elementId) {
        selectableParts.values().forEach(part -> part.removeClassName("is-selected"));
        if (elementId == null) {
            return;
        }
        Div selectedPart = selectableParts.get(elementId);
        if (selectedPart != null) {
            selectedPart.addClassName("is-selected");
        }
    }

    private Component createSheetNote() {
        Div note = new Div();
        note.addClassName("sheet-note");
        note.setText("Tryb arkusza: schemat ideowy / jednostki: SI");
        return note;
    }

    private Component createSheetCanvas(InteractionHandler interactionHandler) {
        Div canvas = new Div();
        canvas.addClassNames("sheet", "sheet-canvas");
        canvas.add(
                createStaticLayer(),
                dynamicLayer);
        canvas.getElement()
                .addEventListener("click", event -> handleCanvasClick(event, interactionHandler))
                .addEventData(CLICKED_ELEMENT_DATA)
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        return canvas;
    }

    private void handleCanvasClick(DomEvent event, InteractionHandler interactionHandler) {
        String elementId = event.getEventData().getString(CLICKED_ELEMENT_DATA);
        if (elementId != null && !elementId.isBlank()) {
            interactionHandler.onElementClick(elementId);
            return;
        }
        double canvasX = event.getEventData().getNumber(CANVAS_X_DATA);
        double canvasY = event.getEventData().getNumber(CANVAS_Y_DATA);
        interactionHandler.onCanvasClick(canvasX, canvasY);
    }

    private Component createStaticLayer() {
        Div layer = area("sheet-layer sheet-static", 0, 0, 1280, 860);
        layer.add(
                area("sheet-grid", 36, 36, 1208, 770),
                createTitleBlock(),
                createSheetDescriptions());
        return layer;
    }

    private Component createTitleBlock() {
        Div block = area("sheet-title-block", 852, 720, 392, 86);
        block.add(
                hLine("title-divider", 0, 28, 392),
                hLine("title-divider", 0, 56, 392),
                vLine("title-divider is-vertical", 266, 28, 58),
                vLine("title-divider is-vertical", 326, 56, 30),
                text("sheet-text", 14, 8, "Projekt: Filtr RLC - ćwiczenie 04"),
                text("sheet-text", 14, 36, "Autor: Laboratorium EE / grupa A2"),
                text("sheet-text", 280, 36, "Arkusz: 1 / 1"),
                text("sheet-text", 14, 64, "Tryb: makieta edytowalna"),
                text("sheet-text", 338, 64, "Skala: 1:1"));
        return block;
    }

    private Component createSheetDescriptions() {
        Div layer = area("sheet-layer", 0, 0, 1280, 860);
        layer.add(
                text("sheet-text is-note", 84, 68,
                        "Kliknij szybki komponent, aby przejść do trybu wstawiania i dodawać elementy na arkuszu."),
                text("sheet-text is-note", 84, 86,
                        "Zaznaczanie, usuwanie i przesuwanie działa już lokalnie bez backendu."));
        return layer;
    }

    private Component createElementPart(WorkspaceMockService.WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR -> createResistor(element);
            case INDUCTOR -> createInductor(element);
            case CAPACITOR -> createCapacitor(element);
            case VOLTAGE -> createVoltageSource(element);
            case GROUND -> createGround(element);
            case DIODE -> createDiode(element);
            case OPAMP -> createOpAmp(element);
        };
    }

    private Component createResistor(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 220, 94),
                line("component-line", 22, 48, 38, 48),
                line("component-line", 38, 48, 58, 28),
                line("component-line", 58, 28, 78, 68),
                line("component-line", 78, 68, 98, 28),
                line("component-line", 98, 28, 118, 68),
                line("component-line", 118, 68, 138, 28),
                line("component-line", 138, 28, 158, 68),
                line("component-line", 158, 68, 178, 28),
                line("component-line", 178, 28, 198, 48),
                text("component-label", 92, 4, element.id()),
                text("component-value", 68, 82, "120 Ohm"));
        return part;
    }

    private Component createInductor(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 248, 94),
                line("component-line", 22, 48, 40, 48),
                loop(40, 29, 38),
                loop(72, 29, 38),
                loop(104, 29, 38),
                loop(136, 29, 38),
                line("component-line", 174, 48, 226, 48),
                text("component-label", 110, 4, element.id()),
                text("component-value", 92, 82, "22 mH"));
        return part;
    }

    private Component createCapacitor(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 96, 234),
                line("component-line", 48, 46, 48, 92),
                line("component-line", 28, 92, 68, 92),
                line("component-line", 28, 142, 68, 142),
                line("component-line", 48, 142, 48, 190),
                text("component-label", 110, 108, element.id()),
                text("component-value", 110, 126, "4,7 uF"));
        return part;
    }

    private Component createVoltageSource(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(22, 0, 104, 272),
                line("component-line", 74, 46, 74, 92),
                circle("component-circle", 34, 92, 80),
                line("component-line", 74, 116, 74, 132),
                line("component-line", 66, 124, 82, 124),
                line("component-line", 66, 206, 82, 206),
                line("component-line", 44, 166, 56, 154),
                line("component-line", 56, 154, 74, 166),
                line("component-line", 74, 166, 92, 178),
                line("component-line", 92, 178, 104, 166),
                line("component-line", 74, 172, 74, 240),
                line("component-line", 74, 240, 74, 326),
                text("component-label", 42, 4, element.id()),
                text("component-value", 0, 280, "SIN(0 5 1k)"));
        return part;
    }

    private Component createGround(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 96, 80),
                line("component-line", 46, 6, 46, 28),
                line("component-line", 18, 28, 74, 28),
                line("component-line", 26, 38, 66, 38),
                line("component-line", 34, 48, 58, 48),
                text("component-label", 10, 74, element.id()),
                text("component-value", 8, 90, "węzeł odniesienia"));
        return part;
    }

    private Component createDiode(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 180, 94),
                line("component-line", 18, 48, 52, 48),
                line("component-line", 128, 48, 162, 48),
                triangle("component-diode-body", 52, 26, 72, 44),
                line("component-diode-bar", 128, 20, 128, 76),
                text("component-label", 70, 4, element.id()),
                text("component-value", 56, 82, "1N4148"));
        return part;
    }

    private Component createOpAmp(WorkspaceMockService.WorkspaceElement element) {
        Div part = part(element);
        part.add(
                halo(0, 0, 220, 148),
                triangle("component-opamp-body", 48, 18, 112, 112),
                line("component-line", 0, 54, 48, 54),
                line("component-line", 0, 94, 48, 94),
                line("component-line", 160, 74, 220, 74),
                text("component-marker", 18, 44, "+"),
                text("component-marker", 18, 84, "-"),
                text("component-label", 70, 0, element.id()),
                text("component-value", 64, 136, "uA741"));
        return part;
    }

    private Div part(WorkspaceMockService.WorkspaceElement element) {
        Div part = area("schematic-part", element.left(), element.top(), element.type().width(), element.type().height());
        part.getElement().setAttribute("data-element-id", element.id());
        selectableParts.put(element.id(), part);
        return part;
    }

    private static Div halo(double left, double top, double width, double height) {
        return area("selection-halo", left, top, width, height);
    }

    private static Div dot(double left, double top) {
        return area("wire-node", left, top, 8, 8);
    }

    private static Div loop(double left, double top, double size) {
        return area("component-loop", left, top, size, size);
    }

    private static Div circle(String className, double left, double top, double size) {
        return area(className, left, top, size, size);
    }

    private static Div triangle(String className, double left, double top, double width, double height) {
        return area(className, left, top, width, height);
    }

    private static Div area(String className, double left, double top, double width, double height) {
        Div area = new Div();
        addClasses(area, className);
        setBox(area, left, top, width, height);
        return area;
    }

    private static Div hLine(String className, double left, double top, double length) {
        return line(className, left, top, left + length, top);
    }

    private static Div vLine(String className, double left, double top, double length) {
        return line(className, left, top, left, top + length);
    }

    private static Div line(String className, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        double length = Math.hypot(dx, dy);
        double angle = Math.toDegrees(Math.atan2(dy, dx));

        Div line = new Div();
        addClasses(line, className);
        line.getStyle()
                .set("left", px(x1))
                .set("top", px(y1))
                .set("width", px(length))
                .set("transform", "rotate(" + angle + "deg)");
        return line;
    }

    private static Span text(String className, double left, double top, String value) {
        Span text = new Span(value);
        addClasses(text, className);
        text.getStyle()
                .set("left", px(left))
                .set("top", px(top));
        return text;
    }

    private static void setBox(Div component, double left, double top, double width, double height) {
        component.getStyle()
                .set("left", px(left))
                .set("top", px(top))
                .set("width", px(width))
                .set("height", px(height));
    }

    private static String px(double value) {
        return (Math.rint(value) == value ? Integer.toString((int) value) : Double.toString(value)) + "px";
    }

    private static void addClasses(Component component, String className) {
        if (className == null || className.isBlank()) {
            return;
        }
        component.addClassNames(className.trim().split("\\s+"));
    }

    public interface InteractionHandler {
        void onCanvasClick(double canvasX, double canvasY);

        void onElementClick(String elementId);
    }
}
