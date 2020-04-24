package se.arkalix.core.coprox.security;

import se.arkalix.security.identity.OwnedIdentity;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.Objects;

public final class SignatureScheme {
    private final String id;
    private final String name;

    private SignatureScheme(final String id, final String name) {
        this.id = id;
        this.name = name;
    }

    public static final SignatureScheme RSA_PKCS1_SHA1 = new SignatureScheme("rsa_pkcs1_sha1", "SHA1withRSA");
    public static final SignatureScheme RSA_PKCS1_SHA256 = new SignatureScheme("rsa_pkcs1_sha256", "SHA256withRSA");
    public static final SignatureScheme RSA_PKCS1_SHA384 = new SignatureScheme("rsa_pkcs1_sha384", "SHA384withRSA");
    public static final SignatureScheme RSA_PKCS1_SHA512 = new SignatureScheme("rsa_pkcs1_sha512", "SHA512withRSA");

    public Signature sign(final PrivateKey privateKey, final Instant timestamp, final byte[] data) {
        final java.security.Signature signer;
        try {
            signer = java.security.Signature.getInstance(name);
        }
        catch (final NoSuchAlgorithmException exception) {
            throw new SignatureSchemeUnsupportedException(name, exception);
        }
        try {
            signer.initSign(privateKey);
            signer.update(data);
            return new Signature(timestamp, this, signer.sign());
        }
        catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public Signature sign(final OwnedIdentity identity, final Instant timestamp, final byte[] data) {
        return sign(identity.privateKey(), timestamp, data);
    }

    boolean verify(final PublicKey publicKey, final byte[] signature, final byte[] data) {
        final java.security.Signature verifier;
        try {
            verifier = java.security.Signature.getInstance(name);
        }
        catch (final NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Signature scheme \"" + name +
                "\" unexpectedly unsupported", exception);
        }
        try {
            verifier.initVerify(publicKey);
            verifier.update(data);
            return verifier.verify(signature);
        }
        catch (final Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public static SignatureScheme valueOf(final String string) {
        switch (Objects.requireNonNull(string, "Expected string").toLowerCase()) {
        case "rsa_pkcs1_sha1": return RSA_PKCS1_SHA1;
        case "rsa_pkcs1_sha256": return RSA_PKCS1_SHA256;
        case "rsa_pkcs1_sha384": return RSA_PKCS1_SHA384;
        case "rsa_pkcs1_sha512": return RSA_PKCS1_SHA512;
        }
        throw new IllegalArgumentException("Unsupported signature scheme: " + string);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final SignatureScheme that = (SignatureScheme) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
