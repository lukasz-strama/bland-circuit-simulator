package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;

public class RegisterDialog extends Dialog<RegisterDialog.RegisterResult> {

    public record RegisterResult(String username, String email, String password) {}

    public RegisterDialog() {
        setTitle("Rejestracja");

        ButtonType btnRegister = new ButtonType("Utwórz konto", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(btnRegister, btnCancel);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        TextField email = new TextField();
        PasswordField password = new PasswordField();
        PasswordField password2 = new PasswordField();

        grid.add(new Label("Login:"), 0, 0);
        grid.add(username, 1, 0);

        grid.add(new Label("Email:"), 0, 1);
        grid.add(email, 1, 1);

        grid.add(new Label("Hasło:"), 0, 2);
        grid.add(password, 1, 2);

        grid.add(new Label("Powtórz hasło:"), 0, 3);
        grid.add(password2, 1, 3);

        getDialogPane().setContent(grid);

        // Walidacja przed zamknięciem okna
        var registerButton = getDialogPane().lookupButton(btnRegister);
        registerButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (username.getText().isBlank() ||
                email.getText().isBlank() ||
                password.getText().isBlank() ||
                password2.getText().isBlank()) {

                new Alert(Alert.AlertType.ERROR, "Wszystkie pola są wymagane.").showAndWait();
                event.consume();
                return;
            }

            if (!password.getText().equals(password2.getText())) {
                new Alert(Alert.AlertType.ERROR, "Hasła nie są takie same.").showAndWait();
                event.consume();
            }
        });

        setResultConverter(btn -> {
            if (btn == btnRegister) {
                return new RegisterResult(
                        username.getText(),
                        email.getText(),
                        password.getText()
                );
            }
            return null;
        });
    }
}
