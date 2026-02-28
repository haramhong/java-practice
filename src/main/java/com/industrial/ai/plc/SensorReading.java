package com.industrial.ai.plc;

/**
 * Immutable snapshot of one PLC sensor reading.
 */
public final class SensorReading {

    private final long   timestamp;
    private final double temperature;   // °C
    private final double pressure;      // bar
    private final double vibration;     // mm/s

    public SensorReading(long timestamp, double temperature,
                         double pressure, double vibration) {
        this.timestamp   = timestamp;
        this.temperature = temperature;
        this.pressure    = pressure;
        this.vibration   = vibration;
    }

    public long   getTimestamp()   { return timestamp;   }
    public double getTemperature() { return temperature; }
    public double getPressure()    { return pressure;    }
    public double getVibration()   { return vibration;   }

    /** Serialise to a simple JSON string (no external library needed). */
    public String toJson() {
        return String.format(
            "{\"timestamp\":%d,\"temperature\":%.2f,\"pressure\":%.2f,\"vibration\":%.2f}",
            timestamp, temperature, pressure, vibration);
    }

    @Override
    public String toString() {
        return String.format(
            "SensorReading{ts=%d, temp=%.2f°C, pressure=%.2f bar, vibration=%.2f mm/s}",
            timestamp, temperature, pressure, vibration);
    }
}
