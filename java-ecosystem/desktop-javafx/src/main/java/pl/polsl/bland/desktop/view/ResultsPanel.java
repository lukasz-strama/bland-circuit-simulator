package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import pl.polsl.bland.desktop.service.SimulationCsvService;

public class ResultsPanel extends BorderPane {

    private final Label infoLabel = new Label("Brak wyników symulacji.");
    private final TabPane tabs    = new TabPane();

    public ResultsPanel() {
        setPadding(new Insets(8));

        VBox header = new VBox(4,
                new Label("Wyniki symulacji"),
                infoLabel
        );
        header.setPadding(new Insets(0, 0, 8, 0));

        setTop(header);
        setCenter(tabs);
    }

    public void showSimulation(SimulationCsvService.ParsedSimulation sim) {
        tabs.getTabs().clear();
        if (sim == null) {
            infoLabel.setText("Brak danych do wyświetlenia.");
            return;
        }

        infoLabel.setText("Symulacja załadowana. Liczba punktów: " + sim.points().size());

        Tab rawTab = new Tab("Surowe dane");
        rawTab.setClosable(false);
        rawTab.setContent(new Label("Tutaj możesz później dodać tabelę / wykres."));

        tabs.getTabs().add(rawTab);
    }

    public void clearResults() {
        tabs.getTabs().clear();
        infoLabel.setText("Brak wyników symulacji.");
    }
}
