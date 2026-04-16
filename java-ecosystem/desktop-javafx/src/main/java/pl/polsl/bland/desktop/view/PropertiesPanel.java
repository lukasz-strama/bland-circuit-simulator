package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import pl.polsl.bland.desktop.service.WorkspaceService;

import java.util.Map;

public class PropertiesPanel extends VBox {

    private final Map<String, WorkspaceService.WorkspaceElement> elements;
    private final Runnable refresh;
    private final WorkspaceService workspace;

    private final Label title = new Label("Właściwości");
    private final Label idLabel = new Label("-");
    private final TextField valueField = new TextField();
    private final Button applyButton = new Button("Zastosuj");
    private final Button rotateButton = new Button("Obróć 90°");

    private WorkspaceService.WorkspaceElement current;

    public PropertiesPanel(
            Map<String, WorkspaceService.WorkspaceElement> elements,
            Runnable refresh,
            WorkspaceService workspace
    ) {
        this.elements = elements;
        this.refresh = refresh;
        this.workspace = workspace;

        setPadding(new Insets(10));
        setSpacing(8);

        valueField.setPromptText("Wartość elementu");

        applyButton.setOnAction(e -> applyValue());
        rotateButton.setOnAction(e -> rotateCurrent());

        getChildren().addAll(title, idLabel, valueField, applyButton, rotateButton);
    }

    public void showElement(WorkspaceService.WorkspaceElement el) {
        current = el;

        if (el == null) {
            idLabel.setText("Brak zaznaczenia");
            valueField.setText("");
            return;
        }

        idLabel.setText("Element: " + el.id());
        valueField.setText(el.value());
    }

    private void applyValue() {
        if (current == null) return;

        WorkspaceService.WorkspaceElement updated =
                new WorkspaceService.WorkspaceElement(
                        current.id(),
                        current.type(),
                        current.x(),
                        current.y(),
                        valueField.getText(),
                        current.rotation()
                );

        elements.put(current.id(), updated);
        refresh.run();
    }

    private void rotateCurrent() {
        if (current == null) return;

        WorkspaceService.WorkspaceElement rotated =
                workspace.rotateElement(current);

        elements.put(rotated.id(), rotated);
        refresh.run();
    }
}
