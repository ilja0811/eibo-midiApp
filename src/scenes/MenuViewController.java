package scenes;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;

public class MenuViewController implements Initializable {

    @FXML
    private Button newProjectButton;

    private final String FXML_PATH = "project-view.fxml";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        newProjectButton.setOnAction(event -> {
            Platform.runLater(() -> {
                loadProjectView();
            });
        });
    }

    public void loadProjectView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH));
            Parent root;
            root = loader.load();

            // Create the new scene
            Scene newScene = new Scene(root, 800, 450);

            // Get the current stage and set the new scene
            Stage stage = (Stage) newProjectButton.getScene().getWindow();
            stage.setScene(newScene);

            // close all open business resources (synth, midi device, etc.) when closing
            // application
            stage.setOnCloseRequest(windowEvent -> {
                ProjectViewController controller = loader.getController();
                controller.exit();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
