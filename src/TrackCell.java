import java.io.IOException;

import javax.sound.midi.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

public class TrackCell extends ListCell<Track> {

    private Parent root;
    private TrackCellViewController controller;

    public TrackCell(FruityProject project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/track-cell-view.fxml"));

            root = loader.load();
            controller = loader.getController();
            controller.setProject(project);
            controller.setTrackCell(this);
            controller.loadDropdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (item != null) {
            controller.updateTrack(item);
            this.setGraphic(root);
        } else {
            this.setGraphic(null);
        }
    }
}
