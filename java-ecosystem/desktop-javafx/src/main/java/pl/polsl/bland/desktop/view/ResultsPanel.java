package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pl.polsl.bland.desktop.service.SimulationCsvService;
import javafx.scene.control.TextArea;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.Button;
import javafx.scene.control.Separator;


public class ResultsPanel extends BorderPane {

   private SimulationCsvService.ParsedSimulation lastSim = null;
    private String lastNetlist = null;

    private final Label statusLabel = new Label("Brak wyników symulacji.");
    private final Button btnShowResults = new Button("Pokaż wyniki symulacji");

    public ResultsPanel() {
        setPadding(new Insets(8));
        btnShowResults.setMaxWidth(Double.MAX_VALUE);
        btnShowResults.setDisable(true);
        btnShowResults.setOnAction(e -> openResultsWindow());

        VBox box = new VBox(8,
                new Label("Ostatnia symulacja:"),
                statusLabel,
                new Separator(),
                btnShowResults
        );
        box.setPadding(new Insets(8));
        setCenter(box);
    }

    public void showSimulation(SimulationCsvService.ParsedSimulation sim, String netlist) {
        this.lastSim = sim;
        this.lastNetlist = netlist;
        statusLabel.setText("Punktów: " + sim.sampleCount()
                + "\nSygnały: " + String.join(", ",
                sim.headers().subList(1, sim.headers().size())));
        btnShowResults.setDisable(false);
    }

   private void openResultsWindow() {
    if (lastSim == null) return;
    new ResultsWindow(lastSim, lastNetlist).show();
}
}