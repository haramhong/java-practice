package com.industrial.ai;

import com.industrial.ai.career.CareerMapAgent;
import com.industrial.ai.detection.AnomalyDetector;
import com.industrial.ai.detection.AnomalyEvent;
import com.industrial.ai.mqtt.MQTTPublisher;
import com.industrial.ai.plc.PLCSimulator;
import com.industrial.ai.plc.SensorReading;
import com.industrial.ai.storage.CloudStorage;
import com.industrial.ai.web.WebVisualizer;

/**
 * Main – wires all pipeline components together and runs the demo.
 *
 * Pipeline:
 *   PLC simulator → MQTT publisher → Cloud storage → Anomaly detector
 *       ↓
 *   Web visualizer (live dashboard)
 *       ↓
 *   Career map agent (report printed on exit)
 *
 * Run with:
 *   mvn package -q && java -jar target/industrial-ai-demo-1.0.0.jar
 *
 * Environment variables:
 *   MQTT_BROKER   – broker URL (default: tcp://localhost:1883)
 *   STORAGE_FILE  – path for persisted JSON data (default: /tmp/cloud-data.jsonl)
 *   WEB_PORT      – HTTP dashboard port (default: 8080)
 *   SAMPLE_COUNT  – number of PLC samples to collect (default: 50)
 *   SAMPLE_DELAY  – milliseconds between samples (default: 200)
 */
public class Main {

    public static void main(String[] args) throws InterruptedException {

        // ── Configuration ──────────────────────────────────────────────────
        String mqttBroker   = System.getenv().getOrDefault("MQTT_BROKER",   "tcp://localhost:1883");
        String storageFile  = System.getenv().getOrDefault("STORAGE_FILE",  "/tmp/cloud-data.jsonl");
        int    webPort      = Integer.parseInt(System.getenv().getOrDefault("WEB_PORT",      "8080"));
        int    sampleCount  = Integer.parseInt(System.getenv().getOrDefault("SAMPLE_COUNT",  "50"));
        long   sampleDelay  = Long.parseLong  (System.getenv().getOrDefault("SAMPLE_DELAY",  "200"));

        System.out.println("╔══════════════════════════════════════════╗");
        System.out.println("║  🏭 Industrial AI Demo  (java-practice)  ║");
        System.out.println("╚══════════════════════════════════════════╝");

        // ── 1. PLC simulator ───────────────────────────────────────────────
        PLCSimulator plc = new PLCSimulator();
        System.out.println("[PLC] Simulator initialised.");

        // ── 2. MQTT publisher ──────────────────────────────────────────────
        MQTTPublisher mqtt = new MQTTPublisher(mqttBroker);
        mqtt.connect();   // gracefully falls back to mock mode if no broker

        // ── 3. Cloud storage ───────────────────────────────────────────────
        CloudStorage cloud = new CloudStorage(storageFile);
        System.out.println("[Cloud] Storage initialised → " + storageFile);

        // ── 4. Anomaly detector ────────────────────────────────────────────
        AnomalyDetector detector = new AnomalyDetector();
        System.out.println("[AI] Anomaly detector initialised (Z-score threshold=3.0).");

        // ── 5. Web visualizer ──────────────────────────────────────────────
        WebVisualizer web = new WebVisualizer(webPort, cloud, detector.getAnomalies());
        web.start();

        // ── Main data-acquisition loop ────────────────────────────────────
        System.out.printf("%n[Loop] Collecting %d samples every %d ms…%n%n", sampleCount, sampleDelay);

        for (int i = 0; i < sampleCount; i++) {
            // Step 1 – acquire
            SensorReading reading = plc.acquire();

            // Step 2 – transmit via MQTT
            mqtt.publish(reading);

            // Step 3 – store in cloud
            cloud.store(reading);

            // Step 4 – AI anomaly detection
            AnomalyEvent event = detector.analyse(reading);
            if (event != null) {
                System.out.println(event.describe());
            }

            Thread.sleep(sampleDelay);
        }

        System.out.printf("%n[Done] %d readings collected, %d anomalies detected.%n",
            cloud.size(), detector.getAnomalyCount());

        // ── 6. Career map agent ────────────────────────────────────────────
        System.out.println("\n" + "=".repeat(62));
        CareerMapAgent agent = new CareerMapAgent();
        agent.registerDemoSkills();
        System.out.println(agent.renderReport());

        // ── Cleanup ────────────────────────────────────────────────────────
        mqtt.close();
        cloud.close();
        // Keep the web server running for a short while so the user can browse
        System.out.println("[Web] Dashboard at http://localhost:" + webPort + "/ (press Ctrl+C to quit)");
        Runtime.getRuntime().addShutdownHook(new Thread(web::stop));
    }
}
