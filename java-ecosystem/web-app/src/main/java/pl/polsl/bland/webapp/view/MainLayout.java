package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.polsl.bland.webapp.service.WorkspaceMockService;
import pl.polsl.bland.webapp.view.panel.PropertiesWindow;
import pl.polsl.bland.webapp.view.panel.ResultsWindow;

@Route("")
@PageTitle("Bland Circuit Simulator")
public class MainLayout extends Div {
    private final WorkspaceMockService workspaceMockService;
    private final PropertiesWindow propertiesWindow;
    private final ResultsWindow resultsWindow;
    private final SchematicPreview schematicPreview;
    private final Span activeSymbolReadout;
    private final Span simulationBadgeText;
    private final Span statusSimulationValue;
    private final Span statusMessageValue;

    public MainLayout(WorkspaceMockService workspaceMockService) {
        this.workspaceMockService = workspaceMockService;
        this.propertiesWindow = new PropertiesWindow();
        this.resultsWindow = new ResultsWindow();
        this.schematicPreview = new SchematicPreview(this::selectElement);
        this.activeSymbolReadout = createWideReadout("R / Rezystor");
        this.simulationBadgeText = new Span("Brak wyników");
        this.statusSimulationValue = new Span("Brak uruchomienia");
        this.statusMessageValue = new Span("Makieta UI jest gotowa. Następny krok: mockowana interakcja.");

        addClassName("main-view");
        setSizeFull();

        add(buildAppShell());
        selectElement(workspaceMockService.defaultElement().id());
    }

    private void selectElement(String elementId) {
        workspaceMockService.findElement(elementId).ifPresent(details -> {
            propertiesWindow.update(details);
            resultsWindow.update(details);
            activeSymbolReadout.setText(details.symbol() + " / " + details.typeLabel());
            simulationBadgeText.setText("Mock: " + details.traceName());
            statusSimulationValue.setText("Podgląd " + details.traceName());
            statusMessageValue.setText("Zaznaczono " + details.id() + ". Kliknij inny element na arkuszu.");
            schematicPreview.setSelectedElement(elementId);
        });
    }

    private Div buildAppShell() {
        Div appShell = new Div();
        appShell.addClassName("app-shell");
        appShell.add(
                buildMenuBar(),
                buildToolbar(),
                buildComponentBar(),
                buildEditorShell(),
                buildStatusBar());
        return appShell;
    }

    private Div buildMenuBar() {
        Div menuBar = new Div();
        menuBar.addClassName("menubar");
        menuBar.add(
                buildMenuButton("Plik"),
                buildMenuButton("Edycja"),
                buildMenuButton("Widok"),
                buildMenuButton("Wstaw"),
                buildMenuButton("Narzędzia"),
                buildMenuButton("Symulacja"),
                buildMenuButton("Pomoc"));

        Div meta = new Div();
        meta.addClassName("menubar-meta");
        meta.add(new Span("Projekt: filtr_rlc_lab.asc"));
        meta.add(new Span("Arkusz 1 / 1"));
        menuBar.add(meta);
        return menuBar;
    }

    private Div buildMenuButton(String label) {
        Div group = new Div();
        group.addClassName("menu-group");

        Span button = new Span(label);
        button.addClassName("menu-button");
        group.add(button);
        return group;
    }

    private Div buildToolbar() {
        Div toolbar = new Div();
        toolbar.addClassName("toolbar");

        toolbar.add(buildToolbarGroup(
                createAction("Nowy", "tool-button"),
                createAction("Zapisz", "tool-button"),
                createAction("Eksport", "tool-button"),
                createAction("Symuluj", "tool-button", "is-primary"),
                createAction("Pokaż wyniki", "tool-button")));

        toolbar.add(separator());

        Select<String> analysis = new Select<>();
        analysis.setItems("Analiza przejściowa", "Punkt pracy DC");
        analysis.setValue("Analiza przejściowa");
        analysis.addClassName("toolbar-select");
        toolbar.add(buildToolbarGroup(createLabel("Analiza"), analysis));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createLabel("Narzędzie"), createReadout("Zaznacz")));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(
                createAction("-", "mini-button"),
                createReadout("92%"),
                createAction("+", "mini-button"),
                createAction("Dopasuj", "mini-button"),
                createAction("100%", "mini-button")));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(buildStatusBadge()));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createAction("?", "icon-button")));
        return toolbar;
    }

    private Div buildComponentBar() {
        Div componentBar = new Div();
        componentBar.addClassName("componentbar");

        componentBar.add(buildComponentGroup(
                createAction("Komponent...", "tool-button"),
                createLabel("Biblioteka")));

        componentBar.add(buildComponentGroup(
                createQuickComponent("R", "Rezystor", true),
                createQuickComponent("C", "Kondensator", false),
                createQuickComponent("L", "Cewka", false),
                createQuickComponent("V", "Źródło", false),
                createQuickComponent("0", "Masa", false),
                createQuickComponent("D", "Dioda", false),
                createQuickComponent("OP", "Wzmacniacz", false)));

        TextField search = new TextField();
        search.setPlaceholder("Szukaj w bibliotece: rezystor, dioda, opamp...");
        search.addClassName("component-search");
        search.setClearButtonVisible(true);

        Div fillGroup = buildComponentGroup(search, createAction("Otwórz bibliotekę", "mini-button"));
        fillGroup.addClassName("is-fill");
        componentBar.add(fillGroup);

        componentBar.add(buildComponentGroup(createLabel("Aktywny symbol"), activeSymbolReadout));
        return componentBar;
    }

    private Div buildEditorShell() {
        Div editorShell = new Div();
        editorShell.addClassName("editor-shell");
        editorShell.add(buildToolRail(), buildWorkspacePanel());
        return editorShell;
    }

    private Div buildToolRail() {
        Div toolRail = new Div();
        toolRail.addClassName("tool-rail");
        toolRail.add(
                createAction("Z", "rail-button", "is-active"),
                createAction("W", "rail-button"),
                createAction("S", "rail-button"),
                createAction("X", "rail-button"),
                createAction("D", "rail-button"));
        return toolRail;
    }

    private Div buildWorkspacePanel() {
        Div workspacePanel = new Div();
        workspacePanel.addClassName("workspace-panel");
        workspacePanel.getElement().setAttribute("data-tool", "select");

        Div workspaceViewport = new Div();
        workspaceViewport.addClassName("workspace-viewport");
        workspaceViewport.add(schematicPreview);

        workspacePanel.add(workspaceViewport, propertiesWindow, resultsWindow);
        return workspacePanel;
    }

    private Div buildStatusBar() {
        Div statusBar = new Div();
        statusBar.addClassName("statusbar");
        statusBar.add(
                createStatusSegment("Arkusz", new Span("1")),
                createStatusSegment("Narzędzie", new Span("Zaznacz")),
                createStatusSegment("Analiza", new Span("Analiza przejściowa")),
                createStatusSegment("Symulacja", statusSimulationValue),
                createStretchStatusSegment("Komunikat", statusMessageValue),
                createStatusSegment("Zoom", new Span("92%")));
        return statusBar;
    }

    private Div buildToolbarGroup(Component... children) {
        Div group = new Div();
        group.addClassName("toolbar-group");
        group.add(children);
        return group;
    }

    private Div buildComponentGroup(Component... children) {
        Div group = new Div();
        group.addClassName("componentbar-group");
        group.add(children);
        return group;
    }

    private Div separator() {
        Div separator = new Div();
        separator.addClassName("toolbar-separator");
        return separator;
    }

    private Span createAction(String text, String... classNames) {
        Span action = new Span(text);
        action.addClassNames(classNames);
        return action;
    }

    private Span createLabel(String text) {
        Span label = new Span(text);
        label.addClassName("toolbar-label");
        return label;
    }

    private Span createReadout(String text) {
        Span readout = new Span(text);
        readout.addClassName("toolbar-readout");
        return readout;
    }

    private Span createWideReadout(String text) {
        Span readout = createReadout(text);
        readout.addClassName("is-wide");
        return readout;
    }

    private Div buildStatusBadge() {
        Div badge = new Div();
        badge.addClassName("badge");
        Span dot = new Span();
        dot.addClassNames("status-dot", "is-idle");
        badge.add(dot, simulationBadgeText);
        return badge;
    }

    private Div createQuickComponent(String glyph, String label, boolean active) {
        Div component = new Div();
        component.addClassName("quick-component");
        if (active) {
            component.addClassName("is-active");
        }

        Span symbol = new Span(glyph);
        symbol.addClassName("quick-component-glyph");
        component.add(symbol, new Span(label));
        return component;
    }

    private Div createStatusSegment(String label, Component value) {
        Div segment = new Div();
        segment.addClassName("status-segment");
        Span labelSpan = new Span(label);
        labelSpan.addClassName("status-label");
        segment.add(labelSpan, value);
        return segment;
    }

    private Div createStretchStatusSegment(String label, Component value) {
        Div segment = createStatusSegment(label, value);
        segment.addClassName("is-stretch");
        return segment;
    }
}
