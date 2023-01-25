import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Track;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import javafx.util.Callback;

public class ProjectViewController implements Initializable {

    /* FXML */
    @FXML
    private Button midiDevButton;

    @FXML
    private Button addInstrButton;

    @FXML
    private ListView<Track> trackListviewEdit;

    @FXML
    private ListView<Track> trackListviewPlay;

    @FXML
    private Button removeInstrButton;

    @FXML
    private Button playPauseButton;

    @FXML
    private TabPane tabPane;

    @FXML
    private Button menuButton;

    @FXML
    private ScrollPane editScrollPane;

    @FXML
    private Slider bpmSlider;

    @FXML
    private Label bpmLabel;

    @FXML
    private Slider posSlider;

    @FXML
    private Label posLabel;

    @FXML
    private Label lengthLabel;

    @FXML
    private Button saveButton;
    /* ----- */

    private FruityProject project;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        project = new FruityProject();

        ObservableList<Track> tracksInEdit = trackListviewEdit.getItems();
        ObservableList<Track> tracksInPlay = trackListviewPlay.getItems();

        trackListviewEdit.setCellFactory(new Callback<ListView<Track>, ListCell<Track>>() {
            @Override
            public ListCell<Track> call(ListView<Track> param) {
                return new TrackCell(project);
            }
        });

        trackListviewEdit.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Track>() {
            @Override
            public void changed(ObservableValue<? extends Track> observable, Track oldTrack, Track newTrack) {
                System.out.println("Selected Track " + newTrack);
            }
        });

        trackListviewPlay.setCellFactory(new Callback<ListView<Track>, ListCell<Track>>() {
            @Override
            public ListCell<Track> call(ListView<Track> param) {
                return new TrackCell(project);
            }
        });

        bpmSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            project.setBpm(newValue.intValue());
        });

        bpmLabel.textProperty().bind(project.bpm().asString("%.0f"));

        project.time().addListener((observable, oldValue, newValue) -> {
            posSlider.setValue(newValue.longValue());

            Platform.runLater(() -> {
                posLabel.setText(HelperClass.formatTime(newValue.longValue()));
            });
        });

        project.length().addListener((observable, oldValue, newValue) -> {
            posSlider.setMax(newValue.longValue());

            Platform.runLater(() -> {
                lengthLabel.setText(HelperClass.formatTime(newValue.longValue()));
            });
        });

        playPauseButton.setOnAction(event -> {
            project.play();
        });

        menuButton.setOnAction(event -> {
            loadMenuView();
        });

        addInstrButton.setOnAction(event -> {
            tracksInEdit.add(project.addTrack());
        });

        removeInstrButton.setOnAction(event -> {
            Track selectedTrack = trackListviewEdit.getSelectionModel().getSelectedItem();
            System.out.println("Deleted Track: " + selectedTrack);
            project.deleteTrack(selectedTrack);
            tracksInEdit.remove(selectedTrack);
            trackListviewEdit.refresh();
        });

        midiDevButton.setOnAction(event -> {
            project.selectMidiDevice();
        });

        saveButton.setOnAction(event -> {
            Alert alert;

            tracksInPlay.clear();
            if (!project.getTracks().isEmpty()) {
                tracksInPlay.addAll(project.getTracks());
                alert = new Alert(AlertType.INFORMATION,
                        "Successfully added tracks: " + project.getTracks().size(), ButtonType.OK);
                alert.showAndWait();
            } else {
                alert = new Alert(AlertType.ERROR,
                        "No valid tracks in selection. Successfully added tracks: 0", ButtonType.OK);
                alert.showAndWait();
            }
        });
    }

    public void loadMenuView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/menu-view.fxml"));
            Parent root;
            root = loader.load();

            // Create the new scene
            Scene newScene = new Scene(root, 800, 450);

            // Get the current stage and set the new scene
            Stage stage = App.getStage();
            stage.setScene(newScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exit() {
        project.closeAll();
    }
}
