package uicomponents;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import logic.Project;
import logic.TrackItem;

public class TrackCellViewPlayController implements Initializable {

    @FXML
    private Label instrName;

    @FXML
    private ToggleButton muteButton;

    @FXML
    private Label trackIndexLabel;

    @FXML
    private Label trackName;

    @FXML
    private ImageView muteButtonImg;

    private Project project;
    private TrackItem trackItem;
    private TrackCell trackCell;

    private static final String UNMUTED_IMG_PATH = "assets/outline_volume_up_white_48dp.png";
    private static final String MUTED_IMG_PATH = "assets/outline_volume_off_white_48dp.png";

    private final String DEFAULT_INSTR_TEXT = "Default";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        muteButton.setOnAction(event -> toggleMute());
    }

    public void toggleMute() {
        if (project.playing().get()) {
            trackItem.mute();

            Platform.runLater(() -> {
                if (trackItem.isMuted()) {
                    muteButtonImg.setImage(new Image(MUTED_IMG_PATH));
                } else {
                    muteButtonImg.setImage(new Image(UNMUTED_IMG_PATH));
                }
            });
        }
    }

    public void updateTrackItem(TrackItem trackItem) {
        this.trackItem = trackItem;

        trackIndexLabel.setText(String.valueOf(trackCell.indexProperty().get() + 1));

        String name = trackItem.getTrackName();
        String instrument = trackItem.getInstrumentName();
        trackName.setText(name);
        if (instrument == null) {
            instrName.setText(DEFAULT_INSTR_TEXT);
        } else {
            instrName.setText(instrument);
        }
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void addPlaybackListener() {
        project.playbackEnded().addListener((observarble, oldValue, newValue) -> {
            if (newValue) {
                muteButtonImg.setImage(new Image(UNMUTED_IMG_PATH));
            }
        });
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }
}
