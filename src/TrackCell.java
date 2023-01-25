import java.io.IOException;

import javax.sound.midi.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

public class TrackCell extends ListCell<Track> {

    private Parent root;
    private TrackCellViewController editController;
    private TrackCellViewPlayController playController;

    public TrackCell(FruityProject project, TrackCellType type) {
        if (type == TrackCellType.EDIT) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/track-cell-view.fxml"));

                root = loader.load();
                editController = loader.getController();
                editController.setTrackCell(this);
                editController.setProject(project);
                editController.loadDefaultTrackState();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/track-cell-view-play.fxml"));

                root = loader.load();
                playController = loader.getController();
                playController.setTrackCell(this);
                playController.setProject(project);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            if (editController != null) {
                editController.updateTrack(item);
            }
            if (playController != null) {
                playController.updateTrack(item);
            }
            this.setGraphic(root);
        } else {
            this.setGraphic(null);
        }
    }
}
