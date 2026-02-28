package com.industrial.ai.storage;

import com.industrial.ai.plc.SensorReading;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * CloudStorage – stores {@link SensorReading} records in memory and optionally
 * persists them to a local file that represents the "cloud" back-end.
 *
 * In a production system this class would be replaced by an SDK call to a
 * cloud provider (AWS IoT / Azure IoT Hub / GCP IoT Core).
 */
public class CloudStorage implements AutoCloseable {

    private final List<SensorReading> records = new ArrayList<>();
    private final Path storageFile;
    private PrintWriter fileWriter;

    /**
     * Create storage backed by {@code storagePath}.
     * Pass {@code null} to disable file persistence (in-memory only).
     */
    public CloudStorage(String storagePath) {
        this.storageFile = (storagePath != null) ? Paths.get(storagePath) : null;
        if (storageFile != null) {
            openFileWriter();
        }
    }

    private void openFileWriter() {
        try {
            if (storageFile.getParent() != null) {
                Files.createDirectories(storageFile.getParent());
            }
            fileWriter = new PrintWriter(
                Files.newBufferedWriter(storageFile,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND));
        } catch (IOException e) {
            System.err.println("[Cloud] Failed to open storage file: " + e.getMessage());
        }
    }

    /** Store one reading (in-memory + file). */
    public void store(SensorReading reading) {
        records.add(reading);
        if (fileWriter != null) {
            fileWriter.println(reading.toJson());
            fileWriter.flush();
        }
    }

    /** Return an unmodifiable view of all stored readings. */
    public List<SensorReading> getAll() {
        return Collections.unmodifiableList(records);
    }

    /** Return the most recently stored {@code n} readings. */
    public List<SensorReading> getLatest(int n) {
        int size  = records.size();
        int start = Math.max(0, size - n);
        return Collections.unmodifiableList(records.subList(start, size));
    }

    public int size() {
        return records.size();
    }

    @Override
    public void close() {
        if (fileWriter != null) {
            fileWriter.close();
        }
    }
}
