package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.polsl.bland.webapp.service.WorkspaceMockService;
import pl.polsl.bland.webapp.view.panel.PropertiesWindow;
import pl.polsl.bland.webapp.view.panel.ResultsWindow;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Route("")
@PageTitle("Bland Circuit Simulator")
public class MainLayout extends Div {
    private static final String ANALYSIS_TRANSIENT = "Analiza przejściowa";
    private static final String ANALYSIS_DC = "Punkt pracy DC";
    private static final double DEFAULT_ZOOM = 0.92;
    private static final double MIN_ZOOM = 0.50;
    private static final double MAX_ZOOM = 1.60;
    private static final double ZOOM_STEP = 0.08;

    private final WorkspaceMockService workspaceMockService;
    private final PropertiesWindow propertiesWindow;
    private final ResultsWindow resultsWindow;
    private final SchematicPreview schematicPreview;
    private final Div workspacePanel = new Div();
    private final Span activeSymbolReadout = createWideReadout("");
    private final Span toolbarToolValue = createReadout("");
    private final Span zoomReadout = createReadout("");
    private final Span simulationBadgeText = new Span();
    private final Span simulationBadgeDot = new Span();
    private final Span statusSimulationValue = new Span();
    private final Span statusMessageValue = new Span();
    private final Span statusToolValue = new Span();
    private final Span statusAnalysisValue = new Span();
    private final Span statusZoomValue = new Span();
    private final Select<String> analysisSelect = new Select<>();
    private final TextField componentSearch = new TextField();
    private final Map<WorkspaceTool, Span> railButtons = new EnumMap<>(WorkspaceTool.class);
    private final Map<QuickComponent, Div> quickComponentButtons = new LinkedHashMap<>();

    private String analysisLabel = ANALYSIS_TRANSIENT;
    private WorkspaceTool activeTool = WorkspaceTool.SELECT;
    private QuickComponent activeComponent = QuickComponent.RESISTOR;
    private String selectedElementId;
    private double zoom = DEFAULT_ZOOM;
    private boolean simulationReady;
    private boolean suppressAnalysisEvents;

    public MainLayout(WorkspaceMockService workspaceMockService) {
        this.workspaceMockService = workspaceMockService;
        this.propertiesWindow = new PropertiesWindow();
        this.resultsWindow = new ResultsWindow();
        this.schematicPreview = new SchematicPreview(this::selectElement);

        configureAnalysisSelect();
        configureComponentSearch();
        configureFloatingWindows();

        addClassName("main-view");
        setSizeFull();

        add(buildAppShell());
        resetWorkspace();
    }

    private void configureAnalysisSelect() {
        analysisSelect.setItems(ANALYSIS_TRANSIENT, ANALYSIS_DC);
        analysisSelect.addClassName("toolbar-select");
        analysisSelect.addValueChangeListener(event -> {
            if (!suppressAnalysisEvents && event.getValue() != null) {
                setAnalysis(event.getValue(), false, false);
            }
        });
    }

    private void configureComponentSearch() {
        componentSearch.setPlaceholder("Szukaj w bibliotece: rezystor, dioda, opamp...");
        componentSearch.addClassName("component-search");
        componentSearch.setClearButtonVisible(true);
        componentSearch.setValueChangeMode(ValueChangeMode.EAGER);
        componentSearch.addValueChangeListener(event -> applyComponentFilter(event.getValue()));
    }

    private void configureFloatingWindows() {
        propertiesWindow.setCloseHandler(() -> {
            propertiesWindow.setVisible(false);
            statusMessageValue.setText("Ukryto panel właściwości.");
        });
        resultsWindow.setCloseHandler(() -> toggleResultsWindow(false));
    }

    private void resetWorkspace() {
        simulationReady = false;
        resultsWindow.setVisible(false);
        propertiesWindow.setVisible(true);
        componentSearch.clear();
        setAnalysis(ANALYSIS_TRANSIENT, true, true);
        setActiveTool(WorkspaceTool.SELECT, true);
        setActiveComponent(QuickComponent.RESISTOR, true);
        applyComponentFilter("");
        setZoom(DEFAULT_ZOOM, true);
        selectedElementId = workspaceMockService.defaultElement().id();
        refreshSelectionPanels();
        updateSimulationIndicators();
        statusMessageValue.setText("Nowy projekt mockowany został przygotowany.");
    }

    private void selectElement(String elementId) {
        selectedElementId = elementId;
        propertiesWindow.setVisible(true);
        refreshSelectionPanels();

        workspaceMockService.findElement(elementId).ifPresent(details -> {
            if (activeTool == WorkspaceTool.PROBE) {
                resultsWindow.setVisible(true);
                resultsWindow.setActiveTab(ResultsWindow.ResultTab.PLOT);
                statusMessageValue.setText("Sonda ustawiona na śladzie " + details.traceName() + ".");
                return;
            }

            if (activeTool == WorkspaceTool.DELETE) {
                statusMessageValue.setText("Tryb Usuń jest jeszcze makietą. " + details.id() + " nie został usunięty.");
                return;
            }

            statusMessageValue.setText("Zaznaczono " + details.id() + ". Kliknij inny element na arkuszu.");
        });
    }

    private void refreshSelectionPanels() {
        if (selectedElementId == null) {
            return;
        }

        workspaceMockService.findElement(selectedElementId).ifPresent(details -> {
            propertiesWindow.update(details, simulationReady);
            resultsWindow.update(details, analysisLabel, simulationReady);
            schematicPreview.setSelectedElement(selectedElementId);
        });
    }

    private void runSimulation() {
        simulationReady = true;
        resultsWindow.setVisible(true);
        resultsWindow.setActiveTab(ResultsWindow.ResultTab.SUMMARY);
        refreshSelectionPanels();
        updateSimulationIndicators();
        statusMessageValue.setText("Mockowana symulacja zakończona dla: " + analysisLabel + ".");
    }

    private void toggleResultsWindow(boolean visible) {
        resultsWindow.setVisible(visible);
        if (visible) {
            resultsWindow.setActiveTab(ResultsWindow.ResultTab.SUMMARY);
            statusMessageValue.setText("Otwarto okno wyników.");
        } else {
            statusMessageValue.setText("Ukryto okno wyników.");
        }
    }

    private void setAnalysis(String nextAnalysis, boolean silent, boolean syncSelect) {
        analysisLabel = nextAnalysis;
        if (syncSelect) {
            suppressAnalysisEvents = true;
            analysisSelect.setValue(nextAnalysis);
            suppressAnalysisEvents = false;
        }

        statusAnalysisValue.setText(nextAnalysis);
        refreshSelectionPanels();
        updateSimulationIndicators();

        if (!silent) {
            statusMessageValue.setText("Tryb analizy ustawiono na: " + nextAnalysis + ".");
        }
    }

    private void setActiveTool(WorkspaceTool tool, boolean silent) {
        activeTool = tool;
        workspacePanel.getElement().setAttribute("data-tool", tool.key());
        toolbarToolValue.setText(tool.label());
        statusToolValue.setText(tool.label());
        railButtons.forEach((key, button) -> setClass(button, "is-active", key == tool));

        if (!silent) {
            statusMessageValue.setText("Aktywne narzędzie: " + tool.label() + ".");
        }
    }

    private void setActiveComponent(QuickComponent component, boolean silent) {
        activeComponent = component;
        activeSymbolReadout.setText(component.glyph() + " / " + component.label());
        quickComponentButtons.forEach((key, button) -> setClass(button, "is-active", key == component));

        if (!silent) {
            statusMessageValue.setText("Aktywny komponent biblioteki: " + component.label() + ".");
        }
    }

    private void applyComponentFilter(String filterValue) {
        String normalized = filterValue == null ? "" : filterValue.trim().toLowerCase(Locale.ROOT);
        QuickComponent fallback = null;

        for (Map.Entry<QuickComponent, Div> entry : quickComponentButtons.entrySet()) {
            boolean visible = normalized.isBlank() || entry.getKey().matches(normalized);
            entry.getValue().setVisible(visible);
            if (visible && fallback == null) {
                fallback = entry.getKey();
            }
        }

        if (!quickComponentButtons.get(activeComponent).isVisible() && fallback != null) {
            setActiveComponent(fallback, true);
        }
    }

    private void setZoom(double nextZoom, boolean silent) {
        zoom = clamp(nextZoom, MIN_ZOOM, MAX_ZOOM);
        getStyle().set("--sheet-scale", formatScale(zoom));

        String label = Math.round(zoom * 100) + "%";
        zoomReadout.setText(label);
        statusZoomValue.setText(label);

        if (!silent) {
            statusMessageValue.setText("Zoom arkusza ustawiono na: " + label + ".");
        }
    }

    private void fitView() {
        setZoom(DEFAULT_ZOOM, true);
        statusMessageValue.setText("Widok dopasowano do arkusza.");
    }

    private void updateSimulationIndicators() {
        simulationBadgeDot.removeClassNames("is-idle", "is-done");
        if (simulationReady) {
            simulationBadgeDot.addClassName("is-done");
            simulationBadgeText.setText("Wyniki gotowe");
            statusSimulationValue.setText("Zakończono dla " + analysisLabel);
        } else {
            simulationBadgeDot.addClassName("is-idle");
            simulationBadgeText.setText("Brak wyników");
            statusSimulationValue.setText("Brak uruchomienia");
        }
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

        Span newProject = createAction("Nowy", "tool-button");
        newProject.addClickListener(event -> resetWorkspace());

        Span saveProject = createAction("Zapisz", "tool-button");
        saveProject.addClickListener(event -> statusMessageValue.setText("Zapis zostanie podpięty po integracji z backendem."));

        Span exportProject = createAction("Eksport", "tool-button");
        exportProject.addClickListener(event -> statusMessageValue.setText("Eksport jest jeszcze makietą interfejsu."));

        Span simulate = createAction("Symuluj", "tool-button", "is-primary");
        simulate.addClickListener(event -> runSimulation());

        Span showResults = createAction("Pokaż wyniki", "tool-button");
        showResults.addClickListener(event -> toggleResultsWindow(true));

        Span zoomOut = createAction("-", "mini-button");
        zoomOut.addClickListener(event -> setZoom(zoom - ZOOM_STEP, false));

        Span zoomIn = createAction("+", "mini-button");
        zoomIn.addClickListener(event -> setZoom(zoom + ZOOM_STEP, false));

        Span zoom100 = createAction("100%", "mini-button");
        zoom100.addClickListener(event -> setZoom(1.0, false));

        Span fitView = createAction("Dopasuj", "mini-button");
        fitView.addClickListener(event -> fitView());

        Span help = createAction("?", "icon-button");
        help.addClickListener(event -> statusMessageValue.setText("Skróty makiety: W - przewód, S - sonda, X - usuń, kliknięcie elementu - podgląd."));

        toolbar.add(buildToolbarGroup(
                newProject,
                saveProject,
                exportProject,
                simulate,
                showResults));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createLabel("Analiza"), analysisSelect));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createLabel("Narzędzie"), toolbarToolValue));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(
                zoomOut,
                zoomReadout,
                zoomIn,
                fitView,
                zoom100));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(buildStatusBadge()));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(help));
        return toolbar;
    }

    private Div buildComponentBar() {
        Div componentBar = new Div();
        componentBar.addClassName("componentbar");

        Span openLibrary = createAction("Komponent...", "tool-button");
        openLibrary.addClickListener(event -> {
            componentSearch.focus();
            statusMessageValue.setText("Filtruj bibliotekę szybką, aby wybrać aktywny komponent.");
        });

        componentBar.add(buildComponentGroup(openLibrary, createLabel("Biblioteka")));
        componentBar.add(buildComponentGroup(
                createQuickComponent(QuickComponent.RESISTOR),
                createQuickComponent(QuickComponent.CAPACITOR),
                createQuickComponent(QuickComponent.INDUCTOR),
                createQuickComponent(QuickComponent.VOLTAGE),
                createQuickComponent(QuickComponent.GROUND),
                createQuickComponent(QuickComponent.DIODE),
                createQuickComponent(QuickComponent.OPAMP)));

        Span openLibraryDialog = createAction("Otwórz bibliotekę", "mini-button");
        openLibraryDialog.addClickListener(event -> statusMessageValue.setText("Pełna biblioteka pojawi się w kolejnym etapie prac."));

        Div fillGroup = buildComponentGroup(componentSearch, openLibraryDialog);
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
                createToolButton(WorkspaceTool.SELECT),
                createToolButton(WorkspaceTool.WIRE),
                createToolButton(WorkspaceTool.PROBE),
                createToolButton(WorkspaceTool.DELETE),
                createFitButton());
        return toolRail;
    }

    private Div buildWorkspacePanel() {
        workspacePanel.addClassName("workspace-panel");
        workspacePanel.getElement().setAttribute("data-tool", WorkspaceTool.SELECT.key());

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
                createStatusSegment("Narzędzie", statusToolValue),
                createStatusSegment("Analiza", statusAnalysisValue),
                createStatusSegment("Symulacja", statusSimulationValue),
                createStretchStatusSegment("Komunikat", statusMessageValue),
                createStatusSegment("Zoom", statusZoomValue));
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

    private Div createQuickComponent(QuickComponent component) {
        Div button = new Div();
        button.addClassName("quick-component");
        button.addClickListener(event -> setActiveComponent(component, false));

        Span symbol = new Span(component.glyph());
        symbol.addClassName("quick-component-glyph");
        button.add(symbol, new Span(component.label()));

        quickComponentButtons.put(component, button);
        return button;
    }

    private Span createToolButton(WorkspaceTool tool) {
        Span button = createAction(tool.shortLabel(), "rail-button");
        button.getElement().setAttribute("title", tool.label());
        button.addClickListener(event -> setActiveTool(tool, false));
        railButtons.put(tool, button);
        return button;
    }

    private Span createFitButton() {
        Span button = createAction("D", "rail-button");
        button.getElement().setAttribute("title", "Dopasuj widok");
        button.addClickListener(event -> fitView());
        return button;
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
        simulationBadgeDot.addClassName("status-dot");
        badge.add(simulationBadgeDot, simulationBadgeText);
        return badge;
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

    private static void setClass(Component component, String className, boolean enabled) {
        if (enabled) {
            component.addClassName(className);
        } else {
            component.removeClassName(className);
        }
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String formatScale(double scale) {
        return String.format(Locale.US, "%.3f", scale);
    }

    private enum WorkspaceTool {
        SELECT("select", "Zaznacz", "Z"),
        WIRE("wire", "Przewód", "W"),
        PROBE("probe", "Sonda", "S"),
        DELETE("delete", "Usuń", "X");

        private final String key;
        private final String label;
        private final String shortLabel;

        WorkspaceTool(String key, String label, String shortLabel) {
            this.key = key;
            this.label = label;
            this.shortLabel = shortLabel;
        }

        public String key() {
            return key;
        }

        public String label() {
            return label;
        }

        public String shortLabel() {
            return shortLabel;
        }
    }

    private enum QuickComponent {
        RESISTOR("R", "Rezystor"),
        CAPACITOR("C", "Kondensator"),
        INDUCTOR("L", "Cewka"),
        VOLTAGE("V", "Źródło"),
        GROUND("0", "Masa"),
        DIODE("D", "Dioda"),
        OPAMP("OP", "Wzmacniacz");

        private final String glyph;
        private final String label;

        QuickComponent(String glyph, String label) {
            this.glyph = glyph;
            this.label = label;
        }

        public String glyph() {
            return glyph;
        }

        public String label() {
            return label;
        }

        public boolean matches(String filter) {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            String normalizedGlyph = glyph.toLowerCase(Locale.ROOT);
            return normalizedLabel.contains(filter) || normalizedGlyph.contains(filter);
        }
    }
}
