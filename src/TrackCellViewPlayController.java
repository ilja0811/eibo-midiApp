import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Track;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;

public class TrackCellViewPlayController implements Initializable {

    @FXML
    private Label instrName;

    @FXML
    private ToggleButton muteButton;

    @FXML
    private Label trackIndexLabel;

    @FXML
    private Label trackName;

    private FruityProject project;
    private Track track;
    private TrackCell trackCell;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        muteButton.setOnAction(event -> {
            if (project.trackIsMuted(track)) {
                project.muteTrack(track, false);
            } else {
                project.muteTrack(track, true);
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
            instrName.setText("Default");
        }
        
    }

    public void setProject(FruityProject project) {
        this.project = project;
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }
}
