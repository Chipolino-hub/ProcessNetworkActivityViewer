package application;

import java.util.*;

public class SnapshotManager {
    private List<Connection> previousSnapshot = null;

    public void takeSnapshot(List<Connection> current) {
        previousSnapshot = new ArrayList<>();
        for (Connection c : current) {
            previousSnapshot.add(new Connection(c.getProcessName(), c.getPid(),
                    c.getLocalAddress(), c.getLocalPort(),
                    c.getRemoteAddress(), c.getRemotePort(),
                    c.getProtocol()));
        }
    }

    public List<ConnectionDiff> compareWithCurrent(List<Connection> current) {
        if (previousSnapshot == null) return Collections.emptyList();

        Map<String, Connection> prevMap = new HashMap<>();
        for (Connection c : previousSnapshot) {
            prevMap.put(c.getKey(), c);
        }

        Map<String, Connection> currMap = new HashMap<>();
        for (Connection c : current) {
            currMap.put(c.getKey(), c);
        }

        List<ConnectionDiff> diffs = new ArrayList<>();

        // Появившиеся
        for (Connection c : current) {
            if (!prevMap.containsKey(c.getKey())) {
                diffs.add(new ConnectionDiff(c, DiffType.NEW));
            }
        }

        // Исчезнувшие
        for (Connection c : previousSnapshot) {
            if (!currMap.containsKey(c.getKey())) {
                diffs.add(new ConnectionDiff(c, DiffType.GONE));
            }
        }

        return diffs;
    }

    public boolean hasSnapshot() {
        return previousSnapshot != null;
    }

    public static class ConnectionDiff {
        private final Connection connection;
        private final DiffType type;

        public ConnectionDiff(Connection connection, DiffType type) {
            this.connection = connection;
            this.type = type;
        }

        public Connection getConnection() { return connection; }
        public DiffType getType() { return type; }
    }

    public enum DiffType {
        NEW, GONE, CHANGED
    }
}