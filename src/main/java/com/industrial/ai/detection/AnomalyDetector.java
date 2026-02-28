package com.industrial.ai.detection;

import com.industrial.ai.plc.SensorReading;

import java.util.ArrayList;
import java.util.List;

/**
 * AnomalyDetector – detects anomalous sensor readings using the Z-score method.
 *
 * A reading is flagged as an anomaly when any of its sensor channels deviates
 * from the running mean by more than {@code threshold} standard deviations.
 * The running statistics are updated incrementally (Welford's online algorithm)
 * so the detector adapts to slow drift in the process.
 */
public class AnomalyDetector {

    /** Number of Z-score standard deviations above which a value is anomalous. */
    private final double threshold;

    // Welford online statistics for each channel
    private long   n              = 0;
    private double meanTemp       = 0, m2Temp       = 0;
    private double meanPressure   = 0, m2Pressure   = 0;
    private double meanVibration  = 0, m2Vibration  = 0;

    private final List<AnomalyEvent> anomalies = new ArrayList<>();

    public AnomalyDetector(double threshold) {
        this.threshold = threshold;
    }

    /** Default Z-score threshold of 3.0 (≈ 99.7% of normal distribution). */
    public AnomalyDetector() {
        this(3.0);
    }

    /**
     * Analyse one reading. Updates running statistics, then checks each channel.
     *
     * @param reading incoming sensor reading
     * @return an {@link AnomalyEvent} if an anomaly is detected, otherwise null
     */
    public AnomalyEvent analyse(SensorReading reading) {
        updateStats(reading);

        if (n < 2) {
            return null;   // need at least two samples for std-dev
        }

        double stdTemp      = Math.sqrt(m2Temp      / (n - 1));
        double stdPressure  = Math.sqrt(m2Pressure  / (n - 1));
        double stdVibration = Math.sqrt(m2Vibration / (n - 1));

        boolean tempAnomaly      = stdTemp      > 0 && zScore(reading.getTemperature(), meanTemp,      stdTemp)      > threshold;
        boolean pressureAnomaly  = stdPressure  > 0 && zScore(reading.getPressure(),    meanPressure,  stdPressure)  > threshold;
        boolean vibrationAnomaly = stdVibration > 0 && zScore(reading.getVibration(),   meanVibration, stdVibration) > threshold;

        if (tempAnomaly || pressureAnomaly || vibrationAnomaly) {
            AnomalyEvent event = new AnomalyEvent(
                reading, tempAnomaly, pressureAnomaly, vibrationAnomaly);
            anomalies.add(event);
            return event;
        }
        return null;
    }

    public List<AnomalyEvent> getAnomalies() {
        return anomalies;
    }

    public int getAnomalyCount() {
        return anomalies.size();
    }

    public long getSampleCount() {
        return n;
    }

    private void updateStats(SensorReading r) {
        n++;
        double deltaTemp  = r.getTemperature() - meanTemp;
        meanTemp         += deltaTemp / n;
        m2Temp           += deltaTemp * (r.getTemperature() - meanTemp);

        double deltaPres  = r.getPressure() - meanPressure;
        meanPressure     += deltaPres / n;
        m2Pressure       += deltaPres * (r.getPressure() - meanPressure);

        double deltaVib   = r.getVibration() - meanVibration;
        meanVibration    += deltaVib / n;
        m2Vibration      += deltaVib * (r.getVibration() - meanVibration);
    }

    private static double zScore(double value, double mean, double std) {
        return Math.abs((value - mean) / std);
    }
}
