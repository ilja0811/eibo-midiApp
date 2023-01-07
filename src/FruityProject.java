import java.io.File;
import java.io.IOException;

import javax.sound.midi.*;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleLongProperty;

public class FruityProject {

    private Sequence sequence;
    private Synthesizer synth;
    private Soundbank soundbank;
    private Instrument[] instruments;
    private Sequencer sequencer;

    private SimpleFloatProperty bpm;
    private SimpleLongProperty length;
    private SimpleLongProperty time;

    private final float defaultBPM = 140;

    public FruityProject() {
        try {
            sequence = new Sequence(Sequence.PPQ, 480);
            synth = MidiSystem.getSynthesizer();
            synth.open();

            soundbank = synth.getDefaultSoundbank();
            synth.loadAllInstruments(soundbank);
            instruments = synth.getLoadedInstruments();

            sequencer = MidiSystem.getSequencer();

            bpm = new SimpleFloatProperty();
            length = new SimpleLongProperty();
            time = new SimpleLongProperty();

            bpm.addListener((observable, oldBPM, newValue) -> {
                sequencer.setTempoInBPM(newValue.intValue());
                length.set((long) ((sequencer.getTickLength() / (sequence.getResolution()
                        * (sequencer.getTempoInBPM() / 60)))));
            });

            setBpm(defaultBPM);

            // listener to check if sequencer is done running
            sequencer.addMetaEventListener(new MetaEventListener() {
                @Override
                public void meta(MetaMessage meta) {
                    if (meta.getType() == 47) {
                        // The end of the sequence has been reached
                        sequencer.close();
                    }
                }
            });
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public SimpleLongProperty time() {
        return time;
    }

    public SimpleFloatProperty bpm() {
        return bpm;
    }

    public void setBpm(float f) {
        bpm.set(f);
    }

    public void setTickPosition(long l) {
        long tickPosition = (long) (l * (sequence.getResolution() * (sequencer.getTempoInBPM() / 60)));
        sequencer.setTickPosition(tickPosition);
    }

    public SimpleLongProperty length() {
        return length;
    }

    public Instrument[] getInstruments() {
        return instruments;
    }

    public Track addTrack() {
        sequence.createTrack();
        return sequence.getTracks()[sequence.getTracks().length - 1];
    }

    public void deleteTrack(Track track) {
        sequence.deleteTrack(track);
    }

    public void loadTrackInstr(int trackIndex, int instrIndex) {
        Track track = sequence.getTracks()[trackIndex];
        try {
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
            MidiEvent instr = new MidiEvent(
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, instruments[instrIndex].getPatch().getProgram(),
                            0),
                    0);
            track.add(instr);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void loadTrackMidi(int index, String midiPath) {
        try {
            Track track = sequence.getTracks()[index];
            Sequence seq = MidiSystem.getSequence(new File(midiPath));

            for (Track t : seq.getTracks()) {
                // Iterate through the events in the curr track
                for (int i = 0; i < t.size(); i++) {
                    // Get the event and add it to newTrack
                    MidiEvent event = t.get(i);
                    track.add(event);
                }
            }
        } catch (IOException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void startRunningThread() {
        new Thread(() -> {
            while (sequencer.isRunning()) {
                time.set((long) ((sequencer.getTickPosition() / (sequence.getResolution()
                        * (sequencer.getTempoInBPM() / 60)))));
                System.out.println(time.get());
            }
        }).start();
    }

    public void play() {
        try {
            sequencer.open();
            sequencer.setSequence(sequence);

            length.set(
                    (long) ((sequencer.getTickLength()
                            / (sequence.getResolution() * (sequencer.getTempoInBPM() / 60)))));

            sequencer.start();
            startRunningThread();
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void closeAll() {
        synth.close();
        sequencer.close();
    }
}
