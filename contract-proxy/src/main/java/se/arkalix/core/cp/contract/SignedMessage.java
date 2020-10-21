package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.SignatureBase64;

import java.security.PublicKey;
import java.security.cert.Certificate;

public interface SignedMessage {
    SignatureBase64 signature();

    default boolean verify(final Certificate certificate) {
        return signature().verify(certificate, canonicalizeWithoutSignatureSum());
    }

    default boolean verify(final PublicKey publicKey) {
        return signature().verify(publicKey, canonicalizeWithoutSignatureSum());
    }

    byte[] canonicalizeWithoutSignatureSum();
}
