package com.industrial.ai.mqtt;

import com.industrial.ai.plc.SensorReading;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.nio.charset.StandardCharsets;

/**
 * MQTTPublisher – publishes {@link SensorReading} payloads to an MQTT broker.
 *
 * If the broker is unavailable the publisher falls back to a mock/offline mode
 * so that the rest of the demo pipeline continues to work without a running
 * broker.
 */
public class MQTTPublisher implements AutoCloseable {

    private static final String DEFAULT_BROKER    = "tcp://localhost:1883";
    private static final String CLIENT_ID         = "industrial-ai-publisher";
    private static final String TOPIC_SENSOR      = "industrial/sensors";
    private static final int    QOS               = 1;

    private final String brokerUrl;
    private MqttClient mqttClient;
    private boolean connected = false;

    public MQTTPublisher() {
        this(DEFAULT_BROKER);
    }

    public MQTTPublisher(String brokerUrl) {
        this.brokerUrl = brokerUrl;
    }

    /**
     * Attempt to connect to the MQTT broker.
     * Returns {@code true} on success, {@code false} when the broker is
     * unreachable (the publisher continues in offline/mock mode).
     */
    public boolean connect() {
        try {
            mqttClient = new MqttClient(brokerUrl, CLIENT_ID, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setConnectionTimeout(3);
            mqttClient.connect(options);
            connected = true;
            System.out.println("[MQTT] Connected to broker: " + brokerUrl);
        } catch (MqttException e) {
            System.out.println("[MQTT] Broker unavailable, running in mock mode. (" + e.getMessage() + ")");
            connected = false;
        }
        return connected;
    }

    /**
     * Publish a sensor reading.
     * In mock mode the payload is simply printed to stdout.
     *
     * @param reading the reading to publish
     */
    public void publish(SensorReading reading) {
        String payload = reading.toJson();
        if (connected && mqttClient != null && mqttClient.isConnected()) {
            try {
                MqttMessage message = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
                message.setQos(QOS);
                mqttClient.publish(TOPIC_SENSOR, message);
            } catch (MqttException e) {
                System.out.println("[MQTT] Publish failed, falling back to mock: " + e.getMessage());
                mockPublish(payload);
            }
        } else {
            mockPublish(payload);
        }
    }

    private void mockPublish(String payload) {
        System.out.println("[MQTT-MOCK] topic=" + TOPIC_SENSOR + " payload=" + payload);
    }

    public boolean isConnected() {
        return connected;
    }

    @Override
    public void close() {
        if (mqttClient != null && mqttClient.isConnected()) {
            try {
                mqttClient.disconnect();
                mqttClient.close();
                System.out.println("[MQTT] Disconnected.");
            } catch (MqttException ignored) {
            }
        }
    }
}
