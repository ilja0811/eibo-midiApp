package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MidiPlayer extends Application {

    private static Stage stage;

    private static final String APP_TITLE = "MIDI Player";
    private static final String FXML_PATH = "../scenes/menu-view.fxml";

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        MidiPlayer.stage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource(FXML_PATH));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 450);

        stage.setTitle(APP_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getStage() {
        return stage;
    }
}
