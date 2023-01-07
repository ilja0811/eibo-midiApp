import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class App extends Application {

    private static Stage stage;

    public static void main(String[] args) {
        launch();
    }

    @Override
    public void start(Stage stage) throws Exception {
        App.stage = stage;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/menu-view.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 800, 450);

        stage.setTitle("Projekt");
        stage.setScene(scene);
        stage.show();
    }

    public static Stage getStage() {
        return stage;
    }
}
