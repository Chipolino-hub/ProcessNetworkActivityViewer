package application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class ProcessNameFetcher {

    public static Map<Integer, String> getProcessMap() {
        Map<Integer, String> map = new HashMap<>();
        try {
            // Используем tasklist в CSV формате без заголовка
            Process process = new ProcessBuilder("tasklist", "/FO", "CSV", "/NH").start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Формат: "image name","pid","session name","session#","mem usage"
                    String[] parts = line.split("\",\"");
                    if (parts.length >= 2) {
                        String name = parts[0].replace("\"", "");
                        try {
                            int pid = Integer.parseInt(parts[1].replace("\"", ""));
                            map.put(pid, name);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
}