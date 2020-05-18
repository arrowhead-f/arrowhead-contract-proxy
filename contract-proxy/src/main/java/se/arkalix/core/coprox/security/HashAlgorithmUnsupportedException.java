package se.arkalix.core.coprox.security;

import se.arkalix.core.coprox.util.UnsatisfiableRequestException;

public class HashAlgorithmUnsupportedException extends UnsatisfiableRequestException {
    public HashAlgorithmUnsupportedException(final String name) {
        super("UNSUPPORTED_HASH_ALGORITHM", name);
    }

    public HashAlgorithmUnsupportedException(final String name, final Throwable cause) {
        super("UNSUPPORTED_HASH_ALGORITHM", name, cause);
    }
}
