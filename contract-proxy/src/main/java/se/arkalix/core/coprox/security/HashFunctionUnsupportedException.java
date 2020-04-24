package se.arkalix.core.coprox.security;

public class HashFunctionUnsupportedException extends IllegalArgumentException {
    public HashFunctionUnsupportedException(final String name, final Throwable cause) {
        super(name, cause);
    }
}
