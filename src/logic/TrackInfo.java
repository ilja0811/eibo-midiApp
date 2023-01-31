package logic;
import java.io.File;

public class TrackInfo {

    private String name;
    private String midiPath;
    private String midiFile;
    private String instrument;

    public TrackInfo() {
        name = null;
        midiPath = null;
        instrument = null;
    }

    public boolean isNull() {
        return midiPath == null;
    }

    /*
     * Getters and Setters
     * -----------------------------------------------------------------------------
     * -----------------------------------------------------------------------------
     */

    public void setMidiPath(String midiPath) {
        this.midiPath = midiPath;
        this.midiFile = new File(midiPath).getName();
        this.name = new File(midiPath).getName().split(".mid")[0];
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMidiPath() {
        return midiPath;
    }

    public String getMidiFile() {
        return midiFile;
    }

    public String getInstrument() {
        return instrument;
    }

    public void setInstrument(String instrument) {
        this.instrument = instrument;
    }
}
