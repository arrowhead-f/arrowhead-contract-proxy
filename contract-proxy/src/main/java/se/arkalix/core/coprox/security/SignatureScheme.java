package se.arkalix.core.coprox.security;

import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public final class SignatureScheme {
    private final String ianaId;
    private final String javaId;
    private final String keyAlgorithmName;
    private final HashAlgorithm hashAlgorithm;

    private SignatureScheme(
        final String ianaId,
        final String javaId,
        final String keyAlgorithmName,
        final HashAlgorithm hashAlgorithm)
    {
        this.ianaId = ianaId;
        this.javaId = javaId;
        this.keyAlgorithmName = keyAlgorithmName;
        this.hashAlgorithm = hashAlgorithm;
    }

    public static final SignatureScheme ECDSA_SHA1 = new SignatureScheme(
        "ecdsa_sha1", "SHA1withECDSA", "EC", HashAlgorithm.SHA_1);
    public static final SignatureScheme ECDSA_SECP256R1_SHA256 = new SignatureScheme(
        "ecdsa_secp256r1_sha256", "SHA256withECDSA", "EC", HashAlgorithm.SHA_256);
    public static final SignatureScheme ECDSA_SECP384R1_SHA384 = new SignatureScheme(
        "ecdsa_secp384r1_sha384", "SHA384withECDSA", "EC", HashAlgorithm.SHA_384);
    public static final SignatureScheme ECDSA_SECP521R1_SHA512 = new SignatureScheme(
        "ecdsa_secp521r1_sha512", "SHA512withECDSA", "EC", HashAlgorithm.SHA_512);
    public static final SignatureScheme RSA_PKCS1_SHA1 = new SignatureScheme(
        "rsa_pkcs1_sha1", "SHA1withRSA", "RSA", HashAlgorithm.SHA_1);
    public static final SignatureScheme RSA_PKCS1_SHA256 = new SignatureScheme(
        "rsa_pkcs1_sha256", "SHA256withRSA", "RSA", HashAlgorithm.SHA_256);
    public static final SignatureScheme RSA_PKCS1_SHA384 = new SignatureScheme(
        "rsa_pkcs1_sha384", "SHA384withRSA", "RSA", HashAlgorithm.SHA_384);
    public static final SignatureScheme RSA_PKCS1_SHA512 = new SignatureScheme(
        "rsa_pkcs1_sha512", "SHA512withRSA", "RSA", HashAlgorithm.SHA_512);

    public static final List<SignatureScheme> ALL = List.of(
        ECDSA_SHA1,
        ECDSA_SECP256R1_SHA256,
        ECDSA_SECP384R1_SHA384,
        ECDSA_SECP521R1_SHA512,
        RSA_PKCS1_SHA1,
        RSA_PKCS1_SHA256,
        RSA_PKCS1_SHA384,
        RSA_PKCS1_SHA512);

    public Signature sign(final PrivateKey privateKey, final Instant timestamp, final byte[] data) {
        final java.security.Signature signer;
        try {
            signer = java.security.Signature.getInstance(javaId);
        }
        catch (final NoSuchAlgorithmException exception) {
            throw new SignatureSchemeUnsupportedException(ianaId, exception);
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

    boolean verify(final PublicKey publicKey, final byte[] signature, final byte[] data) {
        final java.security.Signature verifier;
        try {
            verifier = java.security.Signature.getInstance(javaId);
        }
        catch (final NoSuchAlgorithmException exception) {
            throw new SignatureSchemeUnsupportedException(ianaId, exception);
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

    public String keyAlgorithmName() {
        return keyAlgorithmName;
    }

    public HashAlgorithm hashAlgorithm() {
        return hashAlgorithm;
    }

    public static SignatureScheme valueOf(final String string) {
        switch (Objects.requireNonNull(string, "Expected string").toLowerCase()) {
        case "ecdsa_sha1": return ECDSA_SHA1;
        case "ecdsa_secp256r1_sha256": return ECDSA_SECP256R1_SHA256;
        case "ecdsa_secp384r1_sha384": return ECDSA_SECP384R1_SHA384;
        case "ecdsa_secp521r1_sha512": return ECDSA_SECP521R1_SHA512;
        case "rsa_pkcs1_sha1": return RSA_PKCS1_SHA1;
        case "rsa_pkcs1_sha256": return RSA_PKCS1_SHA256;
        case "rsa_pkcs1_sha384": return RSA_PKCS1_SHA384;
        case "rsa_pkcs1_sha512": return RSA_PKCS1_SHA512;
        default:
            throw new IllegalArgumentException("Unsupported signature scheme: " + string);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final SignatureScheme that = (SignatureScheme) other;
        return ianaId.equals(that.ianaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ianaId);
    }

    @Override
    public String toString() {
        return ianaId;
    }
}
