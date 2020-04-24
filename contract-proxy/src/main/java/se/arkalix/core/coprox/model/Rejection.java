package se.arkalix.core.coprox.model;

import java.time.Instant;

public class Rejection implements Candidate {
    private final Instant createdAt;

    public Rejection() {
        createdAt = Instant.now();
    }

    public Rejection(final Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public Instant createdAt() {
        return createdAt;
    }
}
