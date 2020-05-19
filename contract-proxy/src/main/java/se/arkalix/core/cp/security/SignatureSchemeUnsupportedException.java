package se.arkalix.core.cp.security;

import se.arkalix.core.cp.util.UnsatisfiableRequestException;

public class SignatureSchemeUnsupportedException extends UnsatisfiableRequestException {
    public SignatureSchemeUnsupportedException(final String name, final Throwable cause) {
        super("UNSUPPORTED_SIGNATURE_SCHEME", name, cause);
    }
}
