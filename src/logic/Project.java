package logic;

import java.util.ArrayList;
import java.util.List;

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

    private List<TrackItem> tracks;
    private List<TrackItem> validTracks;
    private boolean tracksSaved;

    private final int DEFAULT_BPM = 140;
    private final int MIDI_CHANNEL = 0; // value between 0-15

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
                sequencer.setTempoInBPM(bpm.get());
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

            tracks = new ArrayList<>();
            validTracks = new ArrayList<>();
            tracksSaved = false;
        } catch (MidiUnavailableException | InvalidMidiDataException e) {
            System.out.println("An error occured when loading the project: " + e.getMessage());
        }
    }

    public TrackItem addTrackItem() {
        TrackItem track = new TrackItem(sequencer, sequence, instruments);
        tracks.add(track);
        return track;
    }

    public void deleteTrackItem(TrackItem track) {
        track.delete();
        tracks.remove(track);
    }

    public void startPosThread() {
        new Thread(() -> {
            while (playing.get()) {
                // have to set tempo in thread because we couldn't extract tempo events from
                // .midi file and have to overwrite tempo changes within the .midi file
                sequencer.setTempoInBPM(bpm.get());
                if (bpm().get() == 0) {
                    pos.set(0);
                } else {
                    pos.set((long) ((sequencer.getTickPosition() / (sequence.getResolution()
                            * (sequencer.getTempoInBPM() / 60)))));
                }
            }
        }).start();
    }

    public void togglePlayback() {
        if (playing.get()) {
            pause();
        } else {
            play();
        }
    }

    public void play() {
        if (tracksSaved && !getValidTracks().isEmpty()) {
            try {
                playbackEnded.set(false);

                sequencer.setSequence(sequence);
                sequencer.open();

                length.set((long) ((sequencer.getTickLength()
                        / (sequence.getResolution() * (bpm.get() / 60)))));

                playing.set(true);
                sequencer.start();
                // because first second plays in tempo of .midi file, tempo had to instantly be
                // set again after start
                sequencer.setTempoInBPM(bpm.get());
                startPosThread();
            } catch (MidiUnavailableException | InvalidMidiDataException e) {
                System.out.println("An error occured during playback: " + e.getMessage());
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
            System.out.println("An error occured when loading your midi devices: " + e.getMessage());
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
    public SimpleLongProperty pos() {
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

    public boolean isPlaying() {
        return playing.get();
    }

    /*
     * Getters and Setters
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */
    public String getDefaultInstrumentName() {
        return instruments[0].getName();
    }

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

    public List<TrackItem> getTracks() {
        return tracks;
    }

    public List<TrackItem> getValidTracks() {
        validTracks.clear();

        for (TrackItem t : tracks) {
            if (t.isValid()) {
                validTracks.add(t);
            }
        }
        return validTracks;
    }

    public void setBpm(int tempo) {
        bpm.set(tempo);
    }

    public void setTickPosition(long l) {
        long tickPosition = (long) (l * (sequence.getResolution() * (sequencer.getTempoInBPM() / 60)));
        sequencer.setTickPosition(tickPosition);
    }

    public Instrument[] getInstruments() {
        return instruments;
    }
}
