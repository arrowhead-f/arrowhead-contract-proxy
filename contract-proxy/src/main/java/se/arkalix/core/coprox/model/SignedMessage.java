package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.SignatureBase64;

public interface SignedMessage {
    SignatureBase64 signature();

    byte[] toCanonicalForm();
}
