package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

public final class SchematicPreview extends Div {
    private final Map<String, Div> selectableParts = new LinkedHashMap<>();

    public SchematicPreview(Consumer<String> selectionHandler) {
        addClassName("sheet-stage");
        add(createSheetNote(), createSheetCanvas(selectionHandler));
    }

    public void setSelectedElement(String elementId) {
        selectableParts.values().forEach(part -> part.removeClassName("is-selected"));
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

    private Component createSheetCanvas(Consumer<String> selectionHandler) {
        Div canvas = new Div();
        canvas.addClassNames("sheet", "sheet-canvas");
        canvas.add(
                area("sheet-grid", 36, 36, 1208, 770),
                createTitleBlock(),
                createGlobalWireLayer(),
                createSheetDescriptions(),
                createResistor(selectionHandler),
                createInductor(selectionHandler),
                createCapacitor(selectionHandler),
                createVoltageSource(selectionHandler),
                createGround(selectionHandler));
        return canvas;
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
                text("sheet-text", 14, 64, "Tryb: schemat ideowy"),
                text("sheet-text", 338, 64, "Skala: 1:1"));
        return block;
    }

    private Component createGlobalWireLayer() {
        Div layer = area("sheet-layer", 0, 0, 1280, 860);
        layer.add(
                line("wire-segment", 184, 240, 292, 240),
                line("wire-segment", 468, 240, 612, 240),
                line("wire-segment", 816, 240, 940, 240),
                line("wire-segment", 940, 384, 940, 520),
                line("wire-segment", 940, 520, 184, 520),
                line("wire-segment", 184, 434, 184, 520),
                dot(180, 236),
                dot(464, 236),
                dot(608, 236),
                dot(812, 236),
                dot(936, 236),
                dot(180, 516),
                dot(936, 516),
                text("sheet-node-label", 168, 204, "IN"),
                text("sheet-node-label", 448, 204, "N001"),
                text("sheet-node-label", 594, 204, "N002"),
                text("sheet-node-label", 919, 204, "N003"));
        return layer;
    }

    private Component createSheetDescriptions() {
        Div layer = area("sheet-layer", 0, 0, 1280, 860);
        layer.add(
                text("sheet-text is-note", 84, 68,
                        "Edytor obwodu: tor RLC w konfiguracji szeregowej z kondensatorem do masy."),
                text("sheet-text is-note", 84, 86,
                        "Zaznaczenie: kliknij element. Wyniki po symulacji zależne od wybranego śladu."));
        return layer;
    }

    private Component createResistor(Consumer<String> selectionHandler) {
        Div part = part("R1", 270, 192, 220, 110, selectionHandler);
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
                text("component-label", 92, 4, "R1"),
                text("component-value", 68, 82, "120 Ohm"));
        return part;
    }

    private Component createInductor(Consumer<String> selectionHandler) {
        Div part = part("L1", 590, 192, 248, 110, selectionHandler);
        part.add(
                halo(0, 0, 248, 94),
                line("component-line", 22, 48, 40, 48),
                loop(40, 29, 38),
                loop(72, 29, 38),
                loop(104, 29, 38),
                loop(136, 29, 38),
                line("component-line", 174, 48, 226, 48),
                text("component-label", 110, 4, "L1"),
                text("component-value", 92, 82, "22 mH"));
        return part;
    }

    private Component createCapacitor(Consumer<String> selectionHandler) {
        Div part = part("C1", 892, 194, 176, 234, selectionHandler);
        part.add(
                halo(0, 0, 96, 234),
                line("component-line", 48, 46, 48, 92),
                line("component-line", 28, 92, 68, 92),
                line("component-line", 28, 142, 68, 142),
                line("component-line", 48, 142, 48, 190),
                text("component-label", 110, 108, "C1"),
                text("component-value", 110, 126, "4,7 uF"));
        return part;
    }

    private Component createVoltageSource(Consumer<String> selectionHandler) {
        Div part = part("V1", 110, 194, 172, 334, selectionHandler);
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
                text("component-label", 42, 4, "V1"),
                text("component-value", 0, 280, "SIN(0 5 1k)"));
        return part;
    }

    private Component createGround(Consumer<String> selectionHandler) {
        Div part = part("GND", 138, 514, 136, 104, selectionHandler);
        part.add(
                halo(0, 0, 96, 80),
                line("component-line", 46, 6, 46, 28),
                line("component-line", 18, 28, 74, 28),
                line("component-line", 26, 38, 66, 38),
                line("component-line", 34, 48, 58, 48),
                text("component-label", 10, 74, "GND"),
                text("component-value", 8, 90, "węzeł odniesienia"));
        return part;
    }

    private Div part(String elementId, double left, double top, double width, double height,
                     Consumer<String> selectionHandler) {
        Div part = area("schematic-part", left, top, width, height);
        part.getElement().setAttribute("data-element", elementId);
        part.addClickListener(event -> selectionHandler.accept(elementId));
        selectableParts.put(elementId, part);
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

    private static Div area(String className, double left, double top, double width, double height) {
        Div area = new Div();
        area.addClassName(className);
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
        line.addClassName(className);
        line.getStyle()
                .set("left", px(x1))
                .set("top", px(y1))
                .set("width", px(length))
                .set("transform", "rotate(" + angle + "deg)");
        return line;
    }

    private static Span text(String className, double left, double top, String value) {
        Span text = new Span(value);
        text.addClassName(className);
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
}
