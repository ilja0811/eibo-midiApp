package uicomponents;

import application.App;
import logic.FruityProject;
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

    private Track track;

    private TrackCell trackCell;

    private final String instrDefaultVal = "Piano 1";
    private final String nameDefaultVal = "Track";
    private final String midiDefaultVal = "Import MIDI";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        midiButton.setOnAction(event -> {
            File file = midiFileChooser();

            if (file != null) {
                project.loadTrackMidi(track, file.getAbsolutePath());
                presetName.setText(project.getTracks().get(track).getName());
                midiButton.setText(project.getTracks().get(track).getMidiFile());
            }
        });

        instrDropdown.valueProperty().addListener(event -> {
            if (!instrDropdown.getValue().equals(instrDefaultVal)) {
                project.loadTrackInstr(track, instrDropdown.getItems().indexOf(instrDropdown.getValue()));
            }
        });
    }

    public void setProject(FruityProject project) {
        this.project = project;
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }

    public void updateTrack(Track track) {
        this.track = track;

        String name = project.getTracks().get(track).getName();
        String instrument = project.getTracks().get(track).getInstrument();
        String midiFile = project.getTracks().get(track).getMidiFile();

        trackIndexLabel.setText(String.valueOf(trackCell.indexProperty().get() + 1));

        if (name != null) {
            presetName.setText(name);
        } else {
            presetName.setText(nameDefaultVal);
        }
        if (instrument != null) {
            instrDropdown.setValue(instrument);
        } else {
            instrDropdown.setValue(instrDefaultVal);
        }
        if (midiFile != null) {
            midiButton.setText(midiFile);
        } else {
            midiButton.setText(midiDefaultVal);
        }
    }

    public void loadDefaultTrackState() {
        for (Instrument i : project.getInstruments()) {
            instrDropdown.getItems().add(i.getName());
        }
        presetName.setText(nameDefaultVal);
        instrDropdown.setValue(instrDefaultVal);
        midiButton.setText(midiDefaultVal);
    }

    public File midiFileChooser() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select a MIDI file");
        fileChooser.setInitialDirectory(new File("midis"));
        fileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("MIDI Files", "*.mid"));

        return fileChooser.showOpenDialog(App.getStage());
    }
}