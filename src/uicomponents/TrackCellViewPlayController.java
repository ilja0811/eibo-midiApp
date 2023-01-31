package uicomponents;
import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Track;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import logic.FruityProject;

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

    private FruityProject project;
    private Track track;
    private TrackCell trackCell;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        muteButton.setOnAction(event -> {
            if (project.isPlaying()) {
                if (project.trackIsMuted(track)) {
                    project.muteTrack(track, false);
                    muteButtonImg.setImage(new Image("assets/outline_volume_up_white_48dp.png"));
                } else {
                    project.muteTrack(track, true);
                    muteButtonImg.setImage(new Image("assets/outline_volume_off_white_48dp.png"));
                }
            }
        });
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

    public void setProject(FruityProject project) {
        this.project = project;
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }
}
