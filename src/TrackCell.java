import java.io.IOException;

import javax.sound.midi.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ListCell;

public class TrackCell extends ListCell<Track> {

    private Parent root;

    public TrackCell(FruityProject project) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("resources/track-cell-view.fxml"));

            root = loader.load();
            TrackCellViewController controller = loader.getController();
            controller.setTrackCell(this);
            controller.setProject(project);
            controller.loadDropdown();
            Button editButton = controller.getEditNameButton();

            selectedProperty().addListener(event -> {
                if (selectedProperty().getValue()) {
                    editButton.setDisable(false);
                } else {
                    editButton.setDisable(true);
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateItem(Track item, boolean empty) {
        super.updateItem(item, empty);

        if (!empty) {
            this.setGraphic(root);
        } else {
            this.setGraphic(null);
        }
    }
}
