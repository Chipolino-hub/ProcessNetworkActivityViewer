package application;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Duration;

import java.net.URL;
import java.util.*;

public class Controller implements Initializable {

    @FXML private TableView<Connection> tableView;
    @FXML private TableColumn<Connection, String> colProcess;
    @FXML private TableColumn<Connection, Integer> colPid;
    @FXML private TableColumn<Connection, String> colLocal;
    @FXML private TableColumn<Connection, String> colRemote;
    @FXML private TableColumn<Connection, String> colProtocol;
    @FXML private TableColumn<Connection, Long> colSent;
    @FXML private TableColumn<Connection, Long> colReceived;

    @FXML private TextField searchField;
    @FXML private ComboBox<String> protocolFilter;
    @FXML private ComboBox<String> portFilter;
    @FXML private Label statusLabel;
    @FXML private Label totalConnectionsLabel;
    @FXML private Label totalSentLabel;
    @FXML private Label totalReceivedLabel;

    private ObservableList<Connection> masterData = FXCollections.observableArrayList();
    private FilteredList<Connection> filteredData;
    private SortedList<Connection> sortedData;

    private final Timeline refreshTimeline = new Timeline();
    private int refreshInterval = 5; // секунд

    private final HistoryManager historyManager = new HistoryManager();
    private final SnapshotManager snapshotManager = new SnapshotManager();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTableColumns();
        setupFilters();
        setupRefreshTimeline();
        loadData();

        tableView.setRowFactory(tv -> {
            TableRow<Connection> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    Connection conn = row.getItem();
                    ProcessChartWindow.show(conn.getProcessName(), conn.getPid(), historyManager);
                }
            });
            return row;
        });
    }

    private void setupTableColumns() {
        colProcess.setCellValueFactory(new PropertyValueFactory<>("processName"));
        colPid.setCellValueFactory(new PropertyValueFactory<>("pid"));
        colLocal.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getLocalAddress() + ":" + cellData.getValue().getLocalPort()));
        colRemote.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getRemoteAddress() + ":" + cellData.getValue().getRemotePort()));
        colProtocol.setCellValueFactory(new PropertyValueFactory<>("protocol"));
        colSent.setCellValueFactory(new PropertyValueFactory<>("sentBytes"));
        colReceived.setCellValueFactory(new PropertyValueFactory<>("receivedBytes"));
    }

    private void setupFilters() {
        filteredData = new FilteredList<>(masterData, p -> true);
        sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(tableView.comparatorProperty());
        tableView.setItems(sortedData);

        searchField.textProperty().addListener((obs, old, newVal) -> applyFilters());

        protocolFilter.setItems(FXCollections.observableArrayList("Все", "TCP", "UDP"));
        protocolFilter.setValue("Все");
        protocolFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());

        portFilter.setItems(FXCollections.observableArrayList(
                "Все", "80 (HTTP)", "443 (HTTPS)", "3389 (RDP)", "4444 (Metasploit)", "22 (SSH)"));
        portFilter.setValue("Все");
        portFilter.valueProperty().addListener((obs, old, newVal) -> applyFilters());
    }

    private void applyFilters() {
        filteredData.setPredicate(conn -> {
            if (!searchField.getText().isEmpty()) {
                String search = searchField.getText().toLowerCase();
                if (!conn.getProcessName().toLowerCase().contains(search) &&
                        !conn.getLocalAddress().toLowerCase().contains(search) &&
                        !conn.getRemoteAddress().toLowerCase().contains(search) &&
                        !String.valueOf(conn.getLocalPort()).contains(search) &&
                        !String.valueOf(conn.getRemotePort()).contains(search)) {
                    return false;
                }
            }

            String protocol = protocolFilter.getValue();
            if (!"Все".equals(protocol) && !conn.getProtocol().equals(protocol)) {
                return false;
            }

            String portChoice = portFilter.getValue();
            if (portChoice != null && !"Все".equals(portChoice)) {
                int port = extractPort(portChoice);
                if (conn.getLocalPort() != port && conn.getRemotePort() != port) {
                    return false;
                }
            }
            return true;
        });
        updateStats();
    }

    private int extractPort(String choice) {
        String[] parts = choice.split(" ");
        return Integer.parseInt(parts[0]);
    }

    private void setupRefreshTimeline() {
        refreshTimeline.setCycleCount(Timeline.INDEFINITE);
        refreshTimeline.getKeyFrames().add(
                new KeyFrame(Duration.seconds(refreshInterval), e -> loadData())
        );
        refreshTimeline.play();
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    private void loadData() {
        Map<Integer, String> processMap = ProcessNameFetcher.getProcessMap();
        List<Connection> newConnections = NetstatParser.getCurrentConnections(processMap);

        // Обновляем счетчики трафика (имитация)
        updateTrafficCounters(newConnections);

        masterData.setAll(newConnections);
        historyManager.addSnapshot(newConnections);
        updateStats();
        statusLabel.setText("Последнее обновление: " + new Date());
    }

    private void updateTrafficCounters(List<Connection> newConns) {
        // Для демонстрации добавляем случайные значения к существующим соединениям
        Random rand = new Random();
        for (Connection conn : newConns) {
            // Ищем такое же соединение в старых данных (по ключу)
            Optional<Connection> old = masterData.stream()
                    .filter(c -> c.getKey().equals(conn.getKey()))
                    .findFirst();
            if (old.isPresent()) {
                // Переносим старые счетчики и добавляем приращение
                conn.addSentBytes(old.get().getSentBytes() + rand.nextInt(1000));
                conn.addReceivedBytes(old.get().getReceivedBytes() + rand.nextInt(1000));
            } else {
                // Новое соединение
                conn.addSentBytes(rand.nextInt(5000));
                conn.addReceivedBytes(rand.nextInt(5000));
            }
        }
    }

    private void updateStats() {
        long total = filteredData.size();
        long totalSent = filteredData.stream().mapToLong(Connection::getSentBytes).sum();
        long totalRecv = filteredData.stream().mapToLong(Connection::getReceivedBytes).sum();

        totalConnectionsLabel.setText("Соединений: " + total);
        totalSentLabel.setText("Отправлено: " + totalSent + " байт");
        totalReceivedLabel.setText("Получено: " + totalRecv + " байт");
    }

    @FXML
    private void onTakeSnapshot() {
        snapshotManager.takeSnapshot(masterData);
        showAlert("Снимок сохранён", "Текущее состояние сохранено как снимок.");
    }

    // ИСПРАВЛЕННЫЙ МЕТОД
    @FXML
    private void onCompareSnapshot() {
        if (!snapshotManager.hasSnapshot()) {
            showAlert("Нет снимка", "Сначала сделайте снимок.");
            return;
        }
        List<SnapshotManager.ConnectionDiff> diffs = snapshotManager.compareWithCurrent(masterData);
        if (diffs.isEmpty()) {
            showAlert("Сравнение", "Изменений не обнаружено.");
        } else {
            SnapshotCompareWindow.show(diffs);
        }
    }

    @FXML
    private void onAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("Process Network Activity Viewer");

        String description = """
                Программа позволяет в реальном времени отслеживать сетевые соединения всех запущенных процессов.
                
                Основные возможности:
                • Отображение списка активных TCP/UDP-соединений с указанием процесса, PID, адресов и портов.
                • Фильтрация по протоколу, портам и текстовый поиск.
                • Визуализация трафика процесса на графике (двойной клик по строке).
                • Сравнение двух снимков сети для быстрого обнаружения новых или исчезнувших соединений.
                
                Программа только наблюдает за сетевой активностью и не вмешивается в работу системы.
                Предназначена для образовательных целей и контроля за сетевыми взаимодействиями.
                """;

        String versionInfo = """
                
                Версия: 1.0
                Год выпуска: 2026
                """;

        alert.setContentText(description + versionInfo);
        alert.getDialogPane().setMinWidth(500);
        alert.showAndWait();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}