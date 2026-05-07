package dev.breezes.settlements.domain.ai.observation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fixed-capacity ring buffer for {@link Observation}s produced during a game tick cycle.
 * <p>
 * When the buffer is full, the oldest entry is silently overwritten. High-volume
 * routine scans are expected to overflow — only notable events should survive to the
 * drain call.
 * <p>
 * {@link #drain()} returns all observations in chronological order and clears the buffer.
 * {@link #peekRecent(int)} returns the {@code n} most recent in most-recent-first order
 * without modifying the buffer.
 */
public class ObservationBuffer {

    public static final int DEFAULT_CAPACITY = 50;

    private final Observation[] buffer;
    private int writeHead;
    private int size;

    public ObservationBuffer() {
        this(DEFAULT_CAPACITY);
    }

    public ObservationBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capacity must be positive");
        }
        this.buffer = new Observation[capacity];
    }

    public void add(Observation observation) {
        this.buffer[this.writeHead % this.buffer.length] = observation;
        this.writeHead++;
        this.size = Math.min(this.size + 1, this.buffer.length);
    }

    /**
     * Removes and returns all observations in chronological (oldest-first) order.
     * The buffer is empty after this call.
     */
    public List<Observation> drain() {
        List<Observation> observations = this.collectChronological();
        this.clear();
        return observations;
    }

    /**
     * Returns up to {@code count} of the most recently added observations in
     * most-recent-first order, without modifying the buffer.
     *
     * @param count maximum number of observations to return
     */
    public List<Observation> peekRecent(int count) {
        if (count <= 0 || this.size == 0) {
            return List.of();
        }

        List<Observation> observations = new ArrayList<>(Math.min(count, this.size));
        int observationsToRead = Math.min(count, this.size);
        for (int offset = 1; offset <= observationsToRead; offset++) {
            int index = Math.floorMod(this.writeHead - offset, this.buffer.length);
            observations.add(this.buffer[index]);
        }
        return Collections.unmodifiableList(observations);
    }

    public int size() {
        return this.size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    private List<Observation> collectChronological() {
        if (this.size == 0) {
            return List.of();
        }

        List<Observation> observations = new ArrayList<>(this.size);
        int oldestIndex = Math.floorMod(this.writeHead - this.size, this.buffer.length);
        for (int offset = 0; offset < this.size; offset++) {
            observations.add(this.buffer[(oldestIndex + offset) % this.buffer.length]);
        }
        return Collections.unmodifiableList(observations);
    }

    private void clear() {
        for (int index = 0; index < this.buffer.length; index++) {
            this.buffer[index] = null;
        }
        this.writeHead = 0;
        this.size = 0;
    }

}
