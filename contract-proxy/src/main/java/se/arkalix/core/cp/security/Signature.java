package se.arkalix.core.cp.security;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Objects;

public class Signature {
    private final Instant timestamp;
    private final SignatureScheme scheme;
    private final byte[] sum;

    public Signature(final Instant timestamp, final SignatureScheme scheme, final byte[] sum) {
        this.timestamp = Objects.requireNonNull(timestamp, "Expected timestamp");
        this.scheme = Objects.requireNonNull(scheme, "Expected scheme");
        this.sum = Objects.requireNonNull(sum, "Expected sum");
    }

    public Instant timestamp() {
        return timestamp;
    }

    public SignatureScheme scheme() {
        return scheme;
    }

    public byte[] sum() {
        return sum;
    }

    public boolean verify(final PublicKey publicKey, final byte[] data) {
        return scheme.verify(publicKey, sum, data);
    }

    public boolean verify(final Certificate certificate, final byte[] data) {
        return verify(certificate.getPublicKey(), data);
    }
}
