package pl.polsl.bland.desktop.view;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;


public class LoginDialog extends Dialog<LoginDialog.LoginResult> {

    public enum Mode { LOGIN, GUEST }
    public record LoginResult(Mode mode, String username, String password) {}

    public LoginDialog() {
        setTitle("Logowanie");

        ButtonType btnLogin = new ButtonType("Zaloguj", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnRegister = new ButtonType("Załóż konto", ButtonBar.ButtonData.LEFT);
        ButtonType btnGuest = new ButtonType("Gość", ButtonBar.ButtonData.CANCEL_CLOSE);

        getDialogPane().getButtonTypes().addAll(btnLogin, btnRegister, btnGuest);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        grid.add(new Label("Login:"), 0, 0);
        grid.add(username, 1, 0);

        grid.add(new Label("Hasło:"), 0, 1);
        grid.add(password, 1, 1);

        getDialogPane().setContent(grid);

        var registerButton = getDialogPane().lookupButton(btnRegister);
        registerButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            RegisterDialog reg = new RegisterDialog();
            var result = reg.showAndWait();

            if (result.isPresent()) {
                var r = result.get();
                try {
                    pl.polsl.bland.desktop.service.ApiService.get()
                            .register(r.username(), r.email(), r.password());
                    new Alert(Alert.AlertType.INFORMATION,
                            "Konto utworzone! Możesz się teraz zalogować.").showAndWait();
                } catch (Exception ex) {
                    new Alert(Alert.AlertType.ERROR,
                            "Błąd rejestracji:\n" + ex.getMessage()).showAndWait();
                }
            }

            event.consume(); // nie zamykaj LoginDialog
        });

        setResultConverter(btn -> {
            if (btn == btnLogin) {
                return new LoginResult(Mode.LOGIN, username.getText(), password.getText());
            }
            return new LoginResult(Mode.GUEST, null, null);
        });
    }
}
