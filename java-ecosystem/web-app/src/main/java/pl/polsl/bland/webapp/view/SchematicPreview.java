package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Svg;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.DomEvent;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public final class SchematicPreview extends Div {
    private static final String POINTER_ELEMENT_DATA =
            "event.target.closest('[data-element-id]') ? event.target.closest('[data-element-id]').dataset.elementId : ''";
    private static final String CLICKED_PIN_KEY_DATA =
            "event.target.closest('[data-pin-key]') ? event.target.closest('[data-pin-key]').dataset.pinKey : ''";
    private static final String CLICKED_NET_KEY_DATA =
            "event.target.closest('[data-net-key]') ? event.target.closest('[data-net-key]').dataset.netKey : ''";
    private static final String CLICKED_WIRE_DATA =
            "event.target.closest('[data-wire-id]') ? event.target.closest('[data-wire-id]').dataset.wireId : ''";
    private static final String CLICKED_ELEMENT_DATA =
            "event.target.closest('[data-element-id]') ? event.target.closest('[data-element-id]').dataset.elementId : ''";
    private static final String CANVAS_X_DATA = "event.clientX - event.currentTarget.getBoundingClientRect().left";
    private static final String CANVAS_Y_DATA = "event.clientY - event.currentTarget.getBoundingClientRect().top";

    private final Div dynamicLayer = area("sheet-layer is-dynamic", 0, 0, 1280, 860);
    private final Map<String, Div> selectableParts = new LinkedHashMap<>();
    private final Map<String, Div> selectableWires = new LinkedHashMap<>();
    private final Map<String, Div> selectablePins = new LinkedHashMap<>();
    private final Map<String, Span> selectableNets = new LinkedHashMap<>();
    private String draggedElementId;

    public SchematicPreview(InteractionHandler interactionHandler) {
        addClassName("sheet-stage");
        add(createSheetNote(), createSheetCanvas(interactionHandler));
    }

    public void renderWorkspace(
            Collection<WorkspaceMockService.WorkspaceElement> elements,
            Collection<WorkspaceMockService.ResolvedWire> wires,
            Collection<WorkspaceMockService.ResolvedNet> nets) {
        selectableParts.clear();
        selectableWires.clear();
        selectablePins.clear();
        selectableNets.clear();
        dynamicLayer.removeAll();
        wires.forEach(wire -> dynamicLayer.add(createWireAssembly(wire)));
        nets.forEach(net -> dynamicLayer.add(netLabel(net)));
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

    public void setPendingWireStart(WorkspaceMockService.PinRef pinRef) {
        selectablePins.values().forEach(pin -> pin.removeClassName("is-pending"));
        if (pinRef == null) {
            return;
        }
        Div pin = selectablePins.get(pinToken(pinRef.elementId(), pinRef.pinKey()));
        if (pin != null) {
            pin.addClassName("is-pending");
        }
    }

    public void setSelectedWire(String wireId) {
        selectableWires.values().forEach(wire -> wire.removeClassName("is-selected"));
        if (wireId == null) {
            return;
        }
        Div wire = selectableWires.get(wireId);
        if (wire != null) {
            wire.addClassName("is-selected");
        }
    }

    public void setFocusedPin(WorkspaceMockService.PinRef pinRef) {
        selectablePins.values().forEach(pin -> pin.removeClassName("is-focused-endpoint"));
        if (pinRef == null) {
            return;
        }
        Div pin = selectablePins.get(pinToken(pinRef.elementId(), pinRef.pinKey()));
        if (pin != null) {
            pin.addClassName("is-focused-endpoint");
        }
    }

    public void setSelectedNet(String netKey) {
        selectableNets.values().forEach(net -> net.removeClassName("is-selected"));
        if (netKey == null) {
            return;
        }
        Span net = selectableNets.get(netKey);
        if (net != null) {
            net.addClassName("is-selected");
        }
    }

    public void setDraggingElement(String elementId) {
        selectableParts.values().forEach(part -> part.removeClassName("is-dragging"));
        if (elementId == null) {
            return;
        }
        Div draggedPart = selectableParts.get(elementId);
        if (draggedPart != null) {
            draggedPart.addClassName("is-dragging");
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
                .addEventData(CLICKED_PIN_KEY_DATA)
                .addEventData(CLICKED_NET_KEY_DATA)
                .addEventData(CLICKED_WIRE_DATA)
                .addEventData(CLICKED_ELEMENT_DATA)
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        canvas.getElement()
                .addEventListener("pointerdown", event -> handlePointerDown(event, interactionHandler))
                .addEventData(POINTER_ELEMENT_DATA)
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        canvas.getElement()
                .addEventListener("pointermove", event -> handlePointerMove(event, interactionHandler))
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        canvas.getElement()
                .addEventListener("pointerup", event -> handlePointerRelease(event, interactionHandler))
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        canvas.getElement()
                .addEventListener("pointerleave", event -> handlePointerRelease(event, interactionHandler))
                .addEventData(CANVAS_X_DATA)
                .addEventData(CANVAS_Y_DATA);
        return canvas;
    }

    private void handlePointerDown(DomEvent event, InteractionHandler interactionHandler) {
        String elementId = event.getEventData().getString(POINTER_ELEMENT_DATA);
        if (elementId == null || elementId.isBlank()) {
            return;
        }
        draggedElementId = elementId;
        interactionHandler.onElementDragStart(
                elementId,
                event.getEventData().getNumber(CANVAS_X_DATA),
                event.getEventData().getNumber(CANVAS_Y_DATA));
    }

    private void handlePointerMove(DomEvent event, InteractionHandler interactionHandler) {
        if (draggedElementId == null) {
            return;
        }
        interactionHandler.onElementDrag(
                draggedElementId,
                event.getEventData().getNumber(CANVAS_X_DATA),
                event.getEventData().getNumber(CANVAS_Y_DATA));
    }

    private void handlePointerRelease(DomEvent event, InteractionHandler interactionHandler) {
        if (draggedElementId == null) {
            return;
        }
        interactionHandler.onElementDragEnd(
                draggedElementId,
                event.getEventData().getNumber(CANVAS_X_DATA),
                event.getEventData().getNumber(CANVAS_Y_DATA));
        draggedElementId = null;
    }

    private void handleCanvasClick(DomEvent event, InteractionHandler interactionHandler) {
        String pinKey = event.getEventData().getString(CLICKED_PIN_KEY_DATA);
        String pinElementId = event.getEventData().getString(CLICKED_ELEMENT_DATA);
        if (pinKey != null && !pinKey.isBlank() && pinElementId != null && !pinElementId.isBlank()) {
            interactionHandler.onPinClick(pinElementId, pinKey);
            return;
        }

        String netKey = event.getEventData().getString(CLICKED_NET_KEY_DATA);
        if (netKey != null && !netKey.isBlank()) {
            interactionHandler.onNetClick(netKey);
            return;
        }

        String wireId = event.getEventData().getString(CLICKED_WIRE_DATA);
        if (wireId != null && !wireId.isBlank()) {
            interactionHandler.onWireClick(wireId);
            return;
        }

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
                        "Tryb przewodów łączy teraz piny elementów i aktualizuje połączenia po przesunięciu komponentu."));
        return layer;
    }

    private Component createElementPart(WorkspaceMockService.WorkspaceElement element) {
        return switch (element.type()) {
            case RESISTOR -> createResistor(element);
            case INDUCTOR -> createInductor(element);
            case CAPACITOR -> createCapacitor(element);
            case VOLTAGE -> createVoltageSource(element);
            case CURRENT -> createCurrentSource(element);
            case GROUND -> createGround(element);
            case DIODE -> createDiode(element);
            case OPAMP -> createOpAmp(element);
        };
    }

    private Component createResistor(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 220, 94),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 94" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="22" y1="48" x2="38" y2="48"/>
                          <polyline points="38,48 58,28 78,68 98,28 118,68 138,28 158,68 178,28 198,48"/>
                        </svg>
                        """, 0, 0, 220, 94),
                pin(element.id(), "A", 22, 48),
                pin(element.id(), "B", 198, 48),
                text("component-label", 92, 4, element.id()),
                text("component-value", 68, 82, element.value()));
        return frame.part();
    }

    private Component createInductor(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 248, 94),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 248 110" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="22" y1="48" x2="42" y2="48"/>
                          <path d="M42 48 A16 16 0 0 1 74 48"/>
                          <path d="M74 48 A16 16 0 0 1 106 48"/>
                          <path d="M106 48 A16 16 0 0 1 138 48"/>
                          <path d="M138 48 A16 16 0 0 1 170 48"/>
                          <line x1="170" y1="48" x2="226" y2="48"/>
                        </svg>
                        """, 0, 0, 248, 110),
                pin(element.id(), "A", 22, 48),
                pin(element.id(), "B", 226, 48),
                text("component-label", 110, 4, element.id()),
                text("component-value", 92, 82, element.value()));
        return frame.part();
    }

    private Component createCapacitor(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 96, 234),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 234" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="48" y1="46" x2="48" y2="92"/>
                          <line x1="28" y1="92" x2="68" y2="92"/>
                          <line x1="28" y1="142" x2="68" y2="142"/>
                          <line x1="48" y1="142" x2="48" y2="190"/>
                        </svg>
                        """, 0, 0, 96, 234),
                pin(element.id(), "A", 48, 46),
                pin(element.id(), "B", 48, 190),
                text("component-label", 110, 108, element.id()),
                text("component-value", 110, 126, element.value()));
        return frame.part();
    }

    private Component createVoltageSource(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(22, 0, 104, 272),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 172 334" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="86" y1="46" x2="86" y2="92"/>
                          <circle cx="86" cy="132" r="40"/>
                          <line x1="86" y1="172" x2="86" y2="326"/>
                          <line x1="78" y1="124" x2="94" y2="124"/>
                          <line x1="86" y1="116" x2="86" y2="132"/>
                          <line x1="78" y1="206" x2="94" y2="206"/>
                          <path d="M56 166c6-8 12-16 18-16s12 8 18 16 12 16 18 16 12-8 18-16"/>
                        </svg>
                        """, 0, 0, 172, 334),
                pin(element.id(), "POS", 86, 46),
                pin(element.id(), "NEG", 86, 326),
                text("component-label", 54, 4, element.id()),
                text("component-value", 18, 280, element.value() + " V"),
                text("component-value is-secondary", 30, 300, sourceModeLabel(element)));
        return frame.part();
    }

    private Component createCurrentSource(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(22, 0, 104, 272),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 172 334" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="86" y1="46" x2="86" y2="92"/>
                          <circle cx="86" cy="132" r="40"/>
                          <line x1="86" y1="172" x2="86" y2="326"/>
                          <line x1="86" y1="118" x2="86" y2="194"/>
                          <line x1="86" y1="194" x2="76" y2="180"/>
                          <line x1="86" y1="194" x2="96" y2="180"/>
                        </svg>
                        """, 0, 0, 172, 334),
                pin(element.id(), "POS", 86, 46),
                pin(element.id(), "NEG", 86, 326),
                text("component-label", 54, 4, element.id()),
                text("component-value", 18, 280, formatCurrentValue(element)),
                text("component-value is-secondary", 30, 300, sourceModeLabel(element)));
        return frame.part();
    }

    private Component createGround(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 96, 80),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 96 80" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="46" y1="6" x2="46" y2="28"/>
                          <line x1="18" y1="28" x2="74" y2="28"/>
                          <line x1="26" y1="38" x2="66" y2="38"/>
                          <line x1="34" y1="48" x2="58" y2="48"/>
                        </svg>
                        """, 0, 0, 96, 80),
                pin(element.id(), "REF", 46, 6),
                text("component-label", 10, 74, element.id()),
                text("component-value", 8, 90, element.value()));
        return frame.part();
    }

    private Component createDiode(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 180, 94),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 180 94" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="18" y1="48" x2="52" y2="48"/>
                          <polygon points="52,26 124,48 52,70"/>
                          <line x1="128" y1="20" x2="128" y2="76"/>
                          <line x1="128" y1="48" x2="162" y2="48"/>
                        </svg>
                        """, 0, 0, 180, 94),
                pin(element.id(), "ANODE", 18, 48),
                pin(element.id(), "CATHODE", 162, 48),
                text("component-label", 70, 4, element.id()),
                text("component-value", 56, 82, element.value()));
        return frame.part();
    }

    private Component createOpAmp(WorkspaceMockService.WorkspaceElement element) {
        PartFrame frame = part(element);
        frame.content().add(
                halo(0, 0, 220, 148),
                symbol("""
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 220 148" fill="none" stroke="currentColor" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round">
                          <line x1="0" y1="54" x2="48" y2="54"/>
                          <line x1="0" y1="94" x2="48" y2="94"/>
                          <line x1="160" y1="74" x2="220" y2="74"/>
                          <polygon points="48,18 160,74 48,130"/>
                          <line x1="18" y1="44" x2="30" y2="44"/>
                          <line x1="24" y1="38" x2="24" y2="50"/>
                          <line x1="18" y1="84" x2="30" y2="84"/>
                        </svg>
                        """, 0, 0, 220, 148),
                pin(element.id(), "IN+", 0, 54),
                pin(element.id(), "IN-", 0, 94),
                pin(element.id(), "OUT", 220, 74),
                text("component-label", 70, 0, element.id()),
                text("component-value", 64, 136, element.value()));
        return frame.part();
    }

    private Component createWireAssembly(WorkspaceMockService.ResolvedWire wire) {
        Div assembly = area("wire-assembly", 0, 0, 1280, 860);
        assembly.getElement().setAttribute("data-wire-id", wire.id());
        selectableWires.put(wire.id(), assembly);
        wire.segments().forEach(segment -> assembly.add(
                line("wire-segment", segment.x1(), segment.y1(), segment.x2(), segment.y2())));
        wire.nodes().forEach(node -> assembly.add(junction(node.x(), node.y())));
        return assembly;
    }

    private Component netLabel(WorkspaceMockService.ResolvedNet net) {
        Span label = text("sheet-net-label", net.labelX(), net.labelY(), net.name());
        label.getElement().setAttribute("data-net-name", net.name());
        label.getElement().setAttribute("data-net-key", net.key());
        selectableNets.put(net.key(), label);
        return label;
    }

    private PartFrame part(WorkspaceMockService.WorkspaceElement element) {
        Div part = area(
                "schematic-part",
                element.left(),
                element.top(),
                element.type().orientedWidth(element.orientation()),
                element.type().orientedHeight(element.orientation()));
        part.getElement().setAttribute("data-element-id", element.id());
        Div content = area("schematic-part-content", 0, 0, element.type().width(), element.type().height());
        content.getStyle()
                .set("left", "50%")
                .set("top", "50%")
                .set("transform", "translate(-50%, -50%) rotate(" + element.orientation().degrees() + "deg)");
        part.add(content);
        selectableParts.put(element.id(), part);
        return new PartFrame(part, content);
    }

    private Div pin(String elementId, String pinKey, double centerX, double centerY) {
        Div pin = area("wire-pin", centerX - 6, centerY - 6, 12, 12);
        pin.getElement().setAttribute("data-pin-key", pinKey);
        selectablePins.put(pinToken(elementId, pinKey), pin);
        return pin;
    }

    private static Div halo(double left, double top, double width, double height) {
        return area("selection-halo", left, top, width, height);
    }

    private static Div junction(double centerX, double centerY) {
        return area("wire-junction", centerX - 4, centerY - 4, 8, 8);
    }

    private static Svg symbol(String svgMarkup, double left, double top, double width, double height) {
        Svg svg = new Svg();
        svg.addClassName("schematic-symbol");
        svg.setSvg(svgMarkup);
        setBox(svg, left, top, width, height);
        return svg;
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

    private static void setBox(Component component, double left, double top, double width, double height) {
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

    private static String pinToken(String elementId, String pinKey) {
        return elementId + ":" + pinKey;
    }

    private static String sourceModeLabel(WorkspaceMockService.WorkspaceElement element) {
        String sourceType = WorkspaceMockService.formatSourceTypeLabel(element.sourceType());
        if (!WorkspaceMockService.SOURCE_TYPE_SINE.equals(WorkspaceMockService.normalizeSourceType(element.sourceType()))) {
            return sourceType;
        }
        return sourceType + " / " + WorkspaceMockService.formatFrequencyLabel(element.frequency());
    }

    private static String formatCurrentValue(WorkspaceMockService.WorkspaceElement element) {
        return element.value() + " A";
    }

    private record PartFrame(Div part, Div content) {
    }

    public interface InteractionHandler {
        void onCanvasClick(double canvasX, double canvasY);

        void onElementClick(String elementId);

        void onElementDragStart(String elementId, double canvasX, double canvasY);

        void onElementDrag(String elementId, double canvasX, double canvasY);

        void onElementDragEnd(String elementId, double canvasX, double canvasY);

        void onPinClick(String elementId, String pinKey);

        void onNetClick(String netKey);

        void onWireClick(String wireId);
    }
}
