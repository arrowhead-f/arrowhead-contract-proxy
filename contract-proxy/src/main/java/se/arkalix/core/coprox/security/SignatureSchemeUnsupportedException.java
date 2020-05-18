package se.arkalix.core.coprox.security;

import se.arkalix.core.coprox.util.UnsatisfiableRequestException;

public class SignatureSchemeUnsupportedException extends UnsatisfiableRequestException {
    public SignatureSchemeUnsupportedException(final String name, final Throwable cause) {
        super("UNSUPPORTED_SIGNATURE_SCHEME", name, cause);
    }
}
