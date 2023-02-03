package uicomponents;

import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Track;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import logic.Project;

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
    private Track track;
    private TrackCell trackCell;

    private static final String UNMUTE_IMG_PATH = "assets/outline_volume_up_white_48dp.png";
    private static final String MUTE_IMG_PATH = "assets/outline_volume_off_white_48dp.png";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        muteButton.setOnAction(event -> toggleMute());
    }

    public void toggleMute() {
        if (project.playing().get()) {
            if (project.trackIsMuted(track)) {
                project.muteTrack(track, false);

                Platform.runLater(() -> {
                    muteButtonImg.setImage(new Image(UNMUTE_IMG_PATH));
                });
            } else {
                project.muteTrack(track, true);

                Platform.runLater(() -> {
                    muteButtonImg.setImage(new Image(MUTE_IMG_PATH));
                });
            }
        }
    }

    public void updateTrack(Track track) {
        this.track = track;

        String name = project.getTracks().get(track).getName();
        String instrument = project.getTracks().get(track).getInstrument();

        trackIndexLabel.setText(String.valueOf(trackCell.indexProperty().get() + 1));

        trackName.setText(name);
        if (instrument != null) {
            instrName.setText(instrument);
        } else {
            instrName.setText(project.getInstruments()[0].getName());
        }

    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void addPlaybackListener() {
        project.playbackEnded().addListener((observarble, oldValue, newValue) -> {
            if (newValue) {
                muteButtonImg.setImage(new Image(UNMUTE_IMG_PATH));
            }
        });
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }
}
