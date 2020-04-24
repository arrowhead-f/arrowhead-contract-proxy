package se.arkalix.core.coprox.security;

public class SignatureSchemeUnsupportedException extends IllegalArgumentException {
    public SignatureSchemeUnsupportedException(final String name, final Throwable cause) {
        super(name, cause);
    }
}
