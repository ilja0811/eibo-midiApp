import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private Map<Track, TrackInfo> tracks;
    private Map<Track, TrackInfo> validTracks;
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
                        // end of the sequence has been reached
                        sequencer.close();
                    }
                }
            });

            tracks = new HashMap<>();
            validTracks = new HashMap<>();
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public Track addTrack() {
        Track newTrack = sequence.createTrack();
        tracks.put(newTrack, new TrackInfo());
        return newTrack;
    }

    public void deleteTrack(Track track) {
        tracks.remove(track);
        sequence.deleteTrack(track);
        track = null;
    }

    public void loadTrackInstr(Track track, int instrIndex) {
        Instrument instrument = instruments[instrIndex];
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
            MidiEvent instrEvent = new MidiEvent(
                    new ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, instrument.getPatch().getProgram(),
                            0),
                    0);
            tracks.get(track).setInstrument(instrument.getName());
            track.add(instrEvent);
        } catch (InvalidMidiDataException e) {
            e.printStackTrace();
        }
    }

    public void loadTrackMidi(Track track, String midiPath) {
        try {
            Sequence seq = MidiSystem.getSequence(new File(midiPath));
            tracks.get(track).setMidiPath(midiPath);

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

    public void deleteEmptyTracks() {
        for (Track t : sequence.getTracks()) {
            if (t.size() < 3) {
                sequence.deleteTrack(t);
            }
        }
    }

    public void muteTrack(Track track, boolean b) {
        sequencer.setTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track), b);
    }

    public boolean trackIsMuted(Track track) {
        return sequencer.getTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track));
    }

    public void closeAll() {
        synth.close();
        sequencer.close();
        if (receiver != null) {
            receiver.close();
        }
    }

    /*
     * Properties
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    public SimpleLongProperty time() {
        return time;
    }

    public SimpleFloatProperty bpm() {
        return bpm;
    }

    public SimpleLongProperty length() {
        return length;
    }

    /*
     * Getters and Setters
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    public Map<Track, TrackInfo> getTracks() {
        return tracks;
    }

    public Map<Track, TrackInfo> getValidTracks() {
        validTracks.clear();

        tracks.forEach((k, v) -> {
            if (!v.isNull()) {
                validTracks.put(k, v);
            }
        });
        return validTracks;
    }

    public void setBpm(float f) {
        bpm.set(f);
    }

    public void setTickPosition(long l) {
        long tickPosition = (long) (l * (sequence.getResolution() * (sequencer.getTempoInBPM() / 60)));
        sequencer.setTickPosition(tickPosition);
    }

    public Instrument[] getInstruments() {
        return instruments;
    }
}
