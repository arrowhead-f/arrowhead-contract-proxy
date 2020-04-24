package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.security.HashFunction;
import se.arkalix.core.coprox.security.Signature;
import se.arkalix.security.identity.TrustedIdentity;

import java.security.cert.CertificateEncodingException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Offer implements Candidate {
    private final Hash offerorFingerprint;
    private final Hash receiverFingerprint;
    private final Instant validAfter;
    private final Instant validUntil;
    private final List<Contract> contracts;
    private final Signature signature;

    public Offer(final Builder builder) {
        offerorFingerprint = Objects.requireNonNull(builder.offerorFingerprint, "Expected offerorFingerprint");
        receiverFingerprint = Objects.requireNonNull(builder.receiverFingerprint, "Expected receiverFingerprint");
        validAfter = Objects.requireNonNullElseGet(builder.validAfter, Instant::now);
        validUntil = Objects.requireNonNull(builder.validUntil, "Expected validUntil");
        contracts = Collections.unmodifiableList(Objects.requireNonNull(builder.contracts, "Expected contracts"));
        if (contracts.size() == 0) {
            throw new IllegalArgumentException("contracts.size() == 0");
        }
        signature = Objects.requireNonNull(builder.signature, "Expected signature");
    }

    public Hash offerorFingerprint() {
        return offerorFingerprint;
    }

    public Hash receiverFingerprint() {
        return receiverFingerprint;
    }

    public Instant validAfter() {
        return validAfter;
    }

    public Instant validUntil() {
        return validUntil;
    }

    public List<Contract> contracts() {
        return contracts;
    }

    public Signature signature() {
        return signature;
    }

    public boolean isAcceptable() {
        final var now = Instant.now();
        return validAfter.isBefore(now) && validUntil.isAfter(now);
    }

    @Override
    public Instant createdAt() {
        return signature.timestamp();
    }

    public static class Builder {
        private Hash offerorFingerprint;
        private Hash receiverFingerprint;
        private Instant validAfter;
        private Instant validUntil;
        private List<Contract> contracts;
        private Signature signature;

        public Builder offerorFingerprint(final Hash offerorFingerprint) {
            this.offerorFingerprint = offerorFingerprint;
            return this;
        }

        public Builder offerorFingerprint(final TrustedIdentity identity) {
            try {
                return offerorFingerprint(HashFunction.SHA256.hash(identity.certificate().getEncoded()));
            }
            catch (final CertificateEncodingException exception) {
                throw new RuntimeException(exception);
            }
        }

        public Builder receiverFingerprint(final Hash receiverFingerprint) {
            this.receiverFingerprint = receiverFingerprint;
            return this;
        }

        public Builder receiverFingerprint(final TrustedIdentity identity) {
            try {
                return receiverFingerprint(HashFunction.SHA256.hash(identity.certificate().getEncoded()));
            }
            catch (final CertificateEncodingException exception) {
                throw new RuntimeException(exception);
            }
        }

        public Builder validAfter(final Instant validAfter) {
            this.validAfter = validAfter;
            return this;
        }

        public Builder validUntil(final Instant validUntil) {
            this.validUntil = validUntil;
            return this;
        }

        public Builder contracts(final List<Contract> contracts) {
            this.contracts = contracts;
            return this;
        }

        public Builder signature(final Signature signature) {
            this.signature = signature;
            return this;
        }

        public Offer build() {
            return new Offer(this);
        }
    }
}
