package pl.polsl.bland.webapp.view.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

import java.math.BigDecimal;

public final class PropertiesWindow extends Div {
    public record ElementFormData(
            String id,
            String value,
            String left,
            String top,
            WorkspaceMockService.Orientation orientation,
            String sourceType,
            String frequency) {
    }

    private final Span caption = new Span();
    private final Span closeButton = new Span("x");
    private final Div parametersEmptyState = new Div();
    private final Div elementEditorContainer = new Div();
    private final Div wireContainer = new Div();
    private final TextField elementIdField = new TextField();
    private final TextField elementValueField = new TextField();
    private final TextField elementLeftField = new TextField();
    private final TextField elementTopField = new TextField();
    private final Select<WorkspaceMockService.Orientation> orientationSelect = new Select<>();
    private final Select<String> sourceTypeSelect = new Select<>();
    private final TextField sourceFrequencyField = new TextField();
    private final Span propType = readOnlyValue();
    private final Span propNodeA = readOnlyValue();
    private final Span propNodeB = readOnlyValue();
    private final Span propDescription = readOnlyValue();
    private final Span wireId = readOnlyValue();
    private final Span wireStartPin = readOnlyValue();
    private final Span wireEndPin = readOnlyValue();
    private final Span wireStartNet = readOnlyValue();
    private final Span wireEndNet = readOnlyValue();
    private final Span wireGeometry = readOnlyValue();
    private final Span wireDescription = readOnlyValue();
    private final Span simTrace = new Span();
    private final Span simPeak = new Span();
    private final Span simMin = new Span();
    private final Span simRms = new Span();
    private final Span simTmax = new Span();
    private final Span simNote = new Span();
    private final Div simulationEmptyState = new Div();
    private final Div simulationData = new Div();
    private Runnable applyChangesHandler = () -> {};
    private Runnable closeHandler = () -> {};
    private Double currentDefaultFrequency;
    private boolean suppressEditorEvents;

    public PropertiesWindow() {
        addClassName("floating-window");
        configureEditorFields();
        closeButton.addClickListener(event -> closeHandler.run());
        add(buildTitleBar(), buildBody());
        clear("Brak aktywnego elementu.");
    }

    public void clear(String message) {
        caption.setText("Brak aktywnego elementu");
        parametersEmptyState.setText(message);
        parametersEmptyState.setVisible(true);
        elementEditorContainer.setVisible(false);
        wireContainer.setVisible(false);
        clearElementEditor();
        propType.setText("-");
        propNodeA.setText("-");
        propNodeB.setText("-");
        propDescription.setText(message);
        simTrace.setText("-");
        simPeak.setText("-");
        simMin.setText("-");
        simRms.setText("-");
        simTmax.setText("-");
        simNote.setText(message);
        simulationEmptyState.setText(message);
        simulationEmptyState.setVisible(true);
        simulationData.setVisible(false);
    }

    public void update(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.ElementDetails details,
            boolean simulationReady) {
        populateElementDetails(element, details);
        caption.setText("Aktywny element: " + details.id());
        simTrace.setText(details.traceName());
        simPeak.setText(details.peak());
        simMin.setText(details.min());
        simRms.setText(details.rms());
        simTmax.setText(details.timeOfPeak());
        simNote.setText(details.simulationNote());
        simulationEmptyState.setText("Uruchom symulację, aby wyświetlić statystyki dla " + details.id() + ".");
        simulationEmptyState.setVisible(!simulationReady);
        simulationData.setVisible(simulationReady);
    }

    public void showMeasuredElement(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.ElementDetails details,
            String traceName,
            String peak,
            String min,
            String rmsOrAverage,
            String timeOfPeak,
            String note) {
        populateElementDetails(element, details);
        caption.setText("Aktywny element: " + details.id());
        simTrace.setText(traceName);
        simPeak.setText(peak);
        simMin.setText(min);
        simRms.setText(rmsOrAverage);
        simTmax.setText(timeOfPeak);
        simNote.setText(note);
        simulationEmptyState.setVisible(false);
        simulationData.setVisible(true);
    }

    public void showWire(WorkspaceMockService.WireDetails details) {
        caption.setText("Aktywny przewód: " + details.id());
        parametersEmptyState.setVisible(false);
        elementEditorContainer.setVisible(false);
        wireContainer.setVisible(true);
        clearElementEditor();
        wireId.setText(details.id());
        wireStartPin.setText(details.startPin());
        wireEndPin.setText(details.endPin());
        wireStartNet.setText(details.startNet());
        wireEndNet.setText(details.endNet());
        wireGeometry.setText(details.geometry());
        wireDescription.setText(details.description());
        simTrace.setText("-");
        simPeak.setText("-");
        simMin.setText("-");
        simRms.setText("-");
        simTmax.setText("-");
        simNote.setText("Przewody nie mają osobnych statystyk symulacji.");
        simulationEmptyState.setText("Przewód jest elementem połączenia. W tym miejscu możesz śledzić jego topologię.");
        simulationEmptyState.setVisible(true);
        simulationData.setVisible(false);
    }

    public ElementFormData readElementForm() {
        return new ElementFormData(
                elementIdField.getValue(),
                elementValueField.getValue(),
                elementLeftField.getValue(),
                elementTopField.getValue(),
                orientationSelect.getValue(),
                sourceTypeSelect.getValue(),
                sourceFrequencyField.getValue());
    }

    public void setApplyChangesHandler(Runnable applyChangesHandler) {
        this.applyChangesHandler = applyChangesHandler == null ? () -> {} : applyChangesHandler;
    }

    public void setCloseHandler(Runnable closeHandler) {
        this.closeHandler = closeHandler == null ? () -> {} : closeHandler;
    }

    private Component buildTitleBar() {
        Div titleBar = new Div();
        titleBar.addClassName("window-titlebar");

        Div textGroup = new Div();
        Span title = new Span("Właściwości elementu");
        title.addClassName("window-title");
        caption.addClassName("window-caption");
        textGroup.add(title, caption);

        closeButton.addClassName("window-close");
        titleBar.add(textGroup, closeButton);
        return titleBar;
    }

    private Component buildBody() {
        Div body = new Div();
        body.addClassName("window-body");
        body.add(buildParametersBlock(), buildSimulationBlock());
        return body;
    }

    private Component buildParametersBlock() {
        Div block = new Div();
        block.addClassName("panel-block");
        parametersEmptyState.addClassNames("hint-box", "properties-empty-state");
        elementEditorContainer.add(buildElementPropertyGrid());
        wireContainer.add(buildWireGrid());
        block.add(sectionTitle("Parametry"), parametersEmptyState, elementEditorContainer, wireContainer);
        return block;
    }

    private Component buildSimulationBlock() {
        Div block = new Div();
        block.addClassName("panel-block");
        simulationEmptyState.addClassNames("hint-box", "simulation-empty-state");
        simulationData.addClassName("simulation-data");
        simulationData.add(
                propertyLabel("Ślad"), simTrace,
                propertyLabel("Wartość szczytowa"), simPeak,
                propertyLabel("Wartość minimalna"), simMin,
                propertyLabel("RMS / średnia"), simRms,
                propertyLabel("Chwila maksimum"), simTmax,
                propertyLabel("Notatka"), simNote);
        block.add(sectionTitle("Dane po symulacji"), simulationEmptyState, simulationData);
        return block;
    }

    private Component buildElementPropertyGrid() {
        Div grid = new Div();
        grid.addClassName("property-grid");
        grid.add(
                propertyLabel("ID elementu"), elementIdField,
                propertyLabel("Typ"), propType,
                propertyLabel("Wartość"), elementValueField,
                propertyLabel("Tryb źródła"), sourceTypeSelect,
                propertyLabel("Częstotliwość"), sourceFrequencyField,
                propertyLabel("Węzeł A"), propNodeA,
                propertyLabel("Węzeł B"), propNodeB,
                propertyLabel("Orientacja"), orientationSelect,
                propertyLabel("Pozycja X"), elementLeftField,
                propertyLabel("Pozycja Y"), elementTopField,
                propertyLabel("Opis"), propDescription);
        return grid;
    }

    private Component buildWireGrid() {
        Div grid = new Div();
        grid.addClassName("property-grid");
        grid.add(
                propertyLabel("ID przewodu"), wireId,
                propertyLabel("Początek"), wireStartPin,
                propertyLabel("Koniec"), wireEndPin,
                propertyLabel("Net start"), wireStartNet,
                propertyLabel("Net koniec"), wireEndNet,
                propertyLabel("Geometria"), wireGeometry,
                propertyLabel("Opis"), wireDescription);
        return grid;
    }

    private Span sectionTitle(String text) {
        Span title = new Span(text);
        title.addClassName("section-title");
        return title;
    }

    private Span propertyLabel(String text) {
        Span label = new Span(text);
        label.addClassName("property-label");
        return label;
    }

    private void configureEditorFields() {
        configureTextField(elementIdField, "ID elementu");
        configureTextField(elementValueField, "Wartość elementu");
        configureTextField(elementLeftField, "X [px]");
        configureTextField(elementTopField, "Y [px]");
        configureTextField(sourceFrequencyField, "Częstotliwość [Hz]");

        orientationSelect.setItems(WorkspaceMockService.Orientation.values());
        orientationSelect.setItemLabelGenerator(WorkspaceMockService.Orientation::label);
        orientationSelect.addClassName("property-field");
        orientationSelect.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                requestApplyChanges();
            }
        });

        sourceTypeSelect.setItems(WorkspaceMockService.SOURCE_TYPE_DC, WorkspaceMockService.SOURCE_TYPE_SINE);
        sourceTypeSelect.setItemLabelGenerator(type -> WorkspaceMockService.SOURCE_TYPE_SINE.equals(type) ? "sinus" : "dc");
        sourceTypeSelect.addClassName("property-field");
        sourceTypeSelect.addValueChangeListener(event -> {
            updateSourceFrequencyFieldState(true);
            if (event.isFromClient()) {
                requestApplyChanges();
            }
        });
    }

    private void configureTextField(TextField field, String placeholder) {
        field.setPlaceholder(placeholder);
        field.setClearButtonVisible(true);
        field.addClassName("property-field");
        field.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                requestApplyChanges();
            }
        });
    }

    private void populateElementDetails(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.ElementDetails details) {
        parametersEmptyState.setVisible(false);
        elementEditorContainer.setVisible(true);
        wireContainer.setVisible(false);
        suppressEditorEvents = true;
        try {
            elementIdField.setValue(details.id());
            elementValueField.setValue(details.value());
            elementLeftField.setValue(formatNumber(element.left()));
            elementTopField.setValue(formatNumber(element.top()));
            orientationSelect.setValue(element.orientation());
            propType.setText(details.typeLabel());
            propNodeA.setText(details.nodeA());
            propNodeB.setText(details.nodeB());
            propDescription.setText(details.description());

            if (!element.isSource()) {
                currentDefaultFrequency = null;
                sourceTypeSelect.clear();
                sourceTypeSelect.setEnabled(false);
                sourceFrequencyField.clear();
                sourceFrequencyField.setEnabled(false);
                return;
            }

            String normalizedType = WorkspaceMockService.normalizeSourceType(element.sourceType());
            currentDefaultFrequency = defaultFrequency(element.type(), WorkspaceMockService.SOURCE_TYPE_SINE);
            sourceTypeSelect.setEnabled(true);
            sourceTypeSelect.setValue(normalizedType);
            sourceFrequencyField.setValue(element.frequency() == null ? "" : formatNumber(element.frequency()));
            updateSourceFrequencyFieldState(false);
        } finally {
            suppressEditorEvents = false;
        }
    }

    private void clearElementEditor() {
        suppressEditorEvents = true;
        try {
            elementIdField.clear();
            elementValueField.clear();
            elementLeftField.clear();
            elementTopField.clear();
            orientationSelect.clear();
            sourceTypeSelect.clear();
            sourceTypeSelect.setEnabled(false);
            sourceFrequencyField.clear();
            sourceFrequencyField.setEnabled(false);
            currentDefaultFrequency = null;
            wireId.setText("-");
            wireStartPin.setText("-");
            wireEndPin.setText("-");
            wireStartNet.setText("-");
            wireEndNet.setText("-");
            wireGeometry.setText("-");
            wireDescription.setText("-");
        } finally {
            suppressEditorEvents = false;
        }
    }

    private void updateSourceFrequencyFieldState(boolean populateDefaultWhenEmpty) {
        String normalizedType = WorkspaceMockService.normalizeSourceType(sourceTypeSelect.getValue());
        boolean sine = sourceTypeSelect.isEnabled() && WorkspaceMockService.SOURCE_TYPE_SINE.equals(normalizedType);
        sourceFrequencyField.setEnabled(sine);
        if (!sine) {
            sourceFrequencyField.clear();
            return;
        }

        if (populateDefaultWhenEmpty
                && (sourceFrequencyField.getValue() == null || sourceFrequencyField.getValue().isBlank())
                && currentDefaultFrequency != null) {
            sourceFrequencyField.setValue(formatNumber(currentDefaultFrequency));
        }
    }

    private void requestApplyChanges() {
        if (!suppressEditorEvents) {
            applyChangesHandler.run();
        }
    }

    private static Span readOnlyValue() {
        Span value = new Span();
        value.addClassName("property-value");
        return value;
    }

    private static String formatNumber(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static Double defaultFrequency(
            WorkspaceMockService.ElementType type,
            String sourceType) {
        if (!type.isSource() || !WorkspaceMockService.SOURCE_TYPE_SINE.equals(WorkspaceMockService.normalizeSourceType(sourceType))) {
            return null;
        }
        return type == WorkspaceMockService.ElementType.VOLTAGE ? 1000.0 : 50.0;
    }
}
