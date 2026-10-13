package application;

import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.stage.Stage;

import java.util.Deque;

public class ProcessChartWindow {

    public static void show(String processName, int pid, HistoryManager historyManager) {
        Stage stage = new Stage();
        stage.setTitle("Активность процесса: " + processName + " (PID: " + pid + ")");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Время (сек. назад)");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Байты");

        AreaChart<Number, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("Трафик процесса");

        XYChart.Series<Number, Number> sentSeries = new XYChart.Series<>();
        sentSeries.setName("Отправлено");
        XYChart.Series<Number, Number> recvSeries = new XYChart.Series<>();
        recvSeries.setName("Получено");

        Deque<HistoryManager.DataPoint> history = historyManager.getHistory(pid);
        long now = System.currentTimeMillis();
        for (HistoryManager.DataPoint dp : history) {
            double secondsAgo = (now - dp.getTimestamp()) / 1000.0;
            sentSeries.getData().add(new XYChart.Data<>(secondsAgo, dp.getSent()));
            recvSeries.getData().add(new XYChart.Data<>(secondsAgo, dp.getReceived()));
        }

        chart.getData().addAll(sentSeries, recvSeries);
        Scene scene = new Scene(chart, 800, 600);
        stage.setScene(scene);
        stage.show();
    }
}