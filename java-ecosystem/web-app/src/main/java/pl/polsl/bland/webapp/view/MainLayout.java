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
    private static final double MOVE_STEP = 16;

    private final WorkspaceMockService workspaceMockService;
    private final PropertiesWindow propertiesWindow;
    private final ResultsWindow resultsWindow;
    private final SchematicPreview schematicPreview;
    private final LinkedHashMap<String, WorkspaceMockService.WorkspaceElement> workspaceElements = new LinkedHashMap<>();
    private final LinkedHashMap<String, WorkspaceMockService.WorkspaceWire> workspaceWires = new LinkedHashMap<>();
    private final Div workspacePanel = new Div();
    private final Span activeSymbolReadout = createWideReadout("");
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
    private final TextField componentSearch = new TextField();
    private final TextField elementValueField = new TextField();
    private final TextField netNameField = new TextField();
    private final Map<WorkspaceTool, Span> railButtons = new EnumMap<>(WorkspaceTool.class);
    private final Map<QuickComponent, Div> quickComponentButtons = new LinkedHashMap<>();
    private final LinkedHashMap<String, String> netAliases = new LinkedHashMap<>();

    private String analysisLabel = ANALYSIS_TRANSIENT;
    private WorkspaceTool activeTool = WorkspaceTool.SELECT;
    private QuickComponent activeComponent = QuickComponent.RESISTOR;
    private String selectedElementId;
    private String selectedWireId;
    private String selectedNetKey;
    private WorkspaceMockService.PinRef pendingWireStart;
    private WorkspaceMockService.WireEndpoint pendingWireEndpoint;
    private WorkspaceMockService.NetTopology workspaceNetTopology = WorkspaceMockService.NetTopology.empty();
    private double zoom = DEFAULT_ZOOM;
    private boolean simulationReady;
    private boolean suppressAnalysisEvents;

    public MainLayout(WorkspaceMockService workspaceMockService) {
        this.workspaceMockService = workspaceMockService;
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
        configureComponentSearch();
        configureElementValueField();
        configureNetNameField();
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

    private void configureElementValueField() {
        elementValueField.setPlaceholder("Wartość elementu");
        elementValueField.addClassName("element-value-field");
        elementValueField.setClearButtonVisible(true);
    }

    private void configureNetNameField() {
        netNameField.setPlaceholder("Nazwa netu");
        netNameField.addClassName("net-name-field");
        netNameField.setClearButtonVisible(true);
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
        simulationReady = false;
        resultsWindow.setVisible(false);
        propertiesWindow.setVisible(true);
        componentSearch.clear();
        setAnalysis(ANALYSIS_TRANSIENT, true, true);
        setActiveTool(WorkspaceTool.SELECT, true);
        setActiveComponent(QuickComponent.RESISTOR, true);
        applyComponentFilter("");
        setZoom(DEFAULT_ZOOM, true);
        selectedElementId = workspaceMockService.firstElement(workspaceElements)
                .map(WorkspaceMockService.WorkspaceElement::id)
                .orElse(null);
        refreshWorkspaceState();
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
                    "Brak aktywnego elementu. Zaznacz komponent, aby zobaczyć mockowane wyniki.");
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
            propertiesWindow.update(details, simulationReady);
            resultsWindow.update(details, analysisLabel, simulationReady);
            schematicPreview.setSelectedElement(selectedElementId);
        });
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
            statusSimulationValue.setText("Brak uruchomienia");
        }
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

        workspaceElements.put(selectedElementId, workspaceMockService.updateElementValue(element, candidate));
        refreshWorkspaceState();
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
        statusMessageValue.setText("Przywrócono domyślną wartość dla " + selectedElementId + ": " + defaultValue + ".");
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
            return;
        }

        WorkspaceMockService.WorkspaceElement element = workspaceElements.get(selectedElementId);
        if (element == null) {
            selectedElementId = null;
            selectedElementReadout.setText("brak");
            if (!elementValueField.isEmpty()) {
                elementValueField.clear();
            }
            return;
        }

        selectedElementReadout.setText(element.id());
        if (!element.value().equals(elementValueField.getValue())) {
            elementValueField.setValue(element.value());
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

        Span renameNet = createAction("Nazwij", "mini-button");
        renameNet.addClickListener(event -> applySelectedNetName());

        Span resetNetName = createAction("Auto", "mini-button");
        resetNetName.addClickListener(event -> resetSelectedNetName());

        Span applyElementValue = createAction("Zastosuj", "mini-button");
        applyElementValue.addClickListener(event -> applySelectedElementValue());

        Span resetElementValue = createAction("Domyślna", "mini-button");
        resetElementValue.addClickListener(event -> resetSelectedElementValue());

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
                createLabel("Element"),
                selectedElementReadout,
                elementValueField,
                applyElementValue,
                resetElementValue));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createLabel("Net"), selectedNetReadout, netNameField, renameNet, resetNetName));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(
                createLabel("Przewód"),
                selectedWireReadout,
                wireEditModeReadout,
                rewireStart,
                rewireEnd,
                cancelRewire));

        toolbar.add(separator());
        toolbar.add(buildToolbarGroup(createLabel("Przesuń"), moveLeft, moveUp, moveDown, moveRight));

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
            statusMessageValue.setText("Filtruj bibliotekę szybką albo kliknij symbol, aby aktywować tryb wstawiania.");
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

    private enum WorkspaceTool {
        SELECT("select", "Zaznacz", "Z", null),
        WIRE("wire", "Przewód", "W", null),
        PROBE("probe", "Sonda", "S", null),
        DELETE("delete", "Usuń", "X", null),
        PLACE_RESISTOR("resistor", "Wstaw: Rezystor", "", WorkspaceMockService.ElementType.RESISTOR),
        PLACE_CAPACITOR("capacitor", "Wstaw: Kondensator", "", WorkspaceMockService.ElementType.CAPACITOR),
        PLACE_INDUCTOR("inductor", "Wstaw: Cewka", "", WorkspaceMockService.ElementType.INDUCTOR),
        PLACE_VOLTAGE("voltage", "Wstaw: Źródło", "", WorkspaceMockService.ElementType.VOLTAGE),
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
        VOLTAGE("V", "Źródło", WorkspaceMockService.ElementType.VOLTAGE),
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
