package application;

import java.util.*;

public class HistoryManager {
    // Для каждого PID храним кольцевой буфер (до 60 точек = 10 мин при шаге 10 сек)
    private final Map<Integer, Deque<DataPoint>> history = new HashMap<>();
    private final int maxPoints = 60;

    public void addSnapshot(List<Connection> connections) {
        // Группируем по PID и суммируем трафик
        Map<Integer, Long> sentByPid = new HashMap<>();
        Map<Integer, Long> recvByPid = new HashMap<>();

        for (Connection c : connections) {
            sentByPid.merge(c.getPid(), c.getSentBytes(), Long::sum);
            recvByPid.merge(c.getPid(), c.getReceivedBytes(), Long::sum);
        }

        long now = System.currentTimeMillis();

        for (Integer pid : sentByPid.keySet()) {
            long sent = sentByPid.getOrDefault(pid, 0L);
            long recv = recvByPid.getOrDefault(pid, 0L);
            history.computeIfAbsent(pid, k -> new ArrayDeque<>()).add(new DataPoint(now, sent, recv));
            Deque<DataPoint> deque = history.get(pid);
            if (deque.size() > maxPoints) {
                deque.removeFirst();
            }
        }
    }

    public Deque<DataPoint> getHistory(int pid) {
        return history.getOrDefault(pid, new ArrayDeque<>());
    }

    public static class DataPoint {
        private final long timestamp;
        private final long sent;
        private final long received;

        public DataPoint(long timestamp, long sent, long received) {
            this.timestamp = timestamp;
            this.sent = sent;
            this.received = received;
        }

        public long getTimestamp() { return timestamp; }
        public long getSent() { return sent; }
        public long getReceived() { return received; }
    }
}