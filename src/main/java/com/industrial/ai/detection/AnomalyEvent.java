package com.industrial.ai.detection;

import com.industrial.ai.plc.SensorReading;

/**
 * Represents one detected anomaly event.
 */
public final class AnomalyEvent {

    private final SensorReading reading;
    private final boolean tempAnomaly;
    private final boolean pressureAnomaly;
    private final boolean vibrationAnomaly;

    public AnomalyEvent(SensorReading reading,
                        boolean tempAnomaly,
                        boolean pressureAnomaly,
                        boolean vibrationAnomaly) {
        this.reading          = reading;
        this.tempAnomaly      = tempAnomaly;
        this.pressureAnomaly  = pressureAnomaly;
        this.vibrationAnomaly = vibrationAnomaly;
    }

    public SensorReading getReading()          { return reading;          }
    public boolean       isTempAnomaly()       { return tempAnomaly;      }
    public boolean       isPressureAnomaly()   { return pressureAnomaly;  }
    public boolean       isVibrationAnomaly()  { return vibrationAnomaly; }

    public String describe() {
        StringBuilder sb = new StringBuilder("[ANOMALY] ");
        if (tempAnomaly)      sb.append("TEMPERATURE ");
        if (pressureAnomaly)  sb.append("PRESSURE ");
        if (vibrationAnomaly) sb.append("VIBRATION ");
        sb.append("→ ").append(reading);
        return sb.toString();
    }

    @Override
    public String toString() {
        return describe();
    }
}
