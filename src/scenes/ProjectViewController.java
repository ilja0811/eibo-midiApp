package scenes;

import uicomponents.TrackCellType;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.sound.midi.Track;
import javax.sound.midi.MidiDevice.Info;

import application.App;
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
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;
import logic.FruityProject;
import resources.HelperClass;
import uicomponents.TrackCell;

public class ProjectViewController implements Initializable {

    /* FXML */
    @FXML
    private Button loadMidiDevButton;

    @FXML
    private Button addTrackButton;

    @FXML
    private ListView<Track> trackListviewEdit;

    @FXML
    private ListView<Track> trackListviewPlay;

    @FXML
    private Button removeTrackButton;

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

    @FXML
    private ImageView playPauseButtonImg;

    @FXML
    private Button stopButton;

    @FXML
    private ImageView stopButtonImg;

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
                return new TrackCell(project, TrackCellType.EDIT);
            }
        });

        trackListviewEdit.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Track>() {
            @Override
            public void changed(ObservableValue<? extends Track> observable, Track oldTrack, Track newTrack) {
                // pass
            }
        });

        trackListviewPlay.setCellFactory(new Callback<ListView<Track>, ListCell<Track>>() {
            @Override
            public ListCell<Track> call(ListView<Track> param) {
                return new TrackCell(project, TrackCellType.PLAY);
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
            if (project.isPlaying()) {
                project.pause();
                playPauseButtonImg.setImage(new Image("assets/round_play_arrow_white_48dp.png"));
            } else {
                project.play();
                if (project.isPlaying()) {
                    playPauseButtonImg.setImage(new Image("assets/round_pause_white_48dp.png"));
                } else {
                    Alert alert = new Alert(AlertType.ERROR,"No valid tracks in selection. Playback failed.", ButtonType.OK);
                    alert.showAndWait();
                }
            }
        });

        menuButton.setOnAction(event -> {
            project.closeAll();
            loadMenuView();
        });

        addTrackButton.setOnAction(event -> {
            tracksInEdit.add(project.addTrack());
        });

        removeTrackButton.setOnAction(event -> {
            Track selectedTrack = trackListviewEdit.getSelectionModel().getSelectedItem();
            tracksInEdit.remove(selectedTrack);
            project.deleteTrack(selectedTrack);
            trackListviewEdit.refresh();
        });

        loadMidiDevButton.setOnAction(event -> {
            List<String> choices = new ArrayList<>();

            for (Info i : project.getMidiDevices()) {
                choices.add(i.getName());
            }

            ChoiceDialog<String> dialog = new ChoiceDialog<>(choices.get(0), choices);
            dialog.setTitle("Choose a MIDI device");
            dialog.setHeaderText("Choose one of the following devices:");
            dialog.setContentText("Devices:");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                project.loadMidiDevice(choices.indexOf(result.get()));
            }
        });

        saveButton.setOnAction(event -> {
            Alert alert;

            tracksInPlay.clear();

            if (!project.getValidTracks().isEmpty()) {
                tracksInPlay.addAll(project.getValidTracks().keySet());

                alert = new Alert(AlertType.INFORMATION,
                        "Successfully added tracks: " + project.getValidTracks().size(), ButtonType.OK);
                alert.showAndWait();
            } else {
                alert = new Alert(AlertType.ERROR,
                        "No valid tracks in selection. Successfully added tracks: 0", ButtonType.OK);
                alert.showAndWait();
            }
        });

        stopButton.setOnAction(event -> {
            if (project.isPlaying()) {
                project.stop();
            }
        });
    }

    public void loadMenuView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("menu-view.fxml"));
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
