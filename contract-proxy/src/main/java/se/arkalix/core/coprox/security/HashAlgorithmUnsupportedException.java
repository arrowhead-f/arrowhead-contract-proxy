package se.arkalix.core.coprox.security;

public class HashAlgorithmUnsupportedException extends IllegalArgumentException {
    public HashAlgorithmUnsupportedException(final String name) {
        super(name);
    }

    public HashAlgorithmUnsupportedException(final String name, final Throwable cause) {
        super(name, cause);
    }
}
