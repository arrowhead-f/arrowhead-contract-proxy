package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashAlgorithm;
import se.arkalix.core.coprox.security.Signature;
import se.arkalix.core.coprox.security.SignatureScheme;

import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public class OwnedParty extends Party {
    private final PrivateKey privateKey;
    private final SignatureScheme signatureScheme;

    public OwnedParty(
        final Certificate certificate,
        final PrivateKey privateKey,
        final Set<HashAlgorithm> supportedHashAlgorithms)
    {
        super(certificate, supportedHashAlgorithms);
        this.privateKey = Objects.requireNonNull(privateKey, "Expected privateKey");

        signatureScheme = SignatureScheme.ALL
            .stream()
            .filter(scheme -> privateKey.getAlgorithm().equalsIgnoreCase(scheme.keyAlgorithmName()) &&
                supportedHashAlgorithms.contains(scheme.hashAlgorithm()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Expected " +
                "privateKey to support at least one known signature scheme" +
                "that relies on one of trustedHashAlgorithms; the known " +
                "signature schemes are " + SignatureScheme.ALL));
    }

    public Signature sign(final Instant timestamp, final byte[] bytes) {
        return signatureScheme.sign(privateKey, timestamp, bytes);
    }

    public SignatureScheme signatureScheme() {
        return signatureScheme;
    }
}
