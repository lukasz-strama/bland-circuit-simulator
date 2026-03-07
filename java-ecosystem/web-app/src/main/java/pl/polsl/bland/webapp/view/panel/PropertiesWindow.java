package pl.polsl.bland.webapp.view.panel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import pl.polsl.bland.webapp.service.WorkspaceMockService;

public final class PropertiesWindow extends Div {
    private final Span caption = new Span();
    private final Span closeButton = new Span("x");
    private final Span propId = new Span();
    private final Span propType = new Span();
    private final Span propValue = new Span();
    private final Span propNodeA = new Span();
    private final Span propNodeB = new Span();
    private final Span propOrientation = new Span();
    private final Span propDescription = new Span();
    private final Span simTrace = new Span();
    private final Span simPeak = new Span();
    private final Span simMin = new Span();
    private final Span simRms = new Span();
    private final Span simTmax = new Span();
    private final Span simNote = new Span();
    private final Div simulationEmptyState = new Div();
    private final Div simulationData = new Div();
    private Runnable closeHandler = () -> {};

    public PropertiesWindow() {
        addClassName("floating-window");
        closeButton.addClickListener(event -> closeHandler.run());
        add(buildTitleBar(), buildBody());
    }

    public void clear(String message) {
        caption.setText("Brak aktywnego elementu");
        propId.setText("-");
        propType.setText("-");
        propValue.setText("-");
        propNodeA.setText("-");
        propNodeB.setText("-");
        propOrientation.setText("-");
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

    public void update(WorkspaceMockService.ElementDetails details, boolean simulationReady) {
        caption.setText("Aktywny element: " + details.id());
        propId.setText(details.id());
        propType.setText(details.typeLabel());
        propValue.setText(details.value());
        propNodeA.setText(details.nodeA());
        propNodeB.setText(details.nodeB());
        propOrientation.setText(details.orientation());
        propDescription.setText(details.description());
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

    public void showWire(WorkspaceMockService.WireDetails details) {
        caption.setText("Aktywny przewód: " + details.id());
        propId.setText(details.id());
        propType.setText("Przewód");
        propValue.setText(details.startPin() + " -> " + details.endPin());
        propNodeA.setText(details.startNet());
        propNodeB.setText(details.endNet());
        propOrientation.setText(details.geometry());
        propDescription.setText(details.description());
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
        block.add(sectionTitle("Parametry"), propertyGrid());
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

    private Component propertyGrid() {
        Div grid = new Div();
        grid.addClassName("property-grid");
        grid.add(
                propertyLabel("ID elementu"), propId,
                propertyLabel("Typ"), propType,
                propertyLabel("Wartość"), propValue,
                propertyLabel("Węzeł A"), propNodeA,
                propertyLabel("Węzeł B"), propNodeB,
                propertyLabel("Orientacja"), propOrientation,
                propertyLabel("Opis"), propDescription);
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
}
