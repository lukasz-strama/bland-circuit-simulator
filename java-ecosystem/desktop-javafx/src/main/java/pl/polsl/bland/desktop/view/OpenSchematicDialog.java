package pl.polsl.bland.desktop.view;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import pl.polsl.bland.desktop.service.ApiService;

import java.util.List;
import java.util.Optional;

/**
 * Okno dialogowe służące do wyboru i otwarcia schematu zapisanego w bazie danych.
 *
 * Funkcjonalność:
 * <ul>
 *     <li>pobiera listę schematów z backendu (asynchronicznie),</li>
 *     <li>wyświetla je w ListView,</li>
 *     <li>umożliwia wybór schematu i zatwierdzenie przyciskiem „Otwórz”,</li>
 *     <li>obsługuje podwójne kliknięcie jako szybkie otwarcie,</li>
 *     <li>wyświetla komunikaty o błędach połączenia.</li>
 * </ul>
 *
 * Wynik dialogu to {@link ApiService.SchematicMeta} wybranego schematu.
 */
public class OpenSchematicDialog extends Dialog<ApiService.SchematicMeta> {

    private final ListView<ApiService.SchematicMeta> listView = new ListView<>();
    private final Label statusLabel = new Label("Ładowanie listy schematów…");

    /**
     * Tworzy okno dialogowe i automatycznie rozpoczyna pobieranie listy schematów.
     *
     * @param apiService serwis API używany do pobrania listy schematów
     */
    public OpenSchematicDialog(ApiService apiService) {
        setTitle("Otwórz schemat");
        setHeaderText("Wybierz schemat do wczytania:");

        // przycisk otwarcia aktywny tylko przy zaznaczeniu elementu
        ButtonType btnOpen  = new ButtonType("Otwórz",  ButtonBar.ButtonData.OK_DONE);
        ButtonType btnClose = new ButtonType("Anuluj",  ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnOpen, btnClose);

  
        Button openBtn = (Button) getDialogPane().lookupButton(btnOpen);
        openBtn.setDisable(true);
        listView.getSelectionModel().selectedItemProperty().addListener(
                (obs, o, n) -> openBtn.setDisable(n == null));

        // podwójne kliknięcie = szybkie otwarcie
        listView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2
                    && listView.getSelectionModel().getSelectedItem() != null) {
                openBtn.fire();
            }
        });

    
        listView.setPrefSize(480, 260);
        listView.setPlaceholder(new Label("Brak zapisanych schematów."));

        VBox content = new VBox(8, statusLabel, listView);
        content.setPadding(new Insets(10));
        getDialogPane().setContent(content);

      
        setResultConverter(bt -> {
            if (bt == btnOpen) return listView.getSelectionModel().getSelectedItem();
            return null;
        });

        loadList(apiService);
    }

    /**
     * Asynchronicznie pobiera listę schematów z backendu.
     * Aktualizuje UI w wątku JavaFX.
     */
    private void loadList(ApiService apiService) {
        Thread.ofVirtual().start(() -> {
            try {
                List<ApiService.SchematicMeta> list = apiService.listSchematics();
                Platform.runLater(() -> {
                    listView.getItems().setAll(list);
                    statusLabel.setText(
                            list.isEmpty()
                                    ? "Brak schematów w bazie."
                                    : "Znaleziono " + list.size() + " schemat(ów).");
                });
            } catch (Exception ex) {
                Platform.runLater(() ->
                        statusLabel.setText("Błąd połączenia: " + ex.getMessage()));
            }
        });
    }

     /**
     * Statyczna metoda pomocnicza otwierająca dialog i zwracająca wynik.
     *
     * @param apiService serwis API
     * @return wybrany schemat lub pusty Optional
     */
    public static Optional<ApiService.SchematicMeta> show(ApiService apiService) {
        return new OpenSchematicDialog(apiService).showAndWait();
    }
}