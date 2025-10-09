package hr.algebra.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/hr/algebra/client/LoginView.fxml"));
        Scene scene = new Scene(loader.load());
        stage.setTitle("Prijava");
        stage.setScene(scene);
        stage.show();
    }
}
