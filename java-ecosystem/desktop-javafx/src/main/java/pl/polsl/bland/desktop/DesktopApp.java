package pl.polsl.bland.desktop;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import pl.polsl.bland.desktop.view.MainView;

public class DesktopApp extends Application {

    @Override
    public void start(Stage primaryStage) {
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
