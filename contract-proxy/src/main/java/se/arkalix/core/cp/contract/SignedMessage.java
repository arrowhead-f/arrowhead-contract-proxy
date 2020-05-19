package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.SignatureBase64;

public interface SignedMessage {
    SignatureBase64 signature();

    byte[] toCanonicalForm();
}
