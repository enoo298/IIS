package hr.algebra.client.controller;

import hr.algebra.client.service.AuthService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Label statusLabel;
    @FXML private Button loginButton;
    @FXML private ProgressIndicator progress;

    private final AuthService authService = new AuthService();

    @FXML
    private void onLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();


        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            statusLabel.setText("Unesite korisničko ime i lozinku.");
            return;
        }

        setUiDisabled(true);

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return authService.login(username, password);
            }
        };

        task.setOnSucceeded(ev -> {
            setUiDisabled(false);
            if (task.getValue()) {
                openMainView();
            } else {
                statusLabel.setText("Neuspješna prijava.");
            }
        });

        task.setOnFailed(ev -> {
            setUiDisabled(false);
            Throwable ex = task.getException();
            statusLabel.setText("Greška: " + (ex != null ? ex.getMessage() : "nepoznato"));
            if (ex != null) ex.printStackTrace();
        });

        new Thread(task, "login-thread").start();
    }

    private void setUiDisabled(boolean disabled) {
        usernameField.setDisable(disabled);
        passwordField.setDisable(disabled);
        loginButton.setDisable(disabled);
        if (progress != null) progress.setVisible(disabled);
        statusLabel.setText(disabled ? "Prijava u tijeku..." : "");
    }

    private void openMainView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/hr/algebra/client/MainView.fxml"));
            Scene scene = new Scene(loader.load());

            MainController controller = loader.getController();
            controller.setAuthService(authService);

            Stage stage = new Stage();
            stage.setTitle("XML Validacija");
            stage.setScene(scene);
            stage.show();


            ((Stage) usernameField.getScene().getWindow()).close();
        } catch (Exception e) {
            statusLabel.setText("Greška pri otvaranju glavnog prozora: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
