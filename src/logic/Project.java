package logic;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.midi.*;
import javax.sound.midi.MidiDevice.Info;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleFloatProperty;
import javafx.beans.property.SimpleLongProperty;

public class Project {

    private Sequence sequence;
    private Synthesizer synth;
    private Soundbank soundbank;
    private Instrument[] instruments;
    private Sequencer sequencer;

    private MidiDevice device;
    private MidiInputReceiver receiver;
    private List<MidiDevice.Info> validMidisInfos;

    private MidiChannel deviceMidiChannel;

    private SimpleFloatProperty bpm;
    private SimpleLongProperty length;
    private SimpleLongProperty pos;
    private SimpleBooleanProperty playing;
    private SimpleBooleanProperty playbackEnded;

    private Map<Track, TrackInfo> tracks;
    private Map<Track, TrackInfo> validTracks;
    private boolean tracksSaved;

    private final float DEFAULT_BPM = 140;
    private final int MIDI_CHANNEL = 15; // value between 0-15

    public Project() {
        try {
            sequence = new Sequence(Sequence.PPQ, 480);
            synth = MidiSystem.getSynthesizer();
            synth.open();

            soundbank = synth.getDefaultSoundbank();
            synth.loadAllInstruments(soundbank);
            instruments = synth.getLoadedInstruments();

            // Get a MIDI channel (0-15)
            deviceMidiChannel = synth.getChannels()[MIDI_CHANNEL];

            sequencer = MidiSystem.getSequencer();

            bpm = new SimpleFloatProperty();
            length = new SimpleLongProperty();
            pos = new SimpleLongProperty();
            playing = new SimpleBooleanProperty();
            playbackEnded = new SimpleBooleanProperty();
            playing.set(false);

            bpm.addListener((observable, oldValue, newValue) -> {
                sequencer.setTempoInBPM(newValue.intValue());
                if (newValue.intValue() == 0) {
                    length.set(0);
                } else {
                    length.set((long) ((sequencer.getTickLength() / (sequence.getResolution()
                            * (sequencer.getTempoInBPM() / 60)))));
                }
            });

            setBpm(DEFAULT_BPM);

            // listener to check if sequencer is done running
            sequencer.addMetaEventListener(new MetaEventListener() {
                @Override
                public void meta(MetaMessage meta) {
                    if (meta.getType() == 47) {
                        // end of the sequence has been reached
                        playing.set(false);
                        pos.set(0);
                        playbackEnded.set(true);
                        sequencer.close();
                    }
                }
            });

            tracks = new HashMap<>();
            validTracks = new HashMap<>();
            tracksSaved = false;
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
        sequence.deleteTrack(track);
        tracks.remove(track);
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

    public void loadMIDIfile(Track track, String midiPath) {
        try {
            Sequence seq = MidiSystem.getSequence(new File(midiPath));
            tracks.get(track).setMidiPath(midiPath);

            /*
             * add all events from track except for instrument changes (=program
             * changes)
             */
            List<MidiEvent> toRemove = new ArrayList<>();
            for (int i = 0; i < track.size(); i++) {
                MidiEvent event = track.get(i);
                if (!(event.getMessage() instanceof ShortMessage &&
                        ((ShortMessage) event.getMessage()).getCommand() == ShortMessage.PROGRAM_CHANGE)) {
                    toRemove.add(event);
                }
            }

            // remove all events from track
            for (MidiEvent event : toRemove) {
                track.remove(event);
            }

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
            while (playing.get()) {
                if (bpm().get() == 0) {
                    pos.set(0);
                } else {
                    pos.set((long) ((sequencer.getTickPosition() / (sequence.getResolution()
                            * (sequencer.getTempoInBPM() / 60)))));
                }
            }
        }).start();
    }

    public void play() {
        if (tracksSaved && !getValidTracks().isEmpty()) {
            try {
                playbackEnded.set(false);

                sequencer.open();
                sequencer.setSequence(sequence);
                sequencer.setTempoInBPM(bpm.get());

                playing.set(true);
                length.set((long) ((sequencer.getTickLength()
                        / (sequence.getResolution() * (sequencer.getTempoInBPM() / 60)))));

                sequencer.start();
                startRunningThread();
            } catch (MidiUnavailableException | InvalidMidiDataException e) {
                e.printStackTrace();
            }
        }
    }

    public List<Info> getMidiDevices() {
        try {
            if (receiver != null) {
                receiver.close();
            }
            // Get a list of available MIDI devices
            MidiDevice.Info[] infos = MidiSystem.getMidiDeviceInfo();
            validMidisInfos = new ArrayList<>();

            MidiDevice currDevice;
            // Evaluate only valid MIDIs that have MIDI OUT PORT
            for (int i = 0; i < infos.length; i++) {
                currDevice = MidiSystem.getMidiDevice(infos[i]);
                currDevice.open();
                if (currDevice.getMaxTransmitters() != 0) {
                    validMidisInfos.add(infos[i]);
                }
                currDevice.close();
            }

            return validMidisInfos;
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void loadMidiDevice(int index) {
        try {
            device = MidiSystem.getMidiDevice(validMidisInfos.get(index));
            device.open();

            // Create a listener for MIDI messages
            receiver = new MidiInputReceiver(deviceMidiChannel);
            device.getTransmitter().setReceiver(receiver);
        } catch (MidiUnavailableException e) {
            e.printStackTrace();
        }
    }

    public void muteTrack(Track track, boolean b) {
        sequencer.setTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track), b);
    }

    public boolean trackIsMuted(Track track) {
        return sequencer.getTrackMute(Arrays.asList(sequence.getTracks()).indexOf(track));
    }

    public void pause() {
        playing.set(false);
        sequencer.stop();
    }

    public void closeAll() {
        synth.close();
        sequencer.close();
        if (receiver != null) {
            receiver.close();
        }
    }

    public void stop() {
        playing.set(false);
        sequencer.close();
    }

    public void changeReceiverInstrument(int instrIndex) {
        receiver.changeInstrument(instrIndex);
    }

    /*
     * Private Class
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    private class MidiInputReceiver implements Receiver {
        private MidiChannel channel;
        private int lastNote;
        private Instrument instrument;

        public MidiInputReceiver(MidiChannel channel) {
            this.channel = channel;
        }

        public void send(MidiMessage message, long timeStamp) {
            if (message instanceof ShortMessage) {
                ShortMessage shortMessage = (ShortMessage) message;
                int command = shortMessage.getCommand();
                int note = shortMessage.getData1();
                lastNote = note;

                if (command == ShortMessage.NOTE_ON) {
                    int velocity = shortMessage.getData2();
                    channel.noteOn(note, velocity);
                } else if (command == ShortMessage.NOTE_OFF) {
                    channel.noteOff(note);
                }
            }
        }

        public void changeInstrument(int instrIndex) {
            // because if instrument is changed while playing notes, note won't
            // automatically cut off and keep ringing for eternity
            channel.noteOff(lastNote);
            instrument = instruments[instrIndex];
            channel.programChange(instrument.getPatch().getProgram());
        }

        public Instrument getInstrument() {
            return instrument;
        }

        @Override
        public void close() {
            for (int i = 0; i < 128; i++) {
                channel.noteOff(i);
            }
            device.close();
        }
    }

    /*
     * Properties
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    public SimpleLongProperty time() {
        return pos;
    }

    public SimpleFloatProperty bpm() {
        return bpm;
    }

    public SimpleLongProperty length() {
        return length;
    }

    public SimpleBooleanProperty playing() {
        return playing;
    }

    public SimpleBooleanProperty playbackEnded() {
        return playbackEnded;
    }

    /*
     * Getters and Setters
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    public MidiDevice getDevice() {
        return device;
    }

    public Instrument getDeviceInstrument() {
        return receiver.getInstrument();
    }

    public MidiInputReceiver getReceiver() {
        return receiver;
    }

    public void setTracksSaved(boolean b) {
        tracksSaved = b;
    }

    public Map<Track, TrackInfo> getTracks() {
        return tracks;
    }

    public Map<Track, TrackInfo> getValidTracks() {
        validTracks.clear();

        tracks.forEach((k, v) -> {
            if (v.isValid()) {
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
