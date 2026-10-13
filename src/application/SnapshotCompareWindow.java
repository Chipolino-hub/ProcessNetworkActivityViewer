package application;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class SnapshotCompareWindow {

    public static void show(java.util.List<SnapshotManager.ConnectionDiff> diffs) {
        Stage stage = new Stage();
        stage.setTitle("Сравнение снимков");

        TableView<SnapshotManager.ConnectionDiff> table = new TableView<>();
        ObservableList<SnapshotManager.ConnectionDiff> data = FXCollections.observableArrayList(diffs);

        TableColumn<SnapshotManager.ConnectionDiff, String> colType = new TableColumn<>("Изменение");
        colType.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getType().toString()));
        colType.setCellFactory(col -> new TableCell<SnapshotManager.ConnectionDiff, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    switch (item) {
                        case "NEW": setStyle("-fx-background-color: lightgreen;"); break;
                        case "GONE": setStyle("-fx-background-color: lightcoral;"); break;
                        case "CHANGED": setStyle("-fx-background-color: lightyellow;"); break;
                        default: setStyle("");
                    }
                }
            }
        });

        TableColumn<SnapshotManager.ConnectionDiff, String> colName = new TableColumn<>("Процесс");
        colName.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(cellData.getValue().getConnection().getProcessName()));

        TableColumn<SnapshotManager.ConnectionDiff, Integer> colPid = new TableColumn<>("PID");
        colPid.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(cellData.getValue().getConnection().getPid()).asObject());

        TableColumn<SnapshotManager.ConnectionDiff, String> colLocal = new TableColumn<>("Локальный адрес");
        colLocal.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getConnection().getLocalAddress() + ":" +
                                cellData.getValue().getConnection().getLocalPort()));

        TableColumn<SnapshotManager.ConnectionDiff, String> colRemote = new TableColumn<>("Удалённый адрес");
        colRemote.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getConnection().getRemoteAddress() + ":" +
                                cellData.getValue().getConnection().getRemotePort()));

        table.getColumns().addAll(colType, colName, colPid, colLocal, colRemote);
        table.setItems(data);

        VBox root = new VBox(table);
        Scene scene = new Scene(root, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}