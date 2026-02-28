# 🏭 Industrial AI Demo

A self-contained Java demonstration of an end-to-end **Industrial IoT + AI** pipeline, and a **career-change career map generation agent** (転職キャリアマップ生成エージェント) built on top of it.

## Pipeline Overview

```
PLC (simulated)
    │  データ取得 / Data Acquisition
    ▼
MQTT Publisher  ──► (falls back to mock if no broker)
    │  MQTT送信 / MQTT Transmission
    ▼
Cloud Storage  (in-memory + JSONL file)
    │  Cloud保存 / Cloud Storage
    ▼
Anomaly Detector  (Z-score, Welford online stats)
    │  AI異常検知 / AI Anomaly Detection
    ▼
Web Dashboard  http://localhost:8080/
    │  Web可視化 / Web Visualization
    ▼
Career Map Agent
    転職キャリアマップ生成 / Career Transition Map Generation
```

## Requirements

| Tool  | Version |
|-------|---------|
| Java  | 17+     |
| Maven | 3.6+    |

## Quick Start

```bash
# Build
mvn package -q

# Run (defaults: 50 samples, 200 ms interval, port 8080)
java -jar target/industrial-ai-demo-1.0.0.jar
```

Open http://localhost:8080/ in a browser to see the live sensor dashboard.

## Configuration (environment variables)

| Variable       | Default                    | Description                     |
|----------------|----------------------------|---------------------------------|
| `MQTT_BROKER`  | `tcp://localhost:1883`     | MQTT broker URL                 |
| `STORAGE_FILE` | `/tmp/cloud-data.jsonl`    | File path for persisted data    |
| `WEB_PORT`     | `8080`                     | HTTP dashboard port             |
| `SAMPLE_COUNT` | `50`                       | Number of PLC samples to collect|
| `SAMPLE_DELAY` | `200`                      | Milliseconds between samples    |

## Run Tests

```bash
mvn test
```

## Modules

| Package                          | Class               | Role                                            |
|----------------------------------|---------------------|-------------------------------------------------|
| `com.industrial.ai.plc`          | `PLCSimulator`      | Generates synthetic temp/pressure/vibration data|
| `com.industrial.ai.plc`          | `SensorReading`     | Immutable sensor snapshot with JSON serialiser  |
| `com.industrial.ai.mqtt`         | `MQTTPublisher`     | Publishes readings; falls back to mock mode     |
| `com.industrial.ai.storage`      | `CloudStorage`      | In-memory + JSONL file persistence              |
| `com.industrial.ai.detection`    | `AnomalyDetector`   | Z-score anomaly detection (Welford algorithm)   |
| `com.industrial.ai.detection`    | `AnomalyEvent`      | Describes one detected anomaly                  |
| `com.industrial.ai.web`          | `WebVisualizer`     | Embedded HTTP dashboard (no external framework) |
| `com.industrial.ai.career`       | `CareerMapAgent`    | Career-change career map generation agent       |
| `com.industrial.ai`              | `Main`              | Wires all components and runs the demo          |

## Career Map Agent (転職キャリアマップ生成エージェント)

After the data-collection loop finishes the agent:

1. **Registers** all skills demonstrated by the pipeline (Java, MQTT/IoT, PLC/OT, Cloud, Statistical AI, Web, DevOps, …).
2. **Scores** each role in a catalogue against the skill set using a match-percentage metric.
3. **Prints** a bilingual (Japanese/English) ranked report with skill gaps and next actions.

Sample output:

```
■ キャリア推薦 / Career Recommendations

  IoT / OT-IT Engineer           適合度: 100%  難易度: 低 / Low
    → ✅ Perfect skill match!

  ML Engineer (Industrial AI)    適合度:  80%  難易度: 中 / Medium
    → 不足スキル / Skill gaps: Python
```