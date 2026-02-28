package com.industrial.ai.plc;

import java.util.Random;

/**
 * PLCSimulator – simulates sensor data from a Programmable Logic Controller.
 *
 * Produces readings for three channels that are common in industrial settings:
 *   - temperature  (°C)
 *   - pressure     (bar)
 *   - vibration    (mm/s)
 *
 * Normal operating ranges are modelled with Gaussian noise. Occasional spikes
 * simulate real faults that the anomaly detector should catch.
 */
public class PLCSimulator {

    private static final double TEMP_MEAN      = 75.0;
    private static final double TEMP_STD       = 2.0;
    private static final double PRESSURE_MEAN  = 5.0;
    private static final double PRESSURE_STD   = 0.3;
    private static final double VIBRATION_MEAN = 1.5;
    private static final double VIBRATION_STD  = 0.1;

    /** Probability of injecting a fault spike on any given reading. */
    private static final double FAULT_PROBABILITY = 0.05;

    private final Random random;
    private long timestamp;

    public PLCSimulator() {
        this.random    = new Random();
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * Acquire one sensor reading from the (simulated) PLC.
     *
     * @return a {@link SensorReading} snapshot
     */
    public SensorReading acquire() {
        timestamp = System.currentTimeMillis();

        double temperature = gaussian(TEMP_MEAN, TEMP_STD);
        double pressure    = gaussian(PRESSURE_MEAN, PRESSURE_STD);
        double vibration   = gaussian(VIBRATION_MEAN, VIBRATION_STD);

        // Randomly inject fault spikes to test the anomaly detector
        if (random.nextDouble() < FAULT_PROBABILITY) {
            temperature += TEMP_MEAN * 0.4;   // 40% spike
        }
        if (random.nextDouble() < FAULT_PROBABILITY) {
            pressure += PRESSURE_MEAN * 0.6;
        }
        if (random.nextDouble() < FAULT_PROBABILITY) {
            vibration += VIBRATION_MEAN * 3.0;
        }

        return new SensorReading(timestamp, temperature, pressure, vibration);
    }

    private double gaussian(double mean, double std) {
        return mean + random.nextGaussian() * std;
    }
}
