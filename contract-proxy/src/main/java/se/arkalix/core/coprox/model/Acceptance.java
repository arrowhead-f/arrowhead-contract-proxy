package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Signature;

import java.time.Instant;

public class Acceptance implements Candidate {
    private final Offer offer;
    private final Signature signature;

    public Acceptance(final Offer offer, final Signature signature) {
        this.offer = offer;
        this.signature = signature;
    }

    public Offer offer() {
        return offer;
    }

    public Signature signature() {
        return signature;
    }

    @Override
    public String type() {
        return "acceptance";
    }

    @Override
    public Instant createdAt() {
        return signature.timestamp();
    }
}
