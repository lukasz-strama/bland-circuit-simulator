package pl.polsl.bland.webapp.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import pl.polsl.bland.models.CircuitSchematic;
import pl.polsl.bland.models.SimulationRequest;
import pl.polsl.bland.webapp.service.BackendClient;
import pl.polsl.bland.webapp.service.SimulationCsvService;
import pl.polsl.bland.webapp.service.WorkspaceMockService;
import pl.polsl.bland.webapp.service.WorkspaceExportService;
import pl.polsl.bland.webapp.view.panel.PropertiesWindow;
import pl.polsl.bland.webapp.view.panel.ResultsWindow;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Route("")
@PageTitle("Bland Circuit Simulator")
public class MainLayout extends Div {
    private static final String PROJECT_FILE_NAME = "filtr_rlc_lab.asc";
    private static final String ANALYSIS_TRANSIENT = "Analiza przejściowa";
    private static final String ANALYSIS_DC = "Punkt pracy DC";
    private static final double DEFAULT_ZOOM = 0.92;
    private static final double MIN_ZOOM = 0.50;
    private static final double MAX_ZOOM = 1.60;
    private static final double ZOOM_STEP = 0.08;
    private static final double MOVE_STEP = 16;

    private final WorkspaceMockService workspaceMockService;
    private final BackendClient backendClient;
    private final WorkspaceExportService workspaceExportService;
    private final SimulationCsvService simulationCsvService;
    private final PropertiesWindow propertiesWindow;
    private final ResultsWindow resultsWindow;
    private final SchematicPreview schematicPreview;
    private final LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> workspaceElements = new LinkedHashMap<>();
    private final LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> workspaceWires = new LinkedHashMap<>();
    private final Div workspacePanel = new Div();
    private final Span activeSymbolReadout = createWideReadout("");
    private final Span undoButton = createAction("Cofnij", "tool-button");
    private final Span redoButton = createAction("Ponów", "tool-button");
    private final Span loadProjectButton = createAction("Wczytaj", "tool-button");
    private final Span toolbarToolValue = createReadout("");
    private final Span selectedElementReadout = createWideReadout("brak");
    private final Span selectedNetReadout = createWideReadout("brak");
    private final Span selectedWireReadout = createWideReadout("brak");
    private final Span wireEditModeReadout = createReadout("-");
    private final Span zoomReadout = createReadout("");
    private final Span simulationBadgeText = new Span();
    private final Span simulationBadgeDot = new Span();
    private final Span statusSimulationValue = new Span();
    private final Span statusMessageValue = new Span();
    private final Span statusToolValue = new Span();
    private final Span statusAnalysisValue = new Span();
    private final Span statusZoomValue = new Span();
    private final Select<String> analysisSelect = new Select<>();
    private final Select<String> sourceTypeSelect = new Select<>();
    private final TextField componentSearch = new TextField();
    private final TextField analysisTstopField = new TextField();
    private final TextField analysisTstepField = new TextField();
    private final TextField elementValueField = new TextField();
    private final TextField sourceFrequencyField = new TextField();
    private final TextField netNameField = new TextField();
    private final Map<WorkspaceTool, Span> railButtons = new EnumMap<>(WorkspaceTool.class);
    private final Map<QuickComponent, Div> quickComponentButtons = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> netAliases = new LinkedHashMap<>();
    private final List<WorkspaceSnapshot> workspaceHistory = new ArrayList<>();

    private String analysisLabel = ANALYSIS_TRANSIENT;
    private WorkspaceTool activeTool = WorkspaceTool.SELECT;
    private QuickComponent activeComponent = QuickComponent.RESISTOR;
    private String selectedElementId;
    private String selectedWireId;
    private String selectedNetKey;
    private WorkspaceMockService.PinRef pendingWireStart;
    private WorkspaceMockService.WireEndpoint pendingWireEndpoint;
    private WorkspaceMockService.NetTopology workspaceNetTopology = WorkspaceMockService.NetTopology.empty();
    private int historyIndex = -1;
    private ProjectSnapshot savedProjectSnapshot;
    private SimulationCsvService.ParsedSimulation latestSimulation;
    private String latestSimulationNetlist;
    private String latestSimulationMessage;
    private double zoom = DEFAULT_ZOOM;
    private double transientTstop;
    private double transientTstep;
    private boolean simulationReady;
    private boolean suppressAnalysisEvents;

    public MainLayout(
            WorkspaceMockService workspaceMockService,
            BackendClient backendClient,
            WorkspaceExportService workspaceExportService,
            SimulationCsvService simulationCsvService) {
        this.workspaceMockService = workspaceMockService;
        this.backendClient = backendClient;
        this.workspaceExportService = workspaceExportService;
        this.simulationCsvService = simulationCsvService;
        initializeTransientParameters();
        this.propertiesWindow = new PropertiesWindow();
        this.resultsWindow = new ResultsWindow();
        this.schematicPreview = new SchematicPreview(new SchematicPreview.InteractionHandler() {
            @Override
            public void onCanvasClick(double canvasX, double canvasY) {
                handleCanvasClick(canvasX / zoom, canvasY / zoom);
            }

            @Override
            public void onElementClick(String elementId) {
                handleElementClick(elementId);
            }

            @Override
            public void onPinClick(String elementId, String pinKey) {
                handlePinClick(elementId, pinKey);
            }

            @Override
            public void onNetClick(String netKey) {
                handleNetClick(netKey);
            }

            @Override
            public void onWireClick(String wireId) {
                handleWireClick(wireId);
            }
        });

        configureAnalysisSelect();
        configureTransientParameterFields();
        configureComponentSearch();
        configureElementValueField();
        configureSourceTypeSelect();
        configureSourceFrequencyField();
        configureNetNameField();
        configureHistoryButtons();
        configureProjectButtons();
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

    private void initializeTransientParameters() {
        transientTstop = workspaceExportService.defaultTransientTstop();
        transientTstep = workspaceExportService.defaultTransientTstep();
    }

    private void configureTransientParameterFields() {
        analysisTstopField.setPlaceholder("tstop [s]");
        analysisTstopField.addClassName("analysis-parameter-field");
        analysisTstopField.setClearButtonVisible(false);

        analysisTstepField.setPlaceholder("tstep [s]");
        analysisTstepField.addClassName("analysis-parameter-field");
        analysisTstepField.setClearButtonVisible(false);
    }

    private void configureComponentSearch() {
        componentSearch.setPlaceholder("Szukaj w bibliotece: rezystor, dioda, źródło prądu...");
        componentSearch.addClassName("component-search");
        componentSearch.setClearButtonVisible(true);
        componentSearch.setValueChangeMode(ValueChangeMode.EAGER);
        componentSearch.addValueChangeListener(event -> applyComponentFilter(event.getValue()));
    }

    private void configureElementValueField() {
        elementValueField.setPlaceholder("Wartość elementu");
        elementValueField.addClassName("element-value-field");
        elementValueField.setClearButtonVisible(true);
    }

    private void configureSourceTypeSelect() {
        sourceTypeSelect.setItems(WorkspaceMockService.SOURCE_TYPE_DC, WorkspaceMockService.SOURCE_TYPE_SINE);
        sourceTypeSelect.setItemLabelGenerator(type -> WorkspaceMockService.SOURCE_TYPE_SINE.equals(type) ? "sinus" : "dc");
        sourceTypeSelect.addClassName("source-type-select");
        sourceTypeSelect.addValueChangeListener(event -> updateSourceFrequencyFieldState(event.getValue(), true));
    }

    private void configureSourceFrequencyField() {
        sourceFrequencyField.setPlaceholder("Częstotliwość [Hz]");
        sourceFrequencyField.addClassName("source-frequency-field");
        sourceFrequencyField.setClearButtonVisible(true);
    }

    private void configureNetNameField() {
        netNameField.setPlaceholder("Nazwa netu");
        netNameField.addClassName("net-name-field");
        netNameField.setClearButtonVisible(true);
    }

    private void configureHistoryButtons() {
        undoButton.addClickListener(event -> undoWorkspaceChange());
        redoButton.addClickListener(event -> redoWorkspaceChange());
        updateHistoryControls();
    }

    private void configureProjectButtons() {
        loadProjectButton.addClickListener(event -> loadSavedProject());
        updateProjectControls();
    }

    private void configureFloatingWindows() {
        propertiesWindow.setCloseHandler(() -> {
            propertiesWindow.setVisible(false);
            statusMessageValue.setText("Ukryto panel właściwości.");
        });
        resultsWindow.setCloseHandler(() -> toggleResultsWindow(false));
    }

    private void resetWorkspace() {
        workspaceElements.clear();
        workspaceElements.putAll(workspaceMockService.createInitialWorkspace());
        workspaceWires.clear();
        workspaceWires.putAll(workspaceMockService.createInitialWires());
        netAliases.clear();
        selectedWireId = null;
        selectedNetKey = null;
        pendingWireStart = null;
        pendingWireEndpoint = null;
        clearSimulationState();
        resultsWindow.setVisible(false);
        propertiesWindow.setVisible(true);
        componentSearch.clear();
        resetTransientParameters(true);
        setAnalysis(ANALYSIS_TRANSIENT, true, true);
        setActiveTool(WorkspaceTool.SELECT, true);
        setActiveComponent(QuickComponent.RESISTOR, true);
        applyComponentFilter("");
        setZoom(DEFAULT_ZOOM, true);
        selectedElementId = workspaceMockService.firstElement(workspaceElements)
                .map(WorkspaceMockService.WorkspaceElement::id)
                .orElse(null);
        refreshWorkspaceState();
        initializeWorkspaceHistory();
        updateSimulationIndicators();
        statusMessageValue.setText("Nowy projekt mockowany został przygotowany.");
    }

    private void handleCanvasClick(double canvasX, double canvasY) {
        if (activeTool.isPlacementTool()) {
            addElementAt(activeTool.elementType(), canvasX, canvasY);
            return;
        }

        switch (activeTool) {
            case WIRE -> {
                if (pendingWireStart != null) {
                    clearPendingWire();
                    statusMessageValue.setText("Anulowano rozpoczęte rysowanie przewodu.");
                } else if (pendingWireEndpoint != null) {
                    clearPendingWireEndpoint();
                    statusMessageValue.setText("Anulowano przepinanie końcówki przewodu.");
                } else {
                    statusMessageValue.setText("Kliknij pierwszy pin, aby rozpocząć przewód.");
                }
            }
            case PROBE -> statusMessageValue.setText("Kliknij element, aby aktywować sondę i podejrzeć przebieg.");
            case DELETE -> statusMessageValue.setText("Kliknij element, aby usunąć go z arkusza.");
            default -> {
                clearSelectedWire();
                clearSelectedNet();
                statusMessageValue.setText("Kliknięto pusty obszar arkusza.");
            }
        }
    }

    private void handleElementClick(String elementId) {
        if (!workspaceElements.containsKey(elementId)) {
            return;
        }

        if (activeTool == WorkspaceTool.DELETE) {
            deleteElement(elementId);
            return;
        }

        showSelection(elementId);

        if (activeTool == WorkspaceTool.PROBE) {
            resultsWindow.setVisible(true);
            resultsWindow.setActiveTab(ResultsWindow.ResultTab.PLOT);
            statusMessageValue.setText("Sonda ustawiona na śladzie aktywnego elementu " + elementId + ".");
            return;
        }

        if (activeTool == WorkspaceTool.WIRE) {
            statusMessageValue.setText("Wybrano " + elementId + ". Kliknij pin elementu, aby rozpocząć albo zakończyć przewód.");
            return;
        }

        if (activeTool.isPlacementTool()) {
            statusMessageValue.setText("Wybrano " + elementId + ". Kliknij pusty obszar, aby dodać kolejny element typu "
                    + activeComponent.label() + ".");
            return;
        }

        statusMessageValue.setText("Zaznaczono " + elementId + ".");
    }

    private void addElementAt(WorkspaceMockService.ElementType type, double canvasX, double canvasY) {
        WorkspaceMockService.WorkspaceElement element =
                workspaceMockService.createElement(workspaceElements.values(), type, canvasX, canvasY);
        workspaceElements.put(element.id(), element);
        selectedElementId = element.id();
        propertiesWindow.setVisible(true);
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Dodano element " + element.id() + ". Kliknij arkusz, aby wstawić kolejny " + type.label() + ".");
    }

    private void deleteElement(String elementId) {
        WorkspaceMockService.WorkspaceElement removed = workspaceElements.remove(elementId);
        if (removed == null) {
            return;
        }

        int removedWireCount = removeWiresForElement(elementId);
        if (elementId.equals(selectedElementId)) {
            selectedElementId = workspaceMockService.firstElement(workspaceElements)
                    .map(WorkspaceMockService.WorkspaceElement::id)
                    .orElse(null);
        }

        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Usunięto element " + elementId + " z arkusza razem z " + removedWireCount + " przewodami.");
    }

    private void moveSelectedElement(double deltaX, double deltaY, String directionLabel) {
        if (selectedElementId == null) {
            statusMessageValue.setText("Najpierw zaznacz element do przesunięcia.");
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            statusMessageValue.setText("Nie znaleziono zaznaczonego elementu.");
            return;
        }

        workspaceElements.put(selectedElementId, workspaceMockService.moveElement(element, deltaX, deltaY));
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Przesunięto " + selectedElementId + " " + directionLabel + ".");
    }

    private void showSelection(String elementId) {
        selectedElementId = elementId;
        clearSelectedWire();
        clearSelectedNet();
        propertiesWindow.setVisible(true);
        schematicPreview.setSelectedElement(selectedElementId);
        refreshSelectionPanels();
        syncElementControls();
    }

    private void handlePinClick(String elementId, String pinKey) {
        if (!workspaceElements.containsKey(elementId)) {
            return;
        }

        if (activeTool == WorkspaceTool.DELETE) {
            deleteElement(elementId);
            return;
        }

        if (pendingWireEndpoint != null && selectedWireId != null) {
            reconnectSelectedWire(new WorkspaceMockService.PinRef(elementId, pinKey));
            return;
        }

        showSelection(elementId);
        WorkspaceMockService.PinRef clickedPin = new WorkspaceMockService.PinRef(elementId, pinKey);

        if (activeTool == WorkspaceTool.PROBE) {
            resultsWindow.setVisible(true);
            resultsWindow.setActiveTab(ResultsWindow.ResultTab.PLOT);
            statusMessageValue.setText("Sonda ustawiona na pinie " + elementId + ":" + pinKey + ".");
            return;
        }

        if (activeTool.isPlacementTool()) {
            statusMessageValue.setText("Wybrano pin " + pinKey + " elementu " + elementId
                    + ". Kliknij pusty obszar, aby dodać kolejny element typu " + activeComponent.label() + ".");
            return;
        }

        if (activeTool != WorkspaceTool.WIRE) {
            statusMessageValue.setText("Wybrano pin " + pinKey + " elementu " + elementId + ".");
            return;
        }

        if (pendingWireStart == null) {
            pendingWireStart = clickedPin;
            schematicPreview.setPendingWireStart(pendingWireStart);
            statusMessageValue.setText("Początek przewodu ustawiono na " + elementId + ":" + pinKey + ". Kliknij drugi pin.");
            return;
        }

        if (pendingWireStart.equals(clickedPin)) {
            clearPendingWire();
            statusMessageValue.setText("Anulowano rozpoczęty przewód z pinu " + elementId + ":" + pinKey + ".");
            return;
        }

        workspaceMockService.createWire(workspaceElements, workspaceWires.values(), pendingWireStart, clickedPin)
                .ifPresentOrElse(wire -> {
                    workspaceWires.put(wire.id(), wire);
                    clearPendingWire();
                    refreshWorkspaceState();
                    recordWorkspaceChange();
                    statusMessageValue.setText(
                            "Dodano przewód " + wire.id() + " między "
                                    + wire.start().elementId() + ":" + wire.start().pinKey()
                                    + " oraz "
                                    + wire.end().elementId() + ":" + wire.end().pinKey() + ".");
                }, () -> statusMessageValue.setText("Taki przewód już istnieje albo wskazane piny są nieprawidłowe."));
    }

    private void handleWireClick(String wireId) {
        if (!workspaceWires.containsKey(wireId)) {
            return;
        }

        if (activeTool == WorkspaceTool.DELETE) {
            workspaceWires.remove(wireId);
            refreshWorkspaceState();
            recordWorkspaceChange();
            statusMessageValue.setText("Usunięto przewód " + wireId + ".");
            return;
        }

        showWireSelection(wireId);
        if (activeTool == WorkspaceTool.WIRE) {
            statusMessageValue.setText("Wybrano przewód " + wireId + ". Użyj toolbaru, aby przepiąć jego początek albo koniec.");
        } else {
            statusMessageValue.setText("Wybrano przewód " + wireId + ".");
        }
    }

    private void handleNetClick(String netKey) {
        if (!workspaceNetTopology.hasNet(netKey)) {
            return;
        }

        clearSelectedWire();
        selectedNetKey = netKey;
        schematicPreview.setSelectedNet(selectedNetKey);
        syncNetControls();

        workspaceNetTopology.findNet(netKey).ifPresent(net -> {
            if (activeTool == WorkspaceTool.DELETE) {
                statusMessageValue.setText("Net " + net.name() + " jest pochodny. Usuń przewody, aby go rozdzielić.");
            } else {
                statusMessageValue.setText("Zaznaczono net " + net.name() + ".");
            }
        });
    }

    private void refreshWorkspaceState() {
        workspaceNetTopology = workspaceMockService.resolveNetTopology(workspaceElements, workspaceWires.values(), netAliases);
        netAliases.keySet().removeIf(key -> !workspaceNetTopology.hasNet(key));
        if (selectedNetKey != null && !workspaceNetTopology.hasNet(selectedNetKey)) {
            selectedNetKey = null;
        }
        if (selectedWireId != null && !workspaceWires.containsKey(selectedWireId)) {
            selectedWireId = null;
            pendingWireEndpoint = null;
        }
        renderWorkspace();
        refreshSelectionPanels();
        syncElementControls();
        syncNetControls();
        syncWireControls();
    }

    private void initializeWorkspaceHistory() {
        workspaceHistory.clear();
        workspaceHistory.add(captureWorkspaceSnapshot());
        historyIndex = 0;
        updateHistoryControls();
    }

    private void recordWorkspaceChange() {
        clearSimulationState();
        refreshSelectionPanels();
        updateSimulationIndicators();
        WorkspaceSnapshot snapshot = captureWorkspaceSnapshot();
        if (historyIndex >= 0 && historyIndex < workspaceHistory.size() && workspaceHistory.get(historyIndex).equals(snapshot)) {
            updateHistoryControls();
            return;
        }

        while (workspaceHistory.size() > historyIndex + 1) {
            workspaceHistory.remove(workspaceHistory.size() - 1);
        }
        workspaceHistory.add(snapshot);
        historyIndex = workspaceHistory.size() - 1;
        updateHistoryControls();
    }

    private WorkspaceSnapshot captureWorkspaceSnapshot() {
        return new WorkspaceSnapshot(
                new LinkedHashMap<>(workspaceElements),
                new LinkedHashMap<>(workspaceWires),
                new LinkedHashMap<>(netAliases),
                analysisLabel,
                transientTstop,
                transientTstep,
                selectedElementId,
                selectedWireId,
                selectedNetKey,
                simulationReady,
                resultsWindow.isVisible(),
                propertiesWindow.isVisible());
    }

    private void undoWorkspaceChange() {
        if (historyIndex <= 0) {
            statusMessageValue.setText("Brak wcześniejszych zmian do cofnięcia.");
            updateHistoryControls();
            return;
        }

        historyIndex--;
        restoreWorkspaceSnapshot(workspaceHistory.get(historyIndex));
        updateHistoryControls();
        statusMessageValue.setText("Cofnięto ostatnią zmianę w edytorze.");
    }

    private void redoWorkspaceChange() {
        if (historyIndex >= workspaceHistory.size() - 1) {
            statusMessageValue.setText("Brak kolejnych zmian do ponowienia.");
            updateHistoryControls();
            return;
        }

        historyIndex++;
        restoreWorkspaceSnapshot(workspaceHistory.get(historyIndex));
        updateHistoryControls();
        statusMessageValue.setText("Ponowiono ostatnio cofniętą zmianę.");
    }

    private void restoreWorkspaceSnapshot(WorkspaceSnapshot snapshot) {
        workspaceElements.clear();
        workspaceElements.putAll(snapshot.elements());
        workspaceWires.clear();
        workspaceWires.putAll(snapshot.wires());
        netAliases.clear();
        netAliases.putAll(snapshot.netAliases());
        setAnalysis(snapshot.analysisLabel(), true, true);
        setTransientParameters(snapshot.transientTstop(), snapshot.transientTstep());
        selectedElementId = snapshot.selectedElementId();
        selectedWireId = snapshot.selectedWireId();
        selectedNetKey = snapshot.selectedNetKey();
        pendingWireStart = null;
        pendingWireEndpoint = null;
        clearSimulationState();
        propertiesWindow.setVisible(snapshot.propertiesVisible());
        resultsWindow.setVisible(snapshot.resultsVisible());
        refreshWorkspaceState();
        updateSimulationIndicators();
    }

    private void updateHistoryControls() {
        setClass(undoButton, "is-disabled", historyIndex <= 0);
        setClass(redoButton, "is-disabled", historyIndex < 0 || historyIndex >= workspaceHistory.size() - 1);
    }

    private void saveCurrentProject() {
        savedProjectSnapshot = captureProjectSnapshot();
        updateProjectControls();
        statusMessageValue.setText("Zapisano projekt mockowany: "
                + workspaceElements.size() + " elementów / " + workspaceWires.size() + " przewodów.");
    }

    private void loadSavedProject() {
        if (savedProjectSnapshot == null) {
            statusMessageValue.setText("Brak zapisanego projektu do wczytania.");
            updateProjectControls();
            return;
        }

        restoreProjectSnapshot(savedProjectSnapshot);
        initializeWorkspaceHistory();
        updateProjectControls();
        statusMessageValue.setText("Wczytano ostatnio zapisany projekt mockowany.");
    }

    private ProjectSnapshot captureProjectSnapshot() {
        return new ProjectSnapshot(
                new LinkedHashMap<>(workspaceElements),
                new LinkedHashMap<>(workspaceWires),
                new LinkedHashMap<>(netAliases),
                analysisLabel,
                transientTstop,
                transientTstep,
                activeComponent,
                selectedElementId,
                selectedWireId,
                selectedNetKey,
                simulationReady,
                resultsWindow.isVisible(),
                propertiesWindow.isVisible(),
                zoom);
    }

    private void restoreProjectSnapshot(ProjectSnapshot snapshot) {
        workspaceElements.clear();
        workspaceElements.putAll(snapshot.elements());
        workspaceWires.clear();
        workspaceWires.putAll(snapshot.wires());
        netAliases.clear();
        netAliases.putAll(snapshot.netAliases());
        selectedElementId = snapshot.selectedElementId();
        selectedWireId = snapshot.selectedWireId();
        selectedNetKey = snapshot.selectedNetKey();
        pendingWireStart = null;
        pendingWireEndpoint = null;
        clearSimulationState();
        setAnalysis(snapshot.analysisLabel(), true, true);
        setTransientParameters(snapshot.transientTstop(), snapshot.transientTstep());
        setActiveComponent(snapshot.activeComponent(), true);
        setActiveTool(WorkspaceTool.SELECT, true);
        setZoom(snapshot.zoom(), true);
        propertiesWindow.setVisible(snapshot.propertiesVisible());
        resultsWindow.setVisible(snapshot.resultsVisible());
        refreshWorkspaceState();
        updateSimulationIndicators();
    }

    private void updateProjectControls() {
        setClass(loadProjectButton, "is-disabled", savedProjectSnapshot == null);
    }

    private void renderWorkspace() {
        schematicPreview.renderWorkspace(
                workspaceElements.values(),
                workspaceMockService.resolveWires(workspaceElements, workspaceWires.values()),
                workspaceNetTopology.nets());
        schematicPreview.setSelectedElement(selectedElementId);
        schematicPreview.setSelectedWire(selectedWireId);
        schematicPreview.setSelectedNet(selectedNetKey);
        schematicPreview.setPendingWireStart(pendingWireStart);
        schematicPreview.setFocusedPin(resolveFocusedPin());
    }

    private void refreshSelectionPanels() {
        if (selectedWireId != null) {
            WorkspaceMockService.WorkspaceWire wire = workspaceWires.get(selectedWireId);
            if (wire == null) {
                selectedWireId = null;
                refreshSelectionPanels();
                return;
            }

            workspaceMockService.describeWire(workspaceElements, workspaceNetTopology, wire).ifPresentOrElse(details -> {
                propertiesWindow.showWire(details);
                resultsWindow.showWire(details, analysisLabel);
                schematicPreview.setSelectedElement(null);
                schematicPreview.setSelectedWire(selectedWireId);
            }, () -> {
                selectedWireId = null;
                refreshSelectionPanels();
            });
            return;
        }

        if (selectedElementId == null) {
            propertiesWindow.clear("Brak zaznaczonego elementu. Dodaj nowy komponent albo wybierz istniejący.");
            resultsWindow.clear(analysisLabel, simulationReady,
                    simulationReady
                            ? "Zaznacz komponent, aby zobaczyć wyniki z ostatniej symulacji."
                            : "Brak aktywnego elementu. Zaznacz komponent, aby zobaczyć wyniki.");
            schematicPreview.setSelectedElement(null);
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            selectedElementId = null;
            refreshSelectionPanels();
            return;
        }

        workspaceMockService.describeElement(workspaceElements, workspaceNetTopology, element).ifPresent(details -> {
            if (latestSimulation != null && simulationReady) {
                showMeasuredResults(element, details);
            } else if (!simulationReady && latestSimulationMessage != null && latestSimulationNetlist != null) {
                propertiesWindow.update(details, false);
                resultsWindow.showSimulation(
                        "Aktywny element: " + details.id() + " / " + details.typeLabel(),
                        analysisLabel,
                        List.of(),
                        List.of(),
                        null,
                        latestSimulationNetlist,
                        List.of(latestSimulationMessage));
            } else {
                propertiesWindow.update(details, simulationReady);
                resultsWindow.update(details, analysisLabel, simulationReady);
            }
            schematicPreview.setSelectedElement(selectedElementId);
        });
    }

    private void showMeasuredResults(
            WorkspaceMockService.WorkspaceElement element,
            WorkspaceMockService.ElementDetails details) {
        MeasuredResults measuredResults = buildMeasuredResults(element);
        propertiesWindow.showMeasuredElement(
                details,
                measuredResults.primaryTraceName(),
                measuredResults.peak(),
                measuredResults.min(),
                measuredResults.rmsOrAverage(),
                measuredResults.timeOfPeak(),
                measuredResults.summaryNote());
        resultsWindow.showSimulation(
                "Aktywny element: " + details.id() + " / " + details.typeLabel(),
                analysisLabel,
                measuredResults.rows(),
                measuredResults.plotTraces(),
                measuredResults.preferredTraceKey(),
                latestSimulationNetlist == null ? details.netlist() : latestSimulationNetlist,
                measuredResults.logs());
    }

    private MeasuredResults buildMeasuredResults(WorkspaceMockService.WorkspaceElement element) {
        String nodeA = resolveSimulationNode(element, true);
        String nodeB = resolveSimulationNode(element, false);
        double[] timePoints = latestSimulation.timePoints();
        double[] voltageA = resolveVoltageSeries(nodeA);
        double[] voltageB = resolveVoltageSeries(nodeB);
        double[] voltageDelta = subtractSeries(voltageA, voltageB);
        double[] currentSeries = resolveCurrentSeries(element, timePoints);

        List<WorkspaceMockService.ResultRow> rows = new ArrayList<>();
        for (int index = 0; index < timePoints.length; index++) {
            String note = "V(" + nodeA + ")=" + formatMeasurement(voltageA[index], "V")
                    + ", V(" + nodeB + ")=" + formatMeasurement(voltageB[index], "V");
            rows.add(new WorkspaceMockService.ResultRow(
                    formatMeasurement(timePoints[index], "s"),
                    formatMeasurement(voltageDelta[index], "V"),
                    currentSeries == null ? "-" : formatMeasurement(currentSeries[index], "A"),
                    note));
        }

        String voltageTrace = "ΔV(" + nodeA + "," + nodeB + ")";
        String currentTrace = resolveCurrentTraceName(element, currentSeries);
        int peakIndex = indexOfMax(voltageDelta);
        String summaryNote = currentTrace == null
                ? "Statystyki pokazują napięcie różnicowe elementu."
                : "Statystyki pokazują napięcie różnicowe elementu, a prąd jest dostępny w tabeli wyników.";

        List<String> logs = new ArrayList<>();
        logs.add("Rzeczywista symulacja została uruchomiona przez backend i silnik C++.");
        logs.add("Liczba próbek CSV: " + rows.size() + ".");
        logs.add("Kolumny CSV: " + String.join(", ", latestSimulation.headers()) + ".");
        logs.add("Napięcie elementu liczono jako różnicę V(" + nodeA + ") - V(" + nodeB + ").");
        if (currentTrace == null) {
            logs.add("Dla tego elementu silnik nie zwrócił bezpośredniego śladu prądowego.");
        } else if (element.type() == WorkspaceMockService.ElementType.CURRENT) {
            logs.add("Prąd źródła prądowego wyliczono z ustawień źródła w web-app.");
        } else {
            logs.add("Użyto śladu prądowego " + currentTrace + " zwróconego przez silnik.");
        }

        List<ResultsWindow.PlotTrace> plotTraces = buildPlotTraces(
                element,
                nodeA,
                nodeB,
                timePoints,
                voltageA,
                voltageB,
                voltageDelta,
                currentSeries,
                rows.size());
        String preferredTraceKey = plotTraces.isEmpty() ? null : plotTraces.get(0).key();

        return new MeasuredResults(
                rows,
                logs,
                plotTraces,
                preferredTraceKey,
                voltageTrace,
                formatMeasurement(max(voltageDelta), "V"),
                formatMeasurement(min(voltageDelta), "V"),
                formatMeasurement(rmsOrAverage(voltageDelta), ANALYSIS_DC.equals(analysisLabel) ? "V avg" : "V rms"),
                formatMeasurement(timePoints[peakIndex], "s"),
                summaryNote);
    }

    private List<ResultsWindow.PlotTrace> buildPlotTraces(
            WorkspaceMockService.WorkspaceElement element,
            String nodeA,
            String nodeB,
            double[] timePoints,
            double[] voltageA,
            double[] voltageB,
            double[] voltageDelta,
            double[] currentSeries,
            int sampleCount) {
        LinkedHashMap<String, ResultsWindow.PlotTrace> traces = new LinkedHashMap<>();

        String deltaTraceLabel = "ΔV(" + nodeA + "," + nodeB + ")";
        addPlotTrace(
                traces,
                "delta:" + element.id(),
                deltaTraceLabel,
                "Aktywny ślad: " + deltaTraceLabel,
                "Próbek: " + sampleCount + ". Napięcie różnicowe aktywnego elementu.",
                timePoints,
                voltageDelta,
                "V");

        if (currentSeries != null) {
            String currentTrace = resolveCurrentTraceName(element, currentSeries);
            addPlotTrace(
                    traces,
                    latestSimulation.hasSeries("I(" + element.id() + ")") ? "I(" + element.id() + ")" : "calc:I(" + element.id() + ")",
                    currentTrace,
                    "Aktywny ślad: " + currentTrace,
                    latestSimulation.hasSeries("I(" + element.id() + ")")
                            ? "Próbek: " + sampleCount + ". Prąd elementu zwrócony przez silnik."
                            : "Próbek: " + sampleCount + ". Prąd źródła wyliczony po stronie web-app.",
                    timePoints,
                    currentSeries,
                    "A");
        }

        addRawVoltageTrace(traces, nodeA, timePoints, voltageA, sampleCount);
        if (!nodeB.equals(nodeA)) {
            addRawVoltageTrace(traces, nodeB, timePoints, voltageB, sampleCount);
        }

        for (String header : latestSimulation.headers()) {
            if ("time".equalsIgnoreCase(header)) {
                continue;
            }
            double[] series = latestSimulation.seriesOrNull(header);
            if (series == null) {
                continue;
            }
            addPlotTrace(
                    traces,
                    header,
                    header,
                    "Aktywny ślad: " + header,
                    "Próbek: " + sampleCount + ". Surowy ślad zwrócony przez backend/silnik.",
                    timePoints,
                    series,
                    resolvePlotUnit(header));
        }

        return List.copyOf(traces.values());
    }

    private void addRawVoltageTrace(
            LinkedHashMap<String, ResultsWindow.PlotTrace> traces,
            String nodeName,
            double[] timePoints,
            double[] voltageSeries,
            int sampleCount) {
        if ("0".equals(nodeName)) {
            return;
        }

        String traceName = "V(" + nodeName + ")";
        addPlotTrace(
                traces,
                traceName,
                traceName,
                "Aktywny ślad: " + traceName,
                "Próbek: " + sampleCount + ". Napięcie węzła aktywnego elementu.",
                timePoints,
                voltageSeries,
                "V");
    }

    private void addPlotTrace(
            LinkedHashMap<String, ResultsWindow.PlotTrace> traces,
            String key,
            String label,
            String title,
            String hint,
            double[] xValues,
            double[] yValues,
            String unit) {
        if (traces.containsKey(key) || xValues == null || yValues == null) {
            return;
        }
        traces.put(key, new ResultsWindow.PlotTrace(key, label, title, hint, xValues, yValues, unit));
    }

    private String resolvePlotUnit(String traceName) {
        return traceName.startsWith("I(") ? "A" : "V";
    }

    private String resolveSimulationNode(WorkspaceMockService.WorkspaceElement element, boolean primary) {
        WorkspaceMockService.PinRef pinRef = switch (element.type()) {
            case RESISTOR, INDUCTOR, CAPACITOR -> new WorkspaceMockService.PinRef(element.id(), primary ? "A" : "B");
            case VOLTAGE, CURRENT -> new WorkspaceMockService.PinRef(element.id(), primary ? "POS" : "NEG");
            case GROUND -> new WorkspaceMockService.PinRef(element.id(), "REF");
            case DIODE -> new WorkspaceMockService.PinRef(element.id(), primary ? "ANODE" : "CATHODE");
            case OPAMP -> new WorkspaceMockService.PinRef(element.id(), primary ? "IN+" : "IN-");
        };
        String netKey = workspaceNetTopology.netKey(pinRef);
        if (netKey != null && workspaceNetTopology.findNet(netKey)
                .filter(net -> net.members().stream().anyMatch(this::isGroundPin))
                .isPresent()) {
            return "0";
        }
        return workspaceNetTopology.netName(pinRef, pinRef.elementId() + "_" + pinRef.pinKey());
    }

    private boolean isGroundPin(WorkspaceMockService.PinRef pinRef) {
        return "REF".equals(pinRef.pinKey()) && pinRef.elementId().startsWith("GND");
    }

    private double[] resolveVoltageSeries(String nodeName) {
        if ("0".equals(nodeName)) {
            return latestSimulation.zeroSeries();
        }
        double[] series = latestSimulation.seriesOrNull("V(" + nodeName + ")");
        return series == null ? latestSimulation.zeroSeries() : series;
    }

    private double[] resolveCurrentSeries(WorkspaceMockService.WorkspaceElement element, double[] timePoints) {
        double[] engineSeries = latestSimulation.seriesOrNull("I(" + element.id() + ")");
        if (engineSeries != null) {
            return engineSeries;
        }
        if (element.type() != WorkspaceMockService.ElementType.CURRENT) {
            return null;
        }

        double amplitude;
        try {
            amplitude = Double.parseDouble(element.value());
        } catch (NumberFormatException exception) {
            return null;
        }

        double[] series = new double[timePoints.length];
        if (WorkspaceMockService.SOURCE_TYPE_SINE.equals(WorkspaceMockService.normalizeSourceType(element.sourceType()))
                && element.frequency() != null) {
            for (int index = 0; index < timePoints.length; index++) {
                series[index] = amplitude * Math.sin(2.0 * Math.PI * element.frequency() * timePoints[index]);
            }
            return series;
        }

        for (int index = 0; index < timePoints.length; index++) {
            series[index] = amplitude;
        }
        return series;
    }

    private String resolveCurrentTraceName(WorkspaceMockService.WorkspaceElement element, double[] currentSeries) {
        if (currentSeries == null) {
            return null;
        }
        return latestSimulation.hasSeries("I(" + element.id() + ")")
                ? "I(" + element.id() + ")"
                : "I(" + element.id() + ") [wyliczone]";
    }

    private double[] subtractSeries(double[] first, double[] second) {
        double[] result = new double[first.length];
        for (int index = 0; index < first.length; index++) {
            result[index] = first[index] - second[index];
        }
        return result;
    }

    private double maxAbs(double[] values) {
        double max = 0;
        for (double value : values) {
            max = Math.max(max, Math.abs(value));
        }
        return max;
    }

    private double max(double[] values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }

    private double min(double[] values) {
        double min = Double.POSITIVE_INFINITY;
        for (double value : values) {
            min = Math.min(min, value);
        }
        return min;
    }

    private int indexOfMax(double[] values) {
        int maxIndex = 0;
        for (int index = 1; index < values.length; index++) {
            if (values[index] > values[maxIndex]) {
                maxIndex = index;
            }
        }
        return maxIndex;
    }

    private double rmsOrAverage(double[] values) {
        if (values.length == 0) {
            return 0;
        }
        if (ANALYSIS_DC.equals(analysisLabel)) {
            double sum = 0;
            for (double value : values) {
                sum += value;
            }
            return sum / values.length;
        }
        double sumSquares = 0;
        for (double value : values) {
            sumSquares += value * value;
        }
        return Math.sqrt(sumSquares / values.length);
    }

    private String formatMeasurement(double value, String unit) {
        return String.format(Locale.US, "%.6g %s", value, unit);
    }

    private void showWireSelection(String wireId) {
        selectedWireId = wireId;
        selectedElementId = null;
        clearPendingWire();
        clearPendingWireEndpoint();
        clearSelectedNet();
        propertiesWindow.setVisible(true);
        syncWireControls();
        refreshSelectionPanels();
        syncElementControls();
    }

    private void reconnectSelectedWire(WorkspaceMockService.PinRef replacementPin) {
        WorkspaceMockService.WorkspaceWire wire = workspaceWires.get(selectedWireId);
        if (wire == null || pendingWireEndpoint == null) {
            clearPendingWireEndpoint();
            statusMessageValue.setText("Najpierw zaznacz przewód i wskaż przepinaną końcówkę.");
            return;
        }

        WorkspaceMockService.PinRef currentPin = pendingWireEndpoint == WorkspaceMockService.WireEndpoint.START
                ? wire.start()
                : wire.end();
        if (currentPin.equals(replacementPin)) {
            clearPendingWireEndpoint();
            statusMessageValue.setText("Ta końcówka przewodu już jest podłączona do " + replacementPin.elementId()
                    + ":" + replacementPin.pinKey() + ".");
            return;
        }

        workspaceMockService.reconnectWire(
                        workspaceElements,
                        workspaceWires.values(),
                        wire,
                        pendingWireEndpoint,
                        replacementPin)
                .ifPresentOrElse(updatedWire -> {
                    String endpointLabel = pendingWireEndpoint.label().toLowerCase(Locale.ROOT);
                    workspaceWires.put(updatedWire.id(), updatedWire);
                    clearPendingWireEndpoint();
                    refreshWorkspaceState();
                    recordWorkspaceChange();
                    statusMessageValue.setText("Przepięto " + endpointLabel + " przewodu " + updatedWire.id()
                            + " do " + replacementPin.elementId() + ":" + replacementPin.pinKey() + ".");
                }, () -> statusMessageValue.setText(
                        "Nie udało się przepiąć przewodu. Sprawdź, czy wskazany pin istnieje i nie tworzy duplikatu połączenia."));
    }

    private void runSimulation() {
        if (workspaceElements.isEmpty()) {
            statusMessageValue.setText("Dodaj elementy na arkusz, zanim uruchomisz symulację.");
            return;
        }

        try {
            var resolvedWires = workspaceMockService.resolveWires(workspaceElements, workspaceWires.values());
            CircuitSchematic schematic = workspaceExportService.exportSchematic(
                    PROJECT_FILE_NAME,
                    workspaceElements,
                    resolvedWires,
                    workspaceNetTopology);
            CircuitSchematic storedSchematic = backendClient.saveSchematic(schematic);
            SimulationRequest request = new SimulationRequest(
                    storedSchematic.id(),
                    resolveAnalysisType(),
                    resolveAnalysisParameters());
            latestSimulationNetlist = workspaceExportService.buildEngineNetlist(storedSchematic, request);
            latestSimulation = simulationCsvService.parse(backendClient.runSimulationCsv(request));
            simulationReady = true;
            latestSimulationMessage = null;
            resultsWindow.setVisible(true);
            resultsWindow.setActiveTab(ResultsWindow.ResultTab.SUMMARY);
            refreshSelectionPanels();
            updateSimulationIndicators();
            statusMessageValue.setText("Symulacja zakończona dla: " + analysisLabel + ".");
        } catch (IllegalArgumentException | IllegalStateException exception) {
            latestSimulation = null;
            simulationReady = false;
            latestSimulationMessage = exception.getMessage();
            resultsWindow.setVisible(true);
            resultsWindow.setActiveTab(ResultsWindow.ResultTab.LOGS);
            refreshSelectionPanels();
            updateSimulationIndicators();
            statusMessageValue.setText(exception.getMessage());
        }
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

    private SimulationRequest.AnalysisType resolveAnalysisType() {
        return ANALYSIS_DC.equals(analysisLabel)
                ? SimulationRequest.AnalysisType.DC
                : SimulationRequest.AnalysisType.TRANSIENT;
    }

    private Map<String, Double> resolveAnalysisParameters() {
        if (resolveAnalysisType() == SimulationRequest.AnalysisType.DC) {
            return Map.of();
        }
        return Map.of(
                "tstop", transientTstop,
                "tstep", transientTstep);
    }

    private void setAnalysis(String nextAnalysis, boolean silent, boolean syncSelect) {
        boolean analysisChanged = !nextAnalysis.equals(analysisLabel);
        analysisLabel = nextAnalysis;
        if (analysisChanged) {
            clearSimulationState();
        }
        if (syncSelect) {
            suppressAnalysisEvents = true;
            analysisSelect.setValue(nextAnalysis);
            suppressAnalysisEvents = false;
        }

        syncTransientControls();
        statusAnalysisValue.setText(nextAnalysis);
        refreshSelectionPanels();
        updateSimulationIndicators();

        if (analysisChanged && !silent) {
            recordWorkspaceChange();
        }

        if (!silent) {
            statusMessageValue.setText("Tryb analizy ustawiono na: " + nextAnalysis + ".");
        }
    }

    private void setActiveTool(WorkspaceTool tool, boolean silent) {
        if (tool != WorkspaceTool.WIRE) {
            clearPendingWire();
        }
        if (tool != WorkspaceTool.SELECT && tool != WorkspaceTool.WIRE) {
            clearPendingWireEndpoint();
        }
        activeTool = tool;
        workspacePanel.getElement().setAttribute("data-tool", tool.key());
        toolbarToolValue.setText(tool.label());
        statusToolValue.setText(tool.label());
        railButtons.forEach((key, button) -> setClass(button, "is-active", key == tool));

        if (!silent) {
            if (tool == WorkspaceTool.WIRE) {
                statusMessageValue.setText("Aktywne narzędzie: " + tool.label() + ". Kliknij pierwszy pin.");
            } else {
                statusMessageValue.setText("Aktywne narzędzie: " + tool.label() + ".");
            }
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

    private void activateComponentPlacement(QuickComponent component) {
        setActiveComponent(component, true);
        setActiveTool(WorkspaceTool.forElementType(component.type()), true);
        statusMessageValue.setText("Tryb wstawiania: " + component.label() + ". Kliknij arkusz, aby dodać element.");
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
            statusSimulationValue.setText(latestSimulationMessage == null ? "Brak uruchomienia" : latestSimulationMessage);
        }
    }

    private void clearSimulationState() {
        latestSimulation = null;
        latestSimulationNetlist = null;
        latestSimulationMessage = null;
        simulationReady = false;
    }

    private void applyTransientParameters() {
        if (resolveAnalysisType() != SimulationRequest.AnalysisType.TRANSIENT) {
            statusMessageValue.setText("Parametry tstop i tstep dotyczą tylko analizy przejściowej.");
            return;
        }

        double nextTstop;
        double nextTstep;
        try {
            nextTstop = parsePositiveAnalysisParameter(analysisTstopField.getValue(), "tstop");
            nextTstep = parsePositiveAnalysisParameter(analysisTstepField.getValue(), "tstep");
        } catch (IllegalArgumentException exception) {
            statusMessageValue.setText(exception.getMessage());
            return;
        }

        if (nextTstep >= nextTstop) {
            statusMessageValue.setText("Parametr tstep musi być mniejszy od tstop.");
            return;
        }

        if (Double.compare(transientTstop, nextTstop) == 0 && Double.compare(transientTstep, nextTstep) == 0) {
            statusMessageValue.setText("Parametry analizy przejściowej nie zmieniły się.");
            return;
        }

        setTransientParameters(nextTstop, nextTstep);
        recordWorkspaceChange();
        statusMessageValue.setText("Ustawiono analizę przejściową: tstop="
                + formatAnalysisParameter(transientTstop)
                + " s, tstep="
                + formatAnalysisParameter(transientTstep)
                + " s.");
    }

    private void resetTransientParameters(boolean silent) {
        setTransientParameters(
                workspaceExportService.defaultTransientTstop(),
                workspaceExportService.defaultTransientTstep());
        if (!silent) {
            recordWorkspaceChange();
            statusMessageValue.setText("Przywrócono domyślne parametry analizy przejściowej.");
        }
    }

    private void setTransientParameters(double tstop, double tstep) {
        transientTstop = tstop;
        transientTstep = tstep;
        syncTransientControls();
    }

    private void syncTransientControls() {
        boolean enabled = resolveAnalysisType() == SimulationRequest.AnalysisType.TRANSIENT;
        analysisTstopField.setEnabled(enabled);
        analysisTstepField.setEnabled(enabled);

        String tstopValue = formatAnalysisParameter(transientTstop);
        if (!tstopValue.equals(analysisTstopField.getValue())) {
            analysisTstopField.setValue(tstopValue);
        }

        String tstepValue = formatAnalysisParameter(transientTstep);
        if (!tstepValue.equals(analysisTstepField.getValue())) {
            analysisTstepField.setValue(tstepValue);
        }
    }

    private double parsePositiveAnalysisParameter(String rawValue, String label) {
        String candidate = rawValue == null ? "" : rawValue.trim().replace(',', '.');
        if (candidate.isBlank()) {
            throw new IllegalArgumentException("Wpisz parametr " + label + " w sekundach.");
        }

        double parsed;
        try {
            parsed = Double.parseDouble(candidate);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Parametr " + label + " musi być dodatnią liczbą.");
        }

        if (!Double.isFinite(parsed) || parsed <= 0) {
            throw new IllegalArgumentException("Parametr " + label + " musi być większy od zera.");
        }
        return parsed;
    }

    private void applySelectedElementValue() {
        if (selectedElementId == null || selectedWireId != null) {
            statusMessageValue.setText("Najpierw zaznacz element na arkuszu.");
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            statusMessageValue.setText("Nie znaleziono zaznaczonego elementu.");
            return;
        }

        String candidate = elementValueField.getValue() == null ? "" : elementValueField.getValue().trim();
        if (candidate.isBlank()) {
            statusMessageValue.setText("Wpisz nową wartość elementu albo użyj przycisku Domyślna.");
            return;
        }

        if (candidate.length() > 48) {
            statusMessageValue.setText("Wartość elementu jest zbyt długa. Skróć opis parametru.");
            return;
        }

        if (element.isSource()) {
            try {
                Double.parseDouble(candidate);
            } catch (NumberFormatException exception) {
                statusMessageValue.setText("Amplituda źródła musi być liczbą, np. 5.0 albo 0.02.");
                return;
            }
        }

        workspaceElements.put(selectedElementId, workspaceMockService.updateElementValue(element, candidate));
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Zmieniono wartość " + selectedElementId + " na: " + candidate + ".");
    }

    private void resetSelectedElementValue() {
        if (selectedElementId == null || selectedWireId != null) {
            statusMessageValue.setText("Najpierw zaznacz element na arkuszu.");
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            statusMessageValue.setText("Nie znaleziono zaznaczonego elementu.");
            return;
        }

        String defaultValue = workspaceMockService.defaultValue(element.type());
        if (defaultValue.equals(element.value())) {
            statusMessageValue.setText("Ten element już używa wartości domyślnej.");
            return;
        }

        workspaceElements.put(selectedElementId, workspaceMockService.updateElementValue(element, defaultValue));
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Przywrócono domyślną wartość dla " + selectedElementId + ": " + defaultValue + ".");
    }

    private void applySelectedSourceSettings() {
        if (selectedElementId == null || selectedWireId != null) {
            statusMessageValue.setText("Najpierw zaznacz element na arkuszu.");
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            statusMessageValue.setText("Nie znaleziono zaznaczonego elementu.");
            return;
        }

        if (!element.isSource()) {
            statusMessageValue.setText("Ustawienia typu i częstotliwości dotyczą tylko źródeł.");
            return;
        }

        String sourceType = sourceTypeSelect.getValue();
        if (sourceType == null || sourceType.isBlank()) {
            sourceType = workspaceMockService.defaultSourceType(element.type());
        }
        sourceType = WorkspaceMockService.normalizeSourceType(sourceType);

        Double frequency = null;
        if (WorkspaceMockService.SOURCE_TYPE_SINE.equals(sourceType)) {
            String candidate = sourceFrequencyField.getValue() == null ? "" : sourceFrequencyField.getValue().trim();
            if (candidate.isBlank()) {
                statusMessageValue.setText("Dla przebiegu sinus wpisz częstotliwość w hercach.");
                return;
            }

            try {
                frequency = Double.parseDouble(candidate);
            } catch (NumberFormatException exception) {
                statusMessageValue.setText("Częstotliwość musi być liczbą dodatnią w formacie dziesiętnym.");
                return;
            }

            if (frequency <= 0) {
                statusMessageValue.setText("Częstotliwość musi być większa od zera.");
                return;
            }
        }

        WorkspaceMockService.WorkspaceElement updated =
                workspaceMockService.updateSourceFrequency(
                        workspaceMockService.updateSourceType(element, sourceType),
                        frequency);
        if (updated.equals(element)) {
            statusMessageValue.setText("Ustawienia źródła nie zmieniły się.");
            return;
        }

        workspaceElements.put(selectedElementId, updated);
        refreshWorkspaceState();
        recordWorkspaceChange();
        String settingsLabel = WorkspaceMockService.formatSourceTypeLabel(updated.sourceType());
        if (updated.frequency() != null) {
            settingsLabel += " / " + WorkspaceMockService.formatFrequencyLabel(updated.frequency());
        }
        statusMessageValue.setText("Zmieniono ustawienia źródła " + selectedElementId + " na " + settingsLabel + ".");
    }

    private void resetSelectedSourceSettings() {
        if (selectedElementId == null || selectedWireId != null) {
            statusMessageValue.setText("Najpierw zaznacz element na arkuszu.");
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            statusMessageValue.setText("Nie znaleziono zaznaczonego elementu.");
            return;
        }

        if (!element.isSource()) {
            statusMessageValue.setText("Ten element nie używa ustawień źródła.");
            return;
        }

        String defaultSourceType = workspaceMockService.defaultSourceType(element.type());
        Double defaultFrequency = workspaceMockService.defaultFrequency(element.type(), defaultSourceType);
        WorkspaceMockService.WorkspaceElement updated =
                workspaceMockService.updateSourceFrequency(
                        workspaceMockService.updateSourceType(element, defaultSourceType),
                        defaultFrequency);
        if (updated.equals(element)) {
            statusMessageValue.setText("To źródło już używa ustawień domyślnych.");
            return;
        }

        workspaceElements.put(selectedElementId, updated);
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Przywrócono domyślne ustawienia źródła dla " + selectedElementId + ".");
    }

    private void startWireEndpointEdit(WorkspaceMockService.WireEndpoint endpoint) {
        if (selectedWireId == null) {
            statusMessageValue.setText("Najpierw zaznacz przewód na arkuszu.");
            return;
        }

        WorkspaceMockService.WorkspaceWire wire = workspaceWires.get(selectedWireId);
        if (wire == null) {
            selectedWireId = null;
            syncWireControls();
            statusMessageValue.setText("Zaznaczony przewód nie istnieje już na arkuszu.");
            return;
        }

        if (activeTool != WorkspaceTool.SELECT && activeTool != WorkspaceTool.WIRE) {
            setActiveTool(WorkspaceTool.SELECT, true);
        }

        clearPendingWire();
        pendingWireEndpoint = endpoint;
        syncWireControls();

        WorkspaceMockService.PinRef currentPin =
                endpoint == WorkspaceMockService.WireEndpoint.START ? wire.start() : wire.end();
        statusMessageValue.setText("Przepinanie: " + endpoint.label().toLowerCase(Locale.ROOT) + " przewodu "
                + wire.id() + ". Kliknij nowy pin zamiast " + currentPin.elementId() + ":" + currentPin.pinKey() + ".");
    }

    private void applySelectedNetName() {
        if (selectedNetKey == null) {
            statusMessageValue.setText("Najpierw zaznacz net na arkuszu.");
            return;
        }

        String candidate = netNameField.getValue() == null ? "" : netNameField.getValue().trim();
        if (candidate.isBlank()) {
            statusMessageValue.setText("Wpisz nazwę netu albo użyj przycisku Auto.");
            return;
        }

        if (!candidate.matches("0|[A-Za-z_][A-Za-z0-9_]*")) {
            statusMessageValue.setText("Nazwa netu może zawierać litery, cyfry i podkreślenia; nie może zaczynać się od cyfry.");
            return;
        }

        boolean duplicate = workspaceNetTopology.nets().stream()
                .anyMatch(net -> !net.key().equals(selectedNetKey) && net.name().equalsIgnoreCase(candidate));
        if (duplicate) {
            statusMessageValue.setText("Taka nazwa netu jest już używana.");
            return;
        }

        netAliases.put(selectedNetKey, candidate);
        refreshWorkspaceState();
        recordWorkspaceChange();
        statusMessageValue.setText("Nadano nazwę netu: " + candidate + ".");
    }

    private void resetSelectedNetName() {
        if (selectedNetKey == null) {
            statusMessageValue.setText("Najpierw zaznacz net na arkuszu.");
            return;
        }

        if (netAliases.remove(selectedNetKey) == null) {
            statusMessageValue.setText("Ten net już używa nazwy automatycznej.");
            return;
        }

        refreshWorkspaceState();
        recordWorkspaceChange();
        workspaceNetTopology.findNet(selectedNetKey).ifPresentOrElse(
                net -> statusMessageValue.setText("Przywrócono nazwę automatyczną netu: " + net.name() + "."),
                () -> statusMessageValue.setText("Przywrócono nazwę automatyczną netu."));
    }

    private void syncNetControls() {
        if (selectedNetKey == null) {
            selectedNetReadout.setText("brak");
            if (!netNameField.isEmpty()) {
                netNameField.clear();
            }
            schematicPreview.setSelectedNet(null);
            return;
        }

        workspaceNetTopology.findNet(selectedNetKey).ifPresentOrElse(net -> {
            selectedNetReadout.setText(net.name());
            if (!net.name().equals(netNameField.getValue())) {
                netNameField.setValue(net.name());
            }
            schematicPreview.setSelectedNet(selectedNetKey);
        }, () -> {
            selectedNetKey = null;
            selectedNetReadout.setText("brak");
            if (!netNameField.isEmpty()) {
                netNameField.clear();
            }
            schematicPreview.setSelectedNet(null);
        });
    }

    private void syncElementControls() {
        if (selectedElementId == null || selectedWireId != null) {
            selectedElementReadout.setText("brak");
            if (!elementValueField.isEmpty()) {
                elementValueField.clear();
            }
            syncSourceControls();
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            selectedElementId = null;
            selectedElementReadout.setText("brak");
            if (!elementValueField.isEmpty()) {
                elementValueField.clear();
            }
            syncSourceControls();
            return;
        }

        selectedElementReadout.setText(element.id());
        if (!element.value().equals(elementValueField.getValue())) {
            elementValueField.setValue(element.value());
        }
        syncSourceControls();
    }

    private void syncSourceControls() {
        if (selectedElementId == null || selectedWireId != null) {
            sourceTypeSelect.clear();
            sourceTypeSelect.setEnabled(false);
            if (!sourceFrequencyField.isEmpty()) {
                sourceFrequencyField.clear();
            }
            sourceFrequencyField.setEnabled(false);
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null || !element.isSource()) {
            sourceTypeSelect.clear();
            sourceTypeSelect.setEnabled(false);
            if (!sourceFrequencyField.isEmpty()) {
                sourceFrequencyField.clear();
            }
            sourceFrequencyField.setEnabled(false);
            return;
        }

        String normalizedType = WorkspaceMockService.normalizeSourceType(element.sourceType());
        sourceTypeSelect.setEnabled(true);
        if (!normalizedType.equals(sourceTypeSelect.getValue())) {
            sourceTypeSelect.setValue(normalizedType);
        }

        String frequencyValue = element.frequency() == null ? "" : formatFrequencyInput(element.frequency());
        if (!frequencyValue.equals(sourceFrequencyField.getValue())) {
            sourceFrequencyField.setValue(frequencyValue);
        }
        updateSourceFrequencyFieldState(normalizedType, false);
    }

    private void updateSourceFrequencyFieldState(String sourceType, boolean populateDefaultWhenEmpty) {
        String normalizedType = WorkspaceMockService.normalizeSourceType(sourceType);
        boolean sine = WorkspaceMockService.SOURCE_TYPE_SINE.equals(normalizedType);
        sourceFrequencyField.setEnabled(sourceTypeSelect.isEnabled() && sine);
        if (!sine) {
            if (!sourceFrequencyField.isEmpty()) {
                sourceFrequencyField.clear();
            }
            return;
        }

        if (!populateDefaultWhenEmpty || selectedElementId == null) {
            return;
        }

        if (!sourceFrequencyField.isEmpty()) {
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null || !element.isSource()) {
            return;
        }

        Double fallbackFrequency = element.frequency() != null
                ? element.frequency()
                : workspaceMockService.defaultFrequency(element.type(), normalizedType);
        if (fallbackFrequency != null) {
            sourceFrequencyField.setValue(formatFrequencyInput(fallbackFrequency));
        }
    }

    private void syncWireControls() {
        if (selectedWireId == null) {
            selectedWireReadout.setText("brak");
            wireEditModeReadout.setText("-");
            schematicPreview.setSelectedWire(null);
            schematicPreview.setFocusedPin(null);
            return;
        }

        WorkspaceMockService.WorkspaceWire wire = workspaceWires.get(selectedWireId);
        if (wire == null) {
            selectedWireId = null;
            pendingWireEndpoint = null;
            selectedWireReadout.setText("brak");
            wireEditModeReadout.setText("-");
            schematicPreview.setSelectedWire(null);
            schematicPreview.setFocusedPin(null);
            return;
        }

        selectedWireReadout.setText(wire.id());
        wireEditModeReadout.setText(pendingWireEndpoint == null ? "-" : pendingWireEndpoint.label());
        schematicPreview.setSelectedWire(selectedWireId);
        schematicPreview.setFocusedPin(resolveFocusedPin());
    }

    private int removeWiresForElement(String elementId) {
        int removed = 0;
        for (var iterator = workspaceWires.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, WorkspaceMockService.WorkspaceWire> entry = iterator.next();
            if (!workspaceMockService.isWireAttachedToElement(entry.getValue(), elementId)) {
                continue;
            }
            iterator.remove();
            removed++;
        }

        if (pendingWireStart != null && pendingWireStart.elementId().equals(elementId)) {
            clearPendingWire();
        }
        if (selectedWireId != null && !workspaceWires.containsKey(selectedWireId)) {
            clearSelectedWire();
        }
        return removed;
    }

    private void clearSelectedNet() {
        if (selectedNetKey == null) {
            return;
        }
        selectedNetKey = null;
        syncNetControls();
    }

    private void clearSelectedWire() {
        if (selectedWireId == null && pendingWireEndpoint == null) {
            return;
        }
        selectedWireId = null;
        clearPendingWireEndpoint();
        syncWireControls();
        syncElementControls();
    }

    private void clearPendingWire() {
        pendingWireStart = null;
        schematicPreview.setPendingWireStart(null);
    }

    private void clearPendingWireEndpoint() {
        pendingWireEndpoint = null;
        schematicPreview.setFocusedPin(null);
        if (selectedWireId != null) {
            syncWireControls();
        } else {
            wireEditModeReadout.setText("-");
        }
    }

    private WorkspaceMockService.PinRef resolveFocusedPin() {
        if (selectedWireId == null || pendingWireEndpoint == null) {
            return null;
        }

        WorkspaceMockService.WorkspaceWire wire = workspaceWires.get(selectedWireId);
        if (wire == null) {
            return null;
        }
        return pendingWireEndpoint == WorkspaceMockService.WireEndpoint.START ? wire.start() : wire.end();
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
        Div primaryRow = buildToolbarRow();
        Div secondaryRow = buildToolbarRow();

        Span newProject = createAction("Nowy", "tool-button");
        newProject.addClickListener(event -> resetWorkspace());

        Span saveProject = createAction("Zapisz", "tool-button");
        saveProject.addClickListener(event -> saveCurrentProject());

        Span simulate = createAction("Symuluj", "tool-button", "is-primary");
        simulate.addClickListener(event -> runSimulation());

        Span showResults = createAction("Pokaż wyniki", "tool-button");
        showResults.addClickListener(event -> toggleResultsWindow(true));

        Span applyTransientButton = createAction("Ustaw", "mini-button");
        applyTransientButton.addClickListener(event -> applyTransientParameters());

        Span resetTransientButton = createAction("Auto", "mini-button");
        resetTransientButton.addClickListener(event -> resetTransientParameters(false));

        Span renameNet = createAction("Nazwij", "mini-button");
        renameNet.addClickListener(event -> applySelectedNetName());

        Span resetNetName = createAction("Auto", "mini-button");
        resetNetName.addClickListener(event -> resetSelectedNetName());

        Span applyElementValue = createAction("Zastosuj", "mini-button");
        applyElementValue.addClickListener(event -> applySelectedElementValue());

        Span resetElementValue = createAction("Domyślna", "mini-button");
        resetElementValue.addClickListener(event -> resetSelectedElementValue());

        Span applySourceSettings = createAction("Ustaw", "mini-button");
        applySourceSettings.addClickListener(event -> applySelectedSourceSettings());

        Span resetSourceSettings = createAction("Auto", "mini-button");
        resetSourceSettings.addClickListener(event -> resetSelectedSourceSettings());

        Span rewireStart = createAction("Początek", "mini-button");
        rewireStart.addClickListener(event -> startWireEndpointEdit(WorkspaceMockService.WireEndpoint.START));

        Span rewireEnd = createAction("Koniec", "mini-button");
        rewireEnd.addClickListener(event -> startWireEndpointEdit(WorkspaceMockService.WireEndpoint.END));

        Span cancelRewire = createAction("Anuluj", "mini-button");
        cancelRewire.addClickListener(event -> {
            if (pendingWireEndpoint == null) {
                statusMessageValue.setText("Brak aktywnego przepinania końcówki przewodu.");
                return;
            }
            clearPendingWireEndpoint();
            statusMessageValue.setText("Anulowano przepinanie końcówki przewodu.");
        });

        Span zoomOut = createAction("-", "mini-button");
        zoomOut.addClickListener(event -> setZoom(zoom - ZOOM_STEP, false));

        Span zoomIn = createAction("+", "mini-button");
        zoomIn.addClickListener(event -> setZoom(zoom + ZOOM_STEP, false));

        Span zoom100 = createAction("100%", "mini-button");
        zoom100.addClickListener(event -> setZoom(1.0, false));

        Span fitView = createAction("Dopasuj", "mini-button");
        fitView.addClickListener(event -> fitView());

        Span moveLeft = createAction("←", "mini-button");
        moveLeft.addClickListener(event -> moveSelectedElement(-MOVE_STEP, 0, "w lewo"));

        Span moveUp = createAction("↑", "mini-button");
        moveUp.addClickListener(event -> moveSelectedElement(0, -MOVE_STEP, "w górę"));

        Span moveDown = createAction("↓", "mini-button");
        moveDown.addClickListener(event -> moveSelectedElement(0, MOVE_STEP, "w dół"));

        Span moveRight = createAction("→", "mini-button");
        moveRight.addClickListener(event -> moveSelectedElement(MOVE_STEP, 0, "w prawo"));

        Span help = createAction("?", "icon-button");
        help.addClickListener(event -> statusMessageValue.setText(
                "Zaznacz element, aby go przesuwać. Zaznacz przewód, aby przepiąć jego początek lub koniec."));

        primaryRow.add(buildToolbarGroup(
                newProject,
                undoButton,
                redoButton,
                saveProject,
                loadProjectButton,
                simulate,
                showResults));

        primaryRow.add(separator());
        primaryRow.add(buildToolbarGroup(
                createLabel("Analiza"),
                analysisSelect,
                analysisTstopField,
                analysisTstepField,
                applyTransientButton,
                resetTransientButton));

        primaryRow.add(separator());
        primaryRow.add(buildToolbarGroup(createLabel("Narzędzie"), toolbarToolValue));

        primaryRow.add(separator());
        primaryRow.add(buildToolbarGroup(
                createLabel("Element"),
                selectedElementReadout,
                elementValueField,
                applyElementValue,
                resetElementValue));

        secondaryRow.add(buildToolbarGroup(createLabel("Net"), selectedNetReadout, netNameField, renameNet, resetNetName));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(
                createLabel("Źródło"),
                sourceTypeSelect,
                sourceFrequencyField,
                applySourceSettings,
                resetSourceSettings));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(
                createLabel("Przewód"),
                selectedWireReadout,
                wireEditModeReadout,
                rewireStart,
                rewireEnd,
                cancelRewire));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(createLabel("Przesuń"), moveLeft, moveUp, moveDown, moveRight));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(
                zoomOut,
                zoomReadout,
                zoomIn,
                fitView,
                zoom100));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(buildStatusBadge()));

        secondaryRow.add(separator());
        secondaryRow.add(buildToolbarGroup(help));
        toolbar.add(primaryRow, secondaryRow);
        return toolbar;
    }

    private Div buildComponentBar() {
        Div componentBar = new Div();
        componentBar.addClassName("componentbar");
        Div primaryRow = buildComponentRow();
        Div secondaryRow = buildComponentRow();

        Span openLibrary = createAction("Komponent...", "tool-button");
        openLibrary.addClickListener(event -> {
            componentSearch.focus();
            statusMessageValue.setText("Filtruj bibliotekę szybką albo kliknij symbol, aby aktywować tryb wstawiania.");
        });

        primaryRow.add(buildComponentGroup(openLibrary, createLabel("Biblioteka")));
        primaryRow.add(buildComponentGroup(
                createQuickComponent(QuickComponent.RESISTOR),
                createQuickComponent(QuickComponent.CAPACITOR),
                createQuickComponent(QuickComponent.INDUCTOR),
                createQuickComponent(QuickComponent.VOLTAGE),
                createQuickComponent(QuickComponent.CURRENT),
                createQuickComponent(QuickComponent.GROUND),
                createQuickComponent(QuickComponent.DIODE),
                createQuickComponent(QuickComponent.OPAMP)));

        Span openLibraryDialog = createAction("Otwórz bibliotekę", "mini-button");
        openLibraryDialog.addClickListener(event -> statusMessageValue.setText("Pełna biblioteka pojawi się w kolejnym etapie prac."));

        Div fillGroup = buildComponentGroup(componentSearch, openLibraryDialog);
        fillGroup.addClassName("is-fill");
        secondaryRow.add(fillGroup);

        secondaryRow.add(buildComponentGroup(createLabel("Aktywny symbol"), activeSymbolReadout));
        componentBar.add(primaryRow, secondaryRow);
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

    private Div buildToolbarRow(Component... children) {
        Div row = new Div();
        row.addClassName("toolbar-row");
        row.add(children);
        return row;
    }

    private Div buildComponentGroup(Component... children) {
        Div group = new Div();
        group.addClassName("componentbar-group");
        group.add(children);
        return group;
    }

    private Div buildComponentRow(Component... children) {
        Div row = new Div();
        row.addClassName("componentbar-row");
        row.add(children);
        return row;
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
        button.addClickListener(event -> activateComponentPlacement(component));

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

    private static String formatFrequencyInput(double frequency) {
        if (Math.abs(frequency - Math.rint(frequency)) < 0.0000001) {
            return Long.toString(Math.round(frequency));
        }
        return Double.toString(frequency);
    }

    private static String formatAnalysisParameter(double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private record MeasuredResults(
            List<WorkspaceMockService.ResultRow> rows,
            List<String> logs,
            List<ResultsWindow.PlotTrace> plotTraces,
            String preferredTraceKey,
            String primaryTraceName,
            String peak,
            String min,
            String rmsOrAverage,
            String timeOfPeak,
            String summaryNote) {
    }

    private record WorkspaceSnapshot(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires,
            LinkedHashMap<String, String> netAliases,
            String analysisLabel,
            double transientTstop,
            double transientTstep,
            String selectedElementId,
            String selectedWireId,
            String selectedNetKey,
            boolean simulationReady,
            boolean resultsVisible,
            boolean propertiesVisible) {
    }

    private record ProjectSnapshot(
            LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> elements,
            LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> wires,
            LinkedHashMap<String, String> netAliases,
            String analysisLabel,
            double transientTstop,
            double transientTstep,
            QuickComponent activeComponent,
            String selectedElementId,
            String selectedWireId,
            String selectedNetKey,
            boolean simulationReady,
            boolean resultsVisible,
            boolean propertiesVisible,
            double zoom) {
    }

    private enum WorkspaceTool {
        SELECT("select", "Zaznacz", "Z", null),
        WIRE("wire", "Przewód", "W", null),
        PROBE("probe", "Sonda", "S", null),
        DELETE("delete", "Usuń", "X", null),
        PLACE_RESISTOR("resistor", "Wstaw: Rezystor", "", WorkspaceMockService.ElementType.RESISTOR),
        PLACE_CAPACITOR("capacitor", "Wstaw: Kondensator", "", WorkspaceMockService.ElementType.CAPACITOR),
        PLACE_INDUCTOR("inductor", "Wstaw: Cewka", "", WorkspaceMockService.ElementType.INDUCTOR),
        PLACE_VOLTAGE("voltage", "Wstaw: Źródło napięcia", "", WorkspaceMockService.ElementType.VOLTAGE),
        PLACE_CURRENT("current", "Wstaw: Źródło prądu", "", WorkspaceMockService.ElementType.CURRENT),
        PLACE_GROUND("ground", "Wstaw: Masa", "", WorkspaceMockService.ElementType.GROUND),
        PLACE_DIODE("diode", "Wstaw: Dioda", "", WorkspaceMockService.ElementType.DIODE),
        PLACE_OPAMP("opamp", "Wstaw: Wzmacniacz", "", WorkspaceMockService.ElementType.OPAMP);

        private final String key;
        private final String label;
        private final String shortLabel;
        private final WorkspaceMockService.ElementType elementType;

        WorkspaceTool(String key, String label, String shortLabel, WorkspaceMockService.ElementType elementType) {
            this.key = key;
            this.label = label;
            this.shortLabel = shortLabel;
            this.elementType = elementType;
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

        public WorkspaceMockService.ElementType elementType() {
            return elementType;
        }

        public boolean isPlacementTool() {
            return elementType != null;
        }

        public static WorkspaceTool forElementType(WorkspaceMockService.ElementType elementType) {
            return switch (elementType) {
                case RESISTOR -> PLACE_RESISTOR;
                case CAPACITOR -> PLACE_CAPACITOR;
                case INDUCTOR -> PLACE_INDUCTOR;
                case VOLTAGE -> PLACE_VOLTAGE;
                case CURRENT -> PLACE_CURRENT;
                case GROUND -> PLACE_GROUND;
                case DIODE -> PLACE_DIODE;
                case OPAMP -> PLACE_OPAMP;
            };
        }
    }

    private enum QuickComponent {
        RESISTOR("R", "Rezystor", WorkspaceMockService.ElementType.RESISTOR),
        CAPACITOR("C", "Kondensator", WorkspaceMockService.ElementType.CAPACITOR),
        INDUCTOR("L", "Cewka", WorkspaceMockService.ElementType.INDUCTOR),
        VOLTAGE("V", "Źródło napięcia", WorkspaceMockService.ElementType.VOLTAGE),
        CURRENT("I", "Źródło prądu", WorkspaceMockService.ElementType.CURRENT),
        GROUND("0", "Masa", WorkspaceMockService.ElementType.GROUND),
        DIODE("D", "Dioda", WorkspaceMockService.ElementType.DIODE),
        OPAMP("OP", "Wzmacniacz", WorkspaceMockService.ElementType.OPAMP);

        private final String glyph;
        private final String label;
        private final WorkspaceMockService.ElementType type;

        QuickComponent(String glyph, String label, WorkspaceMockService.ElementType type) {
            this.glyph = glyph;
            this.label = label;
            this.type = type;
        }

        public String glyph() {
            return glyph;
        }

        public String label() {
            return label;
        }

        public WorkspaceMockService.ElementType type() {
            return type;
        }

        public boolean matches(String filter) {
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            String normalizedGlyph = glyph.toLowerCase(Locale.ROOT);
            return normalizedLabel.contains(filter) || normalizedGlyph.contains(filter);
        }
    }
}
