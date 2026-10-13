package application;

import java.util.Objects;

public class Connection {
    private final String processName;
    private final int pid;
    private final String localAddress;
    private final int localPort;
    private final String remoteAddress;
    private final int remotePort;
    private final String protocol; // "TCP" or "UDP"
    private long sentBytes;
    private long receivedBytes;

    public Connection(String processName, int pid, String localAddress, int localPort,
                      String remoteAddress, int remotePort, String protocol) {
        this.processName = processName;
        this.pid = pid;
        this.localAddress = localAddress;
        this.localPort = localPort;
        this.remoteAddress = remoteAddress;
        this.remotePort = remotePort;
        this.protocol = protocol;
        this.sentBytes = 0;
        this.receivedBytes = 0;
    }

    // Геттеры и сеттеры
    public String getProcessName() { return processName; }
    public int getPid() { return pid; }
    public String getLocalAddress() { return localAddress; }
    public int getLocalPort() { return localPort; }
    public String getRemoteAddress() { return remoteAddress; }
    public int getRemotePort() { return remotePort; }
    public String getProtocol() { return protocol; }
    public long getSentBytes() { return sentBytes; }
    public long getReceivedBytes() { return receivedBytes; }

    public void addSentBytes(long delta) { this.sentBytes += delta; }
    public void addReceivedBytes(long delta) { this.receivedBytes += delta; }

    // Уникальный ключ для сравнения снимков (игнорируем счетчики)
    public String getKey() {
        return pid + ":" + protocol + ":" + localAddress + ":" + localPort + ":" + remoteAddress + ":" + remotePort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Connection that = (Connection) o;
        return pid == that.pid &&
                localPort == that.localPort &&
                remotePort == that.remotePort &&
                Objects.equals(localAddress, that.localAddress) &&
                Objects.equals(remoteAddress, that.remoteAddress) &&
                Objects.equals(protocol, that.protocol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, localAddress, localPort, remoteAddress, remotePort, protocol);
    }
}