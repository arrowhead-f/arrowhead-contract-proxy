package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.security.Signature;

import java.time.Instant;
import java.util.Objects;

public class Rejection implements Candidate {
    private final Hash rejectorFingerprint;
    private final Hash receiverFingerprint;
    private final Signature signature;

    public Rejection(final Hash rejectorFingerprint, final Hash receiverFingerprint, final Signature signature) {
        this.rejectorFingerprint = Objects.requireNonNull(rejectorFingerprint, "Expected rejectorFingerprint");
        this.receiverFingerprint = Objects.requireNonNull(receiverFingerprint, "Expected receiverFingerprint");
        this.signature = Objects.requireNonNull(signature, "Expected signature");
    }

    public Hash rejectorFingerprint() {
        return rejectorFingerprint;
    }

    public Hash receiverFingerprint() {
        return receiverFingerprint;
    }

    public Signature signature() {
        return signature;
    }

    @Override
    public String type() {
        return "rejection";
    }

    @Override
    public Instant createdAt() {
        return signature.timestamp();
    }

    @Override
    public boolean isAcceptableAt(final Instant instant) {
        return false;
    }

    @Override
    public boolean isClosedAt(final Instant instant) {
        return false;
    }
}
