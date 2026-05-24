package pl.polsl.bland.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import pl.polsl.bland.desktop.service.ApiService;
import pl.polsl.bland.desktop.view.LoginDialog;
import pl.polsl.bland.desktop.view.MainView;

public class DesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
         LoginDialog dialog = new LoginDialog();
    var result = dialog.showAndWait();

    if (result.isEmpty()) return;

    var r = result.get();

    ApiService apiService = ApiService.get();



    try {
        switch (r.mode()) {
            case LOGIN -> apiService.login(r.username(), r.password());
            case GUEST -> apiService.setGuestMode();
        }
    } catch (Exception ex) {
        new Alert(Alert.AlertType.ERROR, "Błąd logowania:\n" + ex.getMessage()).showAndWait();
        return;
    }

        MainView mainView = new MainView();
        Scene scene = new Scene(mainView, 1024, 768);
        primaryStage.setTitle("Bland Circuit Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
