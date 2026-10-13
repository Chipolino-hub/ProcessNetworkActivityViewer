package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetstatParser {

    public static List<Connection> getCurrentConnections(Map<Integer, String> processMap) {
        List<Connection> result = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("netstat", "-ano").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("Proto") || line.startsWith("Протокол")) {
                        continue; // пропускаем заголовок
                    }

                    // Парсим строку вручную, отрезая PID в конце
                    Connection conn = parseLine(line, processMap);
                    if (conn != null) {
                        result.add(conn);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static Connection parseLine(String line, Map<Integer, String> processMap) {
        // Ищем PID в конце строки (последнее число)
        int lastSpaceIndex = line.lastIndexOf(' ');
        String pidStr = line.substring(lastSpaceIndex + 1).trim();
        int pid;
        try {
            pid = Integer.parseInt(pidStr);
        } catch (NumberFormatException e) {
            return null; // не удалось распарсить PID — пропускаем строку
        }

        // Отрезаем PID, остаётся часть с протоколом и адресами
        String remaining = line.substring(0, lastSpaceIndex).trim();

        // Разбиваем оставшуюся строку на части, учитывая, что адреса могут содержать квадратные скобки
        String[] tokens = remaining.split("\\s+");
        if (tokens.length < 3) {
            return null; // недостаточно данных
        }

        String protocol = tokens[0].toUpperCase();
        if (!protocol.equals("TCP") && !protocol.equals("UDP")) {
            return null; // не TCP/UDP
        }

        // Локальный адрес и порт
        String localPart = tokens[1];
        String[] localSplit = splitAddressPort(localPart);
        if (localSplit == null) return null;
        String localAddr = localSplit[0];
        int localPort = Integer.parseInt(localSplit[1]);

        // Удалённый адрес и порт
        String remotePart = tokens[2];
        String[] remoteSplit = splitAddressPort(remotePart);
        if (remoteSplit == null) return null;
        String remoteAddr = remoteSplit[0];
        int remotePort;
        if (remoteSplit[1].equals("*")) {
            remotePort = 0; // для UDP
        } else {
            remotePort = Integer.parseInt(remoteSplit[1]);
        }

        String processName = processMap.getOrDefault(pid, "unknown");
        return new Connection(processName, pid, localAddr, localPort, remoteAddr, remotePort, protocol);
    }

    /**
     * Разделяет строку вида "адрес:порт" на две части.
     * Учитывает IPv6-адреса в квадратных скобках: [::1]:80
     */
    private static String[] splitAddressPort(String s) {
        if (s.contains("[")) {
            // IPv6: ищем закрывающую скобку и двоеточие после неё
            int closeBracket = s.indexOf(']');
            if (closeBracket == -1 || closeBracket + 1 >= s.length() || s.charAt(closeBracket + 1) != ':') {
                return null;
            }
            String address = s.substring(0, closeBracket + 1); // включая скобки
            String portStr = s.substring(closeBracket + 2);
            return new String[]{address, portStr};
        } else {
            // IPv4 или hostname
            int colon = s.lastIndexOf(':');
            if (colon == -1) return null;
            String address = s.substring(0, colon);
            String portStr = s.substring(colon + 1);
            return new String[]{address, portStr};
        }
    }
}