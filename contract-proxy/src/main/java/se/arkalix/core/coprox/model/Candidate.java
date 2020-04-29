package se.arkalix.core.coprox.model;

import java.time.Instant;

public interface Candidate {
    String type();
    Instant createdAt();
}
