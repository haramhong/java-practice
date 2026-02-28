package com.industrial.ai.web;

import com.industrial.ai.detection.AnomalyEvent;
import com.industrial.ai.plc.SensorReading;
import com.industrial.ai.storage.CloudStorage;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * WebVisualizer – serves a minimal HTML/JS dashboard that displays live sensor
 * readings and anomaly events.
 *
 * Two endpoints are exposed:
 *   GET /          → HTML dashboard page
 *   GET /api/data  → JSON snapshot of the latest readings and anomalies
 */
public class WebVisualizer {

    private final int          port;
    private final CloudStorage storage;
    private final List<AnomalyEvent> anomalies;
    private HttpServer server;

    public WebVisualizer(int port, CloudStorage storage, List<AnomalyEvent> anomalies) {
        this.port      = port;
        this.storage   = storage;
        this.anomalies = anomalies;
    }

    /** Start the HTTP server. Returns {@code true} on success. */
    public boolean start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/",         this::handleDashboard);
            server.createContext("/api/data", this::handleApiData);
            server.setExecutor(Executors.newSingleThreadExecutor());
            server.start();
            System.out.println("[Web] Dashboard running at http://localhost:" + port + "/");
            return true;
        } catch (IOException e) {
            System.err.println("[Web] Failed to start HTTP server: " + e.getMessage());
            return false;
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    // -------------------------------------------------------------------------
    // HTTP handlers
    // -------------------------------------------------------------------------

    private void handleDashboard(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String html = buildHtml();
        sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
    }

    private void handleApiData(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String json = buildJson();
        sendResponse(exchange, 200, "application/json", json);
    }

    // -------------------------------------------------------------------------
    // Response builders
    // -------------------------------------------------------------------------

    private String buildJson() {
        List<SensorReading> latest = storage.getLatest(20);
        StringBuilder sb = new StringBuilder();
        sb.append("{\"readings\":[");
        for (int i = 0; i < latest.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(latest.get(i).toJson());
        }
        sb.append("],\"anomalies\":[");
        int start = Math.max(0, anomalies.size() - 5);
        for (int i = start; i < anomalies.size(); i++) {
            if (i > start) sb.append(",");
            AnomalyEvent e = anomalies.get(i);
            sb.append(String.format(
                "{\"ts\":%d,\"temp\":%b,\"pressure\":%b,\"vibration\":%b}",
                e.getReading().getTimestamp(),
                e.isTempAnomaly(), e.isPressureAnomaly(), e.isVibrationAnomaly()));
        }
        sb.append("],\"totalReadings\":").append(storage.size());
        sb.append(",\"totalAnomalies\":").append(anomalies.size());
        sb.append("}");
        return sb.toString();
    }

    private String buildHtml() {
        return "<!DOCTYPE html><html lang=\"ja\"><head>" +
            "<meta charset=\"UTF-8\">" +
            "<meta http-equiv=\"refresh\" content=\"3\">" +
            "<title>Industrial AI Demo</title>" +
            "<style>" +
            "body{font-family:monospace;background:#0d1117;color:#c9d1d9;padding:20px}" +
            "h1{color:#58a6ff}" +
            "h2{color:#79c0ff}" +
            ".card{background:#161b22;border:1px solid #30363d;border-radius:6px;padding:16px;margin:12px 0}" +
            ".anomaly{color:#f85149}" +
            ".ok{color:#3fb950}" +
            "table{border-collapse:collapse;width:100%}" +
            "th,td{border:1px solid #30363d;padding:6px 12px;text-align:right}" +
            "th{color:#8b949e}" +
            "</style></head><body>" +
            "<h1>🏭 Industrial AI Demo</h1>" +
            "<p>自動更新（3秒ごと）| Auto-refreshing every 3 seconds</p>" +
            buildStatsCard() +
            buildReadingsTable() +
            buildAnomalyList() +
            "</body></html>";
    }

    private String buildStatsCard() {
        return "<div class=\"card\"><h2>📊 統計 / Statistics</h2>" +
            "<p>Total readings: <strong>" + storage.size() + "</strong></p>" +
            "<p>Total anomalies: <strong class=\"anomaly\">" + anomalies.size() + "</strong></p>" +
            "</div>";
    }

    private String buildReadingsTable() {
        List<SensorReading> latest = storage.getLatest(10);
        StringBuilder sb = new StringBuilder(
            "<div class=\"card\"><h2>🔧 最新センサー値 / Latest Readings (last 10)</h2>" +
            "<table><tr><th>Timestamp</th><th>Temp (°C)</th>" +
            "<th>Pressure (bar)</th><th>Vibration (mm/s)</th></tr>");
        for (int i = latest.size() - 1; i >= 0; i--) {
            SensorReading r = latest.get(i);
            sb.append(String.format(
                "<tr><td>%d</td><td>%.2f</td><td>%.2f</td><td>%.2f</td></tr>",
                r.getTimestamp(), r.getTemperature(), r.getPressure(), r.getVibration()));
        }
        sb.append("</table></div>");
        return sb.toString();
    }

    private String buildAnomalyList() {
        StringBuilder sb = new StringBuilder(
            "<div class=\"card\"><h2>⚠️ 異常検知 / Anomaly Events (last 5)</h2><ul>");
        int start = Math.max(0, anomalies.size() - 5);
        if (anomalies.isEmpty()) {
            sb.append("<li class=\"ok\">No anomalies detected yet.</li>");
        } else {
            for (int i = anomalies.size() - 1; i >= start; i--) {
                sb.append("<li class=\"anomaly\">")
                  .append(anomalies.get(i).describe())
                  .append("</li>");
            }
        }
        sb.append("</ul></div>");
        return sb.toString();
    }

    private void sendResponse(HttpExchange exchange, int status,
                              String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
