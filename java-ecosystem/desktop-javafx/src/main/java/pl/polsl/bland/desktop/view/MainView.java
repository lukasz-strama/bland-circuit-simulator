package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import pl.polsl.bland.desktop.service.WorkspaceService;

import java.util.*;

public class MainView extends BorderPane {

    private final WorkspaceService workspace = new WorkspaceService();
    private final Map<String, WorkspaceService.WorkspaceElement> elements = new LinkedHashMap<>();
    private final Map<String, WorkspaceService.WorkspaceWire> wires = new LinkedHashMap<>();

    private final List<WorkspaceSnapshot> history = new ArrayList<>();
    private int historyIndex = -1;

    private final Canvas canvas = new Canvas(1280, 860);
    private final Tooltip hoverTooltip = new Tooltip();
    private final GraphicsContext gc = canvas.getGraphicsContext2D();

    private final CanvasRenderer renderer;
    private final EditorController controller;
    private final PropertiesPanel propertiesPanel;

    private double zoom = 1.0;
    private final Label lblZoom = new Label("100%");

    private final ToggleGroup toolGroup = new ToggleGroup();
    private final Map<WorkspaceTool, ToggleButton> toolButtons = new EnumMap<>(WorkspaceTool.class);

    private final Map<QuickComponent, Button> componentButtons = new LinkedHashMap<>();
    private final TextField componentSearch = new TextField();
    private QuickComponent activeComponent = null;

    public boolean draggingElement = false;
    public boolean dragHappened = false;

    private final Label statusTool = new Label("Zaznacz");
    private final Label statusMessage = new Label("Gotowy.");
    private final Label statusZoom = new Label("100%");

    public MainView() {

        setTop(new VBox(createMenuBar(), createToolBar(), createComponentBar()));
        setLeft(createToolRail());
        ScrollPane sp = buildCanvasPane();
        setCenter(sp);

        propertiesPanel = new PropertiesPanel(elements, this::refreshAndRecord, workspace);
        setRight(propertiesPanel);
        setBottom(createStatusBar());

        renderer = new CanvasRenderer(workspace);
        controller = new EditorController(
                workspace, elements, wires,
                canvas,
                this::refreshAndRecord,
                el -> propertiesPanel.showElement(el),
                this
        );

        canvas.addEventHandler(MouseEvent.MOUSE_CLICKED, e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                handleCanvasClick(e.getX(), e.getY());
            }
        });

        canvas.addEventHandler(MouseEvent.MOUSE_MOVED, e -> {
            double worldX = e.getX() / zoom;
            double worldY = e.getY() / zoom;

            var el = controller.findElementAt(worldX, worldY);

            if (el != null) {
                hoverTooltip.setText(
                        "Typ: " + el.type() + "\n" +
                        "Nazwa: " + el.id() + "\n" +
                        "Wartość: " + el.value() 
                );
                Tooltip.install(canvas, hoverTooltip);
            } else {
                Tooltip.uninstall(canvas, hoverTooltip);
            }
        });



        elements.putAll(workspace.createInitialWorkspace());
        wires.putAll(workspace.createInitialWires());

        recordHistory();
        setActiveTool(WorkspaceTool.SELECT);
        refresh();
    }

    private ScrollPane buildCanvasPane() {
    ScrollPane sp = new ScrollPane(canvas);
    sp.setPannable(false);

    final double[] panOrigin = {0, 0};
    final double[] panScroll = {0, 0};

    canvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
        if (e.getButton() == MouseButton.SECONDARY) {
            panOrigin[0] = e.getSceneX();
            panOrigin[1] = e.getSceneY();
            panScroll[0] = sp.getHvalue();
            panScroll[1] = sp.getVvalue();
        }
    });

    canvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
        if (e.isSecondaryButtonDown() && !draggingElement) {
            double dx = (panOrigin[0] - e.getSceneX()) / canvas.getWidth();
            double dy = (panOrigin[1] - e.getSceneY()) / canvas.getHeight();
            sp.setHvalue(panScroll[0] + dx);
            sp.setVvalue(panScroll[1] + dy);
        }
    });

        return sp;
    }
    

    private MenuBar createMenuBar() {
        MenuItem miNew = new MenuItem("Nowy projekt");
        MenuItem miExit = new MenuItem("Zakończ");
        miNew.setOnAction(e -> resetWorkspace());
        miExit.setOnAction(e -> System.exit(0));

        Menu menuFile = new Menu("Plik", null, miNew, new SeparatorMenuItem(), miExit);

        MenuItem miUndo = new MenuItem("Cofnij\tCtrl+Z");
        MenuItem miRedo = new MenuItem("Ponów\tCtrl+Y");
        miUndo.setOnAction(e -> undo());
        miRedo.setOnAction(e -> redo());
        Menu menuEdit = new Menu("Edycja", null, miUndo, miRedo);

        MenuItem miZoomIn = new MenuItem("Powiększ");
        MenuItem miZoomOut = new MenuItem("Pomniejsz");
        MenuItem miFit = new MenuItem("Dopasuj widok");
        miZoomIn.setOnAction(e -> setZoom(zoom + 0.1));
        miZoomOut.setOnAction(e -> setZoom(zoom - 0.1));
        miFit.setOnAction(e -> setZoom(1.0));
        Menu menuView = new Menu("Widok", null, miZoomIn, miZoomOut, miFit);

        Menu menuInsert = new Menu("Wstaw");
        for (QuickComponent qc : QuickComponent.values()) {
            MenuItem mi = new MenuItem(qc.label() + " [" + qc.glyph() + "]");
            mi.setOnAction(e -> activateComponentPlacement(qc));
            menuInsert.getItems().add(mi);
        }

        MenuItem miAbout = new MenuItem("O programie…");
        miAbout.setOnAction(e -> new Alert(Alert.AlertType.INFORMATION,
                "Bland Circuit Simulator\nLPM – zaznaczanie/przeciąganie\nPPM – panoramowanie",
                ButtonType.OK).showAndWait());
        Menu menuHelp = new Menu("Pomoc", null, miAbout);

        return new MenuBar(menuFile, menuEdit, menuView, menuInsert, menuHelp);
    }

    private ToolBar createToolBar() {
        Button btnNew = new Button("Nowy");
        Button btnSave = new Button("Zapisz");
        Button btnUndo = new Button("↩ Cofnij");
        Button btnRedo = new Button("↪ Ponów");

        btnNew.setOnAction(e -> resetWorkspace());
        btnUndo.setOnAction(e -> undo());
        btnRedo.setOnAction(e -> redo());

        Button btnZoomOut = new Button("−");
        Button btnZoomIn = new Button("+");
        Button btnZoom100 = new Button("100%");
        btnZoomOut.setOnAction(e -> setZoom(zoom - 0.1));
        btnZoomIn.setOnAction(e -> setZoom(zoom + 0.1));
        btnZoom100.setOnAction(e -> setZoom(1.0));

        lblZoom.setMinWidth(48);
        lblZoom.setAlignment(Pos.CENTER);

        ComboBox<WireRoutingMode> routingCombo = new ComboBox<>();
        routingCombo.getItems().addAll(WireRoutingMode.values());
        routingCombo.setValue(WireRoutingMode.STRAIGHT);

        return new ToolBar(
                btnNew, btnSave,
                new Separator(),
                btnUndo, btnRedo,
                new Separator(),
                new Label("Routing:"), routingCombo,
                new Separator(),
                btnZoomOut, lblZoom, btnZoomIn, btnZoom100
        );
    }

    private HBox createComponentBar() {
        HBox bar = new HBox(6);
        bar.setPadding(new Insets(4, 8, 4, 8));
        bar.setAlignment(Pos.CENTER_LEFT);

        bar.getChildren().add(new Label("Biblioteka:"));

        for (QuickComponent qc : QuickComponent.values()) {
            Button btn = new Button(qc.glyph() + " " + qc.label());
            btn.setOnAction(e -> activateComponentPlacement(qc));
            componentButtons.put(qc, btn);
            bar.getChildren().add(btn);
        }

        bar.getChildren().add(new Separator(Orientation.VERTICAL));

        componentSearch.setPromptText("Szukaj...");
        componentSearch.textProperty().addListener((obs, o, n) -> applyComponentFilter(n));
        bar.getChildren().add(componentSearch);

        return bar;
    }

    private VBox createToolRail() {
        VBox rail = new VBox(4);
        rail.setPadding(new Insets(8));

        for (WorkspaceTool tool : new WorkspaceTool[]{
                WorkspaceTool.SELECT,
                WorkspaceTool.WIRE,
                WorkspaceTool.DELETE
        }) {
            ToggleButton btn = new ToggleButton(tool.shortLabel());
            btn.setToggleGroup(toolGroup);
            btn.setMaxWidth(Double.MAX_VALUE);
            btn.setOnAction(e -> setActiveTool(tool));
            toolButtons.put(tool, btn);
            rail.getChildren().add(btn);
        }

        return rail;
    }

    private HBox createStatusBar() {
        HBox bar = new HBox();
        bar.setPadding(new Insets(4));
        bar.setAlignment(Pos.CENTER_LEFT);

        bar.getChildren().addAll(
                new Label("Narzędzie: "), statusTool,
                new Separator(Orientation.VERTICAL),
                new Label("Komunikat: "), statusMessage,
                new Separator(Orientation.VERTICAL),
                new Label("Zoom: "), statusZoom
        );

        return bar;
    }

    private void resetWorkspace() {
        elements.clear();
        elements.putAll(workspace.createInitialWorkspace());
        wires.clear();
        wires.putAll(workspace.createInitialWires());
        recordHistory();
        refresh();
    }

    private void refresh() {
        gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight());
        renderer.render(gc, elements, wires.values(), null, zoom, null, null);
    }

    private void refreshAndRecord() {
        recordHistory();
        refresh();
    }

    private void setZoom(double value) {
        zoom = Math.max(0.5, Math.min(2.0, value));
        lblZoom.setText((int) (zoom * 100) + "%");
        statusZoom.setText(lblZoom.getText());
        refresh();
    }

    private void setActiveTool(WorkspaceTool tool) {
        activeComponent = null; // 🔥 wyłącz tryb wstawiania
        controller.setTool(tool);
        ToggleButton btn = toolButtons.get(tool);
        if (btn != null) btn.setSelected(true);
        statusTool.setText(tool.label());
        statusMessage.setText("Aktywne narzędzie: " + tool.label() + ".");
    }

    private void activateComponentPlacement(QuickComponent qc) {
        activeComponent = qc;

        componentButtons.forEach((k, btn) -> btn.setStyle(""));
        componentButtons.get(qc).setStyle("-fx-border-color: #0078d4; -fx-border-width: 2;");
        statusMessage.setText("Tryb wstawiania: " + qc.label() + ". Kliknij LPM na arkuszu.");
    }

    private void applyComponentFilter(String filter) {
        String f = filter == null ? "" : filter.trim().toLowerCase(Locale.ROOT);
        componentButtons.forEach((qc, btn) ->
                btn.setVisible(f.isBlank() || qc.matches(f)));
    }

    private void handleCanvasClick(double x, double y) {
        if (activeComponent == null) return;
        if (draggingElement) return;
        if (dragHappened) return; 
        if (controller.isClickOnElement(x, y)) return; 
        

        var el = workspace.createElement(activeComponent.type(), x / zoom, y / zoom);
        elements.put(el.id(), el);
        refreshAndRecord();
        statusMessage.setText("Dodano element: " + el.id() + ".");
    }

    private void recordHistory() {
        while (history.size() > historyIndex + 1)
            history.remove(history.size() - 1);

        history.add(new WorkspaceSnapshot(
                new LinkedHashMap<>(elements),
                new LinkedHashMap<>(wires)
        ));
        historyIndex = history.size() - 1;
    }

    private void undo() {
        if (historyIndex <= 0) return;
        historyIndex--;
        restoreSnapshot(history.get(historyIndex));
    }

    private void redo() {
        if (historyIndex >= history.size() - 1) return;
        historyIndex++;
        restoreSnapshot(history.get(historyIndex));
    }

    private void restoreSnapshot(WorkspaceSnapshot snap) {
        elements.clear();
        elements.putAll(snap.elements());
        wires.clear();
        wires.putAll(snap.wires());
        refresh();
    }




    private record WorkspaceSnapshot(
            Map<String, WorkspaceService.WorkspaceElement> elements,
            Map<String, WorkspaceService.WorkspaceWire> wires
    ) {}

    public enum QuickComponent {
        RESISTOR("R", "Rezystor", WorkspaceService.ElementType.RESISTOR),
        CAPACITOR("C", "Kondensator", WorkspaceService.ElementType.CAPACITOR),
        INDUCTOR("L", "Cewka", WorkspaceService.ElementType.INDUCTOR),
        VOLTAGE("V", "Źródło napięcia", WorkspaceService.ElementType.VOLTAGE),
        CURRENT("I", "Źródło prądu", WorkspaceService.ElementType.CURRENT),
        GROUND("0", "Masa", WorkspaceService.ElementType.GROUND);

        private final String glyph;
        private final String label;
        private final WorkspaceService.ElementType type;

        QuickComponent(String glyph, String label, WorkspaceService.ElementType type) {
            this.glyph = glyph;
            this.label = label;
            this.type = type;
        }

        public String glyph() { return glyph; }
        public String label() { return label; }
        public WorkspaceService.ElementType type() { return type; }

        public boolean matches(String filter) {
            return label.toLowerCase(Locale.ROOT).contains(filter)
                    || glyph.toLowerCase(Locale.ROOT).contains(filter);
        }
    }

    public enum WireRoutingMode {
        STRAIGHT("Prosta"),
        ORTHOGONAL("Ortogonalna");

        private final String label;
        WireRoutingMode(String label) { this.label = label; }
        public String label() { return label; }
    }
}
