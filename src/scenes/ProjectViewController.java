package scenes;

import uicomponents.TrackCellType;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javax.sound.midi.Instrument;
import javax.sound.midi.MidiDevice.Info;

import application.MidiPlayer;
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
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.util.Callback;
import logic.Project;
import logic.TrackItem;
import resources.HelperClass;
import uicomponents.TrackCell;

public class ProjectViewController implements Initializable {

    /* FXML */
    @FXML
    private Button loadMidiDeviceButton;

    @FXML
    private Button addTrackItemButton;

    @FXML
    private ListView<TrackItem> trackItemListviewEdit;

    @FXML
    private ListView<TrackItem> trackItemListviewPlay;

    @FXML
    private Button removeTrackItemButton;

    @FXML
    private Button playbackButton;

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

    @FXML
    private Button changeDeviceInstrButton;

    @FXML
    private Tab playModeTab;
    /* ----- */

    private Project project;

    private final String PLAY_IMG_PATH = "assets/round_play_arrow_white_48dp.png";
    private final String PAUSE_IMG_PATH = "assets/round_pause_white_48dp.png";
    private final String MENU_FXML_PATH = "menu-view.fxml";

    private ObservableList<TrackItem> trackItemsEdit;
    private ObservableList<TrackItem> trackItemsPlay;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        project = new Project();

        /*
         * Tab 1 (Edit Mode)
         */
        trackItemsEdit = trackItemListviewEdit.getItems();

        trackItemListviewEdit.setCellFactory(new Callback<ListView<TrackItem>, ListCell<TrackItem>>() {
            @Override
            public ListCell<TrackItem> call(ListView<TrackItem> param) {
                return new TrackCell(project, TrackCellType.EDIT);
            }
        });

        trackItemListviewEdit.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TrackItem>() {
            @Override
            public void changed(ObservableValue<? extends TrackItem> observable, TrackItem oldTrack,
                    TrackItem newTrack) {
                // pass
            }
        });

        menuButton.setOnAction(event -> {
            project.closeAll();

            Platform.runLater(() -> {
                loadMenuView();
            });
        });

        removeTrackItemButton.setOnAction(event -> {
            Platform.runLater(() -> {
                TrackItem selectedItem = trackItemListviewEdit.getSelectionModel().getSelectedItem();
                trackItemsEdit.remove(selectedItem);
                project.deleteTrackItem(selectedItem);
                trackItemListviewEdit.refresh();
            });

        });

        addTrackItemButton.setOnAction(event -> {
            Platform.runLater(() -> {
                trackItemsEdit.add(project.addTrackItem());
            });
        });

        saveButton.setOnAction(event -> {
            Platform.runLater(() -> {
                trackItemsPlay.clear();
                showTracksSavedAlert();
            });
        });

        /*
         * Tab 2 (Play Mode)
         */
        trackItemsPlay = trackItemListviewPlay.getItems();

        trackItemListviewPlay.setCellFactory(new Callback<ListView<TrackItem>, ListCell<TrackItem>>() {
            @Override
            public ListCell<TrackItem> call(ListView<TrackItem> param) {
                return new TrackCell(project, TrackCellType.PLAY);
            }
        });

        playbackButton.setOnAction(event -> {
            project.togglePlayback();

            Platform.runLater(() -> {
                if (!project.isPlaying() && project.getValidTracks().isEmpty()) {
                    showPlaybackFailedAlert();
                }
            });
        });

        stopButton.setOnAction(event -> {
            if (project.isPlaying()) {
                project.stop();

                Platform.runLater(() -> {
                    resetPosSliderView();
                });
            }
        });

        bpmSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            project.setBpm(newValue.intValue());
        });

        bpmLabel.textProperty().bind(project.bpm().asString("%.0f"));

        project.pos().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                posSlider.setValue(newValue.longValue());
                posLabel.setText(HelperClass.formatTime(newValue.longValue()));
            });
        });

        project.length().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                posSlider.setMax(newValue.longValue());
                lengthLabel.setText(HelperClass.formatTime(newValue.longValue()));
            });
        });

        project.playing().addListener((observable, oldValue, newValue) -> {
            Platform.runLater(() -> {
                refreshPlaybackView();
            });
        });

        changeDeviceInstrButton.setOnAction(event -> {
            if (project.getReceiver() != null) {
                Platform.runLater(() -> {
                    showDeviceChoiceDialog();
                });
            } else {
                Platform.runLater(() -> {
                    showNoDeviceAlert();
                });
            }
        });

        loadMidiDeviceButton.setOnAction(event -> {
            Platform.runLater(() -> {
                showInstrumentChoiceDialog();
            });
        });

        tabPane.getSelectionModel().selectedItemProperty().addListener((observable, oldTab, newTab) -> {
            if (oldTab.equals(playModeTab)) {
                project.stop();
                project.setTracksSaved(false);

                Platform.runLater(() -> {
                    trackItemsPlay.clear();
                    resetPosSliderView();
                });
            }
        });
    }

    public void loadMenuView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(MENU_FXML_PATH));
            Parent root;
            root = loader.load();

            // Create the new scene
            Scene newScene = new Scene(root, 800, 450);

            // Get the current stage and set the new scene
            Stage stage = MidiPlayer.getStage();
            stage.setScene(newScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void refreshPlaybackView() {
        if (project.isPlaying()) {
            playPauseButtonImg.setImage(new Image(PAUSE_IMG_PATH));
        } else {
            playPauseButtonImg.setImage(new Image(PLAY_IMG_PATH));
        }
    }

    public void showPlaybackFailedAlert() {
        Alert alert = new Alert(AlertType.ERROR,
                project.getValidTracks().isEmpty() ? "Playback failed: No valid tracks in selection."
                        : "Playback failed: Tracks need to be saved before playback.",
                ButtonType.OK);
        alert.showAndWait();
    }

    public void showNoDeviceAlert() {
        Alert alert = new Alert(AlertType.ERROR, "Please select a MIDI device before changing instruments.",
                ButtonType.OK);
        alert.showAndWait();
    }

    public void showTracksSavedAlert() {
        Alert alert;
        if (!project.getValidTracks().isEmpty()) {
            trackItemsPlay.addAll(project.getValidTracks());
            project.setTracksSaved(true);

            alert = new Alert(AlertType.INFORMATION,
                    "Successfully added tracks: " + project.getValidTracks().size(), ButtonType.OK);
            alert.showAndWait();
        } else {
            project.setTracksSaved(false);

            alert = new Alert(AlertType.ERROR,
                    "No valid tracks in selection. Successfully added tracks: 0", ButtonType.OK);
            alert.showAndWait();
        }
    }

    public void showDeviceChoiceDialog() {
        List<String> choices = new ArrayList<>();

        for (Instrument i : project.getInstruments()) {
            choices.add(i.getName());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                project.getDeviceInstrument() == null ? choices.get(0) : project.getDeviceInstrument().getName(),
                choices);
        dialog.setTitle("Choose an Instrument");
        dialog.setHeaderText("Choose one of the following instruments:");
        dialog.setContentText("Instruments:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            project.changeReceiverInstrument((choices.indexOf(result.get())));
        }
    }

    public void showInstrumentChoiceDialog() {
        List<String> choices = new ArrayList<>();

        for (Info i : project.getMidiDevices()) {
            choices.add(i.getName());
        }

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                project.getDevice() == null ? choices.get(0) : project.getDevice().getDeviceInfo().getName(),
                choices);
        dialog.setTitle("Choose a MIDI device");
        dialog.setHeaderText("Choose one of the following devices:");
        dialog.setContentText("Devices:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            project.loadMidiDevice(choices.indexOf(result.get()));
        }
    }

    public void resetPosSliderView() {
        posLabel.setText(HelperClass.formatTime(0));
        posSlider.setValue(0);
    }

    public void exit() {
        project.closeAll();
    }
}
