import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.sound.midi.*;

import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleLongProperty;

public class FruityProject {

    private Sequence sequence;
    private Synthesizer synth;
    private Soundbank soundbank;
    private Instrument[] instruments;
    private Sequencer sequencer;

    private MidiDevice device;
    private MidiInputReceiver receiver;
    private List<MidiDevice.Info> validMidisInfos;

    private MidiChannel channel;

    private SimpleFloatProperty bpm;
    private SimpleLongProperty length;
    private SimpleLongProperty time;

    private List<Track> validTracks;
    private final float defaultBPM = 140;

    public FruityProject() {
        try {
            sequence = new Sequence(Sequence.PPQ, 480);
            synth = MidiSystem.getSynthesizer();
            synth.open();

            soundbank = synth.getDefaultSoundbank();
            synth.loadAllInstruments(soundbank);
            instruments = synth.getLoadedInstruments();

            // Get a MIDI channel
            channel = synth.getChannels()[10];

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

            validTracks = new ArrayList<>();
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
                // System.out.println(time.get());
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

    public void selectMidiDevice() {
        new Thread(() -> {
            try {
                if (receiver != null) {
                    receiver.close();
                }

                // Get a list of available MIDI devices
                MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
                validMidisInfos = new ArrayList<>();

                // Evaluate only valid MIDIs that have MIDI OUT PORT
                for (int i = 0; i < infos.length; i++) {
                    device = MidiSystem.getMidiDevice(infos[i]);
                    device.open();
                    if (device.getMaxTransmitters() != 0) {
                        validMidisInfos.add(infos[i]);
                    }
                    device.close();
                }

                for (MidiDevice.Info i : validMidisInfos) {
                    System.out.println(validMidisInfos.indexOf(i) + ": " + i.getName());
                }

                // Prompt the user to select a MIDI device
                Scanner scanner = new Scanner(System.in);

                System.out.print("Select a MIDI device: ");
                if (scanner.hasNextInt()) {
                    System.out.println("Has next int");
                    int deviceIndex = scanner.nextInt();
                    loadMidiDevice(deviceIndex);
                }
                scanner.close();
            } catch (MidiUnavailableException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void loadMidiDevice(int index) {
        try {
            device = MidiSystem.getMidiDevice(validMidisInfos.get(index));
            device.open();

            // Create a listener for MIDI messages
            receiver = new MidiInputReceiver(channel);
            device.getTransmitter().setReceiver(receiver);

            System.out.println("Listening for MIDI input...");
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    private class MidiInputReceiver implements Receiver {
        private MidiChannel channel;

        public MidiInputReceiver(MidiChannel channel) {
            this.channel = channel;
        }

        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int command = shortMessage.getCommand();
                int note = shortMessage.getData1();

                if (command == ShortMessage.NOTE_ON) {
                    int velocity = shortMessage.getData2();
                    channel.noteOn(note, velocity);
                } else if (command == ShortMessage.NOTE_OFF) {
                    channel.noteOff(note);
                }
            }
        }

        @Override
        public void close() {
            for (int i = 0; i < 128; i++) {
                channel.noteOff(i);
            }
            synth.close();
            device.close();
        }
    }

    public void closeAll() {
        synth.close();
        sequencer.close();
        if (receiver != null) {
            receiver.close();
        }
    }

    public void deleteEmptyTracks() {
        for (Track t : sequence.getTracks()) {
            if (t.size() < 3) {
                sequence.deleteTrack(t);
            }
        }
    }

    public List<Track> getTracks() {
        validTracks.clear();

        for (Track t : sequence.getTracks()) {
            if (t.size() >= 3) {
                validTracks.add(t);
            }
        }
        return validTracks;
    }
}
