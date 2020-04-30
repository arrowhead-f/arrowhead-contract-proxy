package se.arkalix.core.coprox.model;

import java.time.Instant;
import java.util.Objects;

public class Session {
    private final long id;
    private final Candidate candidate;

    public Session(final long id, final Candidate candidate) {
        if (id <= 0) {
            throw new IllegalArgumentException("id <= 0");
        }
        this.id = id;
        this.candidate = Objects.requireNonNull(candidate, "Expected candidate");
    }

    public boolean isAcceptableAt(final Instant instant) {
        return candidate.isAcceptableAt(instant);
    }

    public boolean isClosedAt(final Instant instant) {
        return candidate.isClosedAt(instant);
    }

    public long id() {
        return id;
    }

    public Candidate candidate() {
        return candidate;
    }
}
