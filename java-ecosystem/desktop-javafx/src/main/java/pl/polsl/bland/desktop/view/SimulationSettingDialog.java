package pl.polsl.bland.desktop.view;


import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import pl.polsl.bland.models.SimulationRequest;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class SimulationSettingDialog extends Dialog<SimulationRequest> {

    public SimulationSettingDialog() {
        setTitle("Ustawienia symulacji");
        setHeaderText("Wybierz typ analizy i parametry");

        ButtonType btnRun = new ButtonType("Symuluj", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnRun, btnCancel);

        // --- zakładka DC ---
        VBox dcContent = new VBox(8);
        dcContent.setPadding(new Insets(12));
        dcContent.getChildren().add(new Label("Analiza DC nie wymaga dodatkowych parametrów."));

        Tab dcTab = new Tab("DC");
        dcTab.setClosable(false);
        dcTab.setContent(dcContent);

        // --- zakładka Transient ---
        GridPane trGrid = new GridPane();
        trGrid.setHgap(12);
        trGrid.setVgap(8);
        trGrid.setPadding(new Insets(12));

        TextField tfTstop = new TextField("0.008");
        TextField tfTstep = new TextField("0.0001");

        trGrid.addRow(0, new Label("Czas końcowy (tstop) [s]:"), tfTstop);
        trGrid.addRow(1, new Label("Krok czasowy (tstep) [s]:"), tfTstep);

        Tab trTab = new Tab("Transient");
        trTab.setClosable(false);
        trTab.setContent(trGrid);

        TabPane tabPane = new TabPane(dcTab, trTab);
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        getDialogPane().setContent(tabPane);

        setResultConverter(btn -> {
            if (btn != btnRun) return null;

            Tab selected = tabPane.getSelectionModel().getSelectedItem();

            if (selected == dcTab) {
                return new SimulationRequest(null, SimulationRequest.AnalysisType.DC, Map.of());
            }

            // Transient
            double tstop, tstep;
            try {
                tstop = Double.parseDouble(tfTstop.getText().trim());
                tstep = Double.parseDouble(tfTstep.getText().trim());
            } catch (NumberFormatException e) {
                new Alert(Alert.AlertType.ERROR, "Nieprawidłowe wartości parametrów.", ButtonType.OK).showAndWait();
                return null;
            }

            Map<String, Double> params = new LinkedHashMap<>();
            params.put("tstop", tstop);
            params.put("tstep", tstep);
            return new SimulationRequest(null, SimulationRequest.AnalysisType.TRANSIENT, params);
        });
    }

        
}