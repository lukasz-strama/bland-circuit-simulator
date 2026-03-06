package pl.polsl.bland.desktop.view;

import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public class MainView extends BorderPane {

    private final Canvas schematicCanvas;

    public MainView() {
        MenuBar menuBar = createMenuBar();
        setTop(menuBar);

        schematicCanvas = new Canvas(800, 600);
        StackPane canvasHolder = new StackPane(schematicCanvas);
        setCenter(canvasHolder);

        Label statusLabel = new Label("Ready");
        setBottom(statusLabel);
    }

    private MenuBar createMenuBar() {
        Menu fileMenu = new Menu("File");
        fileMenu.getItems().addAll(
                new MenuItem("New"),
                new MenuItem("Open"),
                new MenuItem("Save"));

        Menu simulationMenu = new Menu("Simulation");
        simulationMenu.getItems().addAll(
                new MenuItem("Run DC Analysis"),
                new MenuItem("Run Transient Analysis"));

        return new MenuBar(fileMenu, simulationMenu);
    }

    public Canvas getSchematicCanvas() {
        return schematicCanvas;
    }
}
