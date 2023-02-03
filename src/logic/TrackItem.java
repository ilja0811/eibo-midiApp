package logic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sound.midi.Instrument;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

public class TrackItem {

    private Sequencer sequencer;
    private Sequence sequence;
    private Track track;
    private Instrument[] instruments;

    private String trackName;
    private String midiPath;
    private String midiFileName;
    private String instrumentName;

    public TrackItem(Sequencer sequencer, Sequence sequence, Instrument[] instruments) {
        this.sequencer = sequencer;
        this.sequence = sequence;
        this.track = sequence.createTrack();
        this.instruments = instruments;
    }

    public void delete() {
        sequence.deleteTrack(track);
    }

    public boolean isValid() {
        return midiPath != null;
    }

    public void loadTrackInstr(int instrIndex) {
        try {
            Instrument instrument = instruments[instrIndex];
            this.instrumentName = instrument.getName();

            Track trackNewInstr = sequence.createTrack();
            // if there is already is a patch change event in the track, remove it
            for (int i = 0; i < track.size(); i++) {
                MidiEvent e = track.get(i);

                if (e.getMessage() instanceof ShortMessage) {
                    ShortMessage sm = (ShortMessage) e.getMessage();
                    if (sm.getCommand() == ShortMessage.PROGRAM_CHANGE) {
                        track.remove(e);
                    }
                }
            }

            // add patch change event (instrument change)
            MidiEvent instrEvent = new MidiEvent(
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument.getPatch().getProgram(),
                            0),
                    0);
            trackNewInstr.add(instrEvent);

            for (int i = 0; i < track.size(); i++) {
                MidiEvent e = track.get(i);
                trackNewInstr.add(e);
            }

            sequence.deleteTrack(track);
            this.track = trackNewInstr;
        } catch (InvalidMidiDataException e) {
            System.out.println("An error occured while loading your selected instrument: " + e.getMessage());
        }
    }

    public void loadMIDIfile(String midiPath) {
        try {
            updateMidiPath(midiPath);

            Sequence midiSeq = MidiSystem.getSequence(new File(midiPath));

            /*
             * add all events from old track except for instrument changes (=program
             * changes)
             */
            List<MidiEvent> toRemove = new ArrayList<>();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (!((event.getMessage() instanceof ShortMessage &&
                        ((ShortMessage) event.getMessage()).getCommand() == ShortMessage.PROGRAM_CHANGE))) {
                    toRemove.add(event);
                }
            }

            // remove all note events from old track
            for (MidiEvent event : toRemove) {
                track.remove(event);
            }

            for (Track t : midiSeq.getTracks()) {
                // Iterate through the events in the curr track
                for (int i = 0; i < t.size(); i++) {
                    // Get the event and add it to newTrack
                    MidiEvent event = t.get(i);
                    track.add(event);
                }
            }
        } catch (IOException | InvalidMidiDataException e) {
            System.out.println("An error occured while loading your .midi file: " + e.getMessage());
        }
    }

    public void mute() {
        if (isMuted()) {
            sequencer.setTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track), false);
        } else {
            sequencer.setTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track), true);
        }
    }

    public boolean isMuted() {
        return sequencer.getTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track));
    }

    private void updateMidiPath(String midiPath) {
        this.midiPath = midiPath;
        this.midiFileName = new File(midiPath).getName();
        this.trackName = new File(midiPath).getName().split(".mid")[0];
    }

    /*
     * Getters and Setters
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */

    public Track getTrack() {
        return track;
    }

    public String getTrackName() {
        return trackName;
    }

    public String getMidiPath() {
        return midiPath;
    }

    public String getMidiFileName() {
        return midiFileName;
    }

    public String getInstrumentName() {
        return instrumentName;
    }
}
