
public class TestFruityProject {
    public static void main(String[] args) {
        FruityProject pj = new FruityProject();
       
        pj.addTrack();
        pj.loadTrackInstr(0, 0);
        pj.loadTrackMidi(0, "midis/HowWeDo.mid");
        pj.play();
    }
}
