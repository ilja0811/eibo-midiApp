package uicomponents;

import application.MidiPlayer;
import logic.Project;
import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

import javax.sound.midi.Instrument;
import javax.sound.midi.Track;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;

public class TrackCellViewEditController implements Initializable {

    /* FXML */
    @FXML
    private Button loadMidiButton;

    @FXML
    private ChoiceBox<String> instrDropdown;

    @FXML
    private Label trackLabel;

    @FXML
    private Label trackIndexLabel;
    /* ----- */

    private Project project;
    private Track track;

    private TrackCell trackCell;

    private FileChooser midiFileChooser;
    private File midiFile;

    private String instrText;
    private String trackText;
    private String midiText;

    private final String FILE_CHOOSER_TITLE = "Select a MIDI file";
    private final String FILE_CHOOSER_INIT_DIR = "midis";

    private final String DEFAULT_INSTR_TEXT = "Default";
    private final String DEFAULT_TRACK_TEXT = "Track";
    private final String DEFAULT_MIDI_TEXT = "Import MIDI";
 
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadMidiButton.setOnAction(event -> {
            Platform.runLater(() -> {
                midiFile = midiFileChooser();

                if (midiFile != null) {
                    project.loadMIDIfile(track, midiFile.getAbsolutePath());
                    trackLabel.setText(project.getTracks().get(track).getName());
                    loadMidiButton.setText(project.getTracks().get(track).getMidiFile());
                }
            });
        });

        instrDropdown.valueProperty().addListener(event -> {
            if (!instrDropdown.getValue().equals(DEFAULT_INSTR_TEXT)) {
                project.loadTrackInstr(track, instrDropdown.getItems().indexOf(instrDropdown.getValue()));
            }
        });
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setTrackCell(TrackCell trackCell) {
        this.trackCell = trackCell;
    }

    public void updateTrack(Track track) {
        this.track = track;

        trackText = project.getTracks().get(track).getName();
        if (isStringEmpty(trackText)) {
            trackLabel.setText(DEFAULT_TRACK_TEXT);
        } else {
            trackLabel.setText(trackText);
        }

        instrText = project.getTracks().get(track).getInstrument();
        if (isStringEmpty(instrText)) {
            instrDropdown.setValue(DEFAULT_INSTR_TEXT);
        } else {
            instrDropdown.setValue(instrText);
        }

        midiText = project.getTracks().get(track).getMidiFile();
        if (isStringEmpty(midiText)) {
            loadMidiButton.setText(DEFAULT_MIDI_TEXT);
        } else {
            loadMidiButton.setText(midiText);
        }

        trackIndexLabel.setText(String.valueOf(trackCell.indexProperty().get() + 1));
    }

    private boolean isStringEmpty(String s) {
        return s == null;
    }

    public void loadDefaultTrackState() {
        for (Instrument i : project.getInstruments()) {
            instrDropdown.getItems().add(i.getName());
        }
        trackLabel.setText(DEFAULT_TRACK_TEXT);
        instrDropdown.setValue(DEFAULT_INSTR_TEXT);
        loadMidiButton.setText(DEFAULT_MIDI_TEXT);
    }

    public File midiFileChooser() {
        midiFileChooser = new FileChooser();
        midiFileChooser.setTitle(FILE_CHOOSER_TITLE);
        midiFileChooser.setInitialDirectory(new File(FILE_CHOOSER_INIT_DIR));
        midiFileChooser.getExtensionFilters().addAll(new FileChooser.ExtensionFilter("MIDI Files", "*.mid"));

        return midiFileChooser.showOpenDialog(MidiPlayer.getStage());
    }
}