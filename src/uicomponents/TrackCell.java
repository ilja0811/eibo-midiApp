package uicomponents;

import logic.Project;

import java.io.IOException;

import javax.sound.midi.Track;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ListCell;

public class TrackCell extends ListCell<Track> {

    private Parent root;
    private TrackCellViewEditController editController;
    private TrackCellViewPlayController playController;
    private FXMLLoader loader;
    private Project project;
    private TrackCellType type;

    public TrackCell(Project project, TrackCellType type) {
        this.project = project;
        this.type = type;
        try {
            initLoader();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initLoader() throws IOException {
        String fxmlPath;

        if (type == TrackCellType.EDIT) {
            fxmlPath = "track-cell-view-edit.fxml";

            loader = new FXMLLoader(getClass().getResource(fxmlPath));
            root = loader.load();
            editController = loader.getController();

            editController.setTrackCell(this);
            editController.setProject(project);
            editController.loadDefaultTrackState();
        } else {
            fxmlPath = "track-cell-view-play.fxml";

            loader = new FXMLLoader(getClass().getResource(fxmlPath));
            root = loader.load();
            playController = loader.getController();
            
            playController.setTrackCell(this);
            playController.setProject(project);
            playController.addPlaybackListener();
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