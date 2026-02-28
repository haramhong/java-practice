package com.industrial.ai;

import com.industrial.ai.career.CareerMapAgent;
import com.industrial.ai.detection.AnomalyDetector;
import com.industrial.ai.detection.AnomalyEvent;
import com.industrial.ai.plc.PLCSimulator;
import com.industrial.ai.plc.SensorReading;
import com.industrial.ai.storage.CloudStorage;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the Industrial AI Demo pipeline.
 */
class IndustrialAIDemoTest {

    // ── SensorReading ─────────────────────────────────────────────────────

    @Test
    void testSensorReadingToJson() {
        SensorReading r = new SensorReading(1000L, 75.5, 5.1, 1.4);
        String json = r.toJson();
        assertTrue(json.contains("\"timestamp\":1000"));
        assertTrue(json.contains("\"temperature\":75.50"));
        assertTrue(json.contains("\"pressure\":5.10"));
        assertTrue(json.contains("\"vibration\":1.40"));
    }

    @Test
    void testSensorReadingGetters() {
        SensorReading r = new SensorReading(42L, 80.0, 6.0, 2.0);
        assertEquals(42L,  r.getTimestamp());
        assertEquals(80.0, r.getTemperature(), 0.001);
        assertEquals(6.0,  r.getPressure(),    0.001);
        assertEquals(2.0,  r.getVibration(),   0.001);
    }

    // ── PLCSimulator ──────────────────────────────────────────────────────

    @Test
    void testPLCSimulatorProducesReadings() {
        PLCSimulator plc = new PLCSimulator();
        for (int i = 0; i < 20; i++) {
            SensorReading r = plc.acquire();
            assertNotNull(r);
            // values should be in a reasonable range (normal + possible spike)
            assertTrue(r.getTemperature() > 0 && r.getTemperature() < 200,
                "Temperature out of expected range: " + r.getTemperature());
            assertTrue(r.getPressure() > 0 && r.getPressure() < 50,
                "Pressure out of expected range: " + r.getPressure());
            assertTrue(r.getVibration() >= 0,
                "Vibration must be non-negative: " + r.getVibration());
        }
    }

    // ── CloudStorage ──────────────────────────────────────────────────────

    @Test
    void testCloudStorageStoreAndRetrieve() {
        CloudStorage storage = new CloudStorage(null);   // in-memory only
        assertEquals(0, storage.size());

        SensorReading r1 = new SensorReading(1L, 70.0, 5.0, 1.5);
        SensorReading r2 = new SensorReading(2L, 71.0, 5.1, 1.6);
        storage.store(r1);
        storage.store(r2);

        assertEquals(2, storage.size());
        List<SensorReading> all = storage.getAll();
        assertEquals(2, all.size());
    }

    @Test
    void testCloudStorageGetLatest() {
        CloudStorage storage = new CloudStorage(null);
        for (int i = 0; i < 10; i++) {
            storage.store(new SensorReading(i, 70.0 + i, 5.0, 1.5));
        }
        List<SensorReading> latest = storage.getLatest(3);
        assertEquals(3, latest.size());
        assertEquals(9, latest.get(2).getTimestamp());   // most recent last
    }

    @Test
    void testCloudStorageGetLatestWhenFewerRecords() {
        CloudStorage storage = new CloudStorage(null);
        storage.store(new SensorReading(1L, 70.0, 5.0, 1.5));
        // requesting 5 but only 1 stored
        List<SensorReading> latest = storage.getLatest(5);
        assertEquals(1, latest.size());
    }

    // ── AnomalyDetector ───────────────────────────────────────────────────

    @Test
    void testAnomalyDetectorNoAnomalyForNormalData() {
        AnomalyDetector detector = new AnomalyDetector(3.0);
        // Feed 50 near-constant readings to build stable statistics
        for (int i = 0; i < 50; i++) {
            SensorReading r = new SensorReading(i, 75.0, 5.0, 1.5);
            detector.analyse(r);
        }
        assertEquals(0, detector.getAnomalyCount(),
            "No anomalies expected for constant normal data");
    }

    @Test
    void testAnomalyDetectorDetectsSpikedTemperature() {
        AnomalyDetector detector = new AnomalyDetector(2.0);
        // Warm-up with 30 stable readings
        for (int i = 0; i < 30; i++) {
            detector.analyse(new SensorReading(i, 75.0, 5.0, 1.5));
        }
        // Inject a very large spike
        AnomalyEvent event = detector.analyse(new SensorReading(31, 200.0, 5.0, 1.5));
        assertNotNull(event, "Spike should be detected as anomaly");
        assertTrue(event.isTempAnomaly(), "Temperature should be flagged");
    }

    @Test
    void testAnomalyDetectorSampleCountIncreases() {
        AnomalyDetector detector = new AnomalyDetector();
        for (int i = 0; i < 5; i++) {
            detector.analyse(new SensorReading(i, 75.0, 5.0, 1.5));
        }
        assertEquals(5, detector.getSampleCount());
    }

    // ── CareerMapAgent ────────────────────────────────────────────────────

    @Test
    void testCareerMapAgentRegistersSkills() {
        CareerMapAgent agent = new CareerMapAgent();
        agent.registerDemoSkills();
        assertFalse(agent.getAcquiredSkills().isEmpty(),
            "Agent should register demo skills");
    }

    @Test
    void testCareerMapAgentGeneratesRecommendations() {
        CareerMapAgent agent = new CareerMapAgent();
        agent.registerDemoSkills();
        List<CareerMapAgent.CareerRecommendation> recs = agent.generateCareerMap();
        assertFalse(recs.isEmpty(), "Should produce at least one recommendation");
        // Ensure sorted descending by score
        for (int i = 0; i + 1 < recs.size(); i++) {
            assertTrue(recs.get(i).matchScore() >= recs.get(i + 1).matchScore(),
                "Recommendations must be sorted by matchScore descending");
        }
    }

    @Test
    void testCareerMapAgentReportContainsExpectedSections() {
        CareerMapAgent agent = new CareerMapAgent();
        agent.registerDemoSkills();
        String report = agent.renderReport();
        assertTrue(report.contains("転職キャリアマップ"),   "Report must contain Japanese title");
        assertTrue(report.contains("Acquired Skills"),     "Report must list acquired skills");
        assertTrue(report.contains("Career Recommendations"), "Report must contain recommendations");
        assertTrue(report.contains("Next Actions"),        "Report must contain next actions");
    }

    @Test
    void testCareerMapAgentCustomSkill() {
        CareerMapAgent agent = new CareerMapAgent();
        agent.registerDemoSkills();
        agent.addSkill(new CareerMapAgent.Skill("Python", "Programming", 2));
        boolean found = agent.getAcquiredSkills().stream()
            .anyMatch(s -> "Python".equals(s.name()));
        assertTrue(found, "Custom skill should be added");
    }
}
