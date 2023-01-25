
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Instrument;
import javax.sound.midi.Track;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class TrackCellViewController implements Initializable {

    /* FXML */
    @FXML
    private Button midiButton;

    @FXML
    private ChoiceBox<String> instrDropdown;

    @FXML
    private Label presetName;

    @FXML
    private Label trackIndexLabel;
    /* ----- */

    private FruityProject project;

    private TrackCell trackCell;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        instrDropdown.setValue("Preset");

        midiButton.setOnAction(event -> {
            File file = midiFileChooser();

            if (file != null) {
                project.loadTrackMidi(trackCell.getIndex(), file.getAbsolutePath());
                presetName.setText(file.getName().split(".mid")[0]);
                midiButton.setText(file.getName());
            }
        });

        instrDropdown.valueProperty().addListener(event -> {
            project.loadTrackInstr(trackCell.getIndex(), instrDropdown.getItems().indexOf(instrDropdown.getValue()));
        });
    }

    public void setProject(FruityProject project) {
        this.project = project;
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;

        trackIndexLabel.setText(String.valueOf(trackCell.getIndex() + 1));
        trackCell.indexProperty().addListener((observable, oldIndex, newIndex) -> {
            trackIndexLabel.setText(String.valueOf(newIndex.intValue() + 1));
        });
    }

    public void updateTrack(Track track) {

    }

    public void loadDropdown() {
        for (Instrument i : project.getInstruments()) {
            instrDropdown.getItems().add(i.getName());
        }
    }

    public File midiFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a MIDI file");
        // fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setInitialDirectory(new File("midis"));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("MIDI Files", "*.mid"));

        return fileChooser.showOpenDialog(App.getStage());
    }
}
