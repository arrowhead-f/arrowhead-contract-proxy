package se.arkalix.core.coprox.util;

public class UnsatisfiableRequestException extends RuntimeException {
    private final String type;

    public UnsatisfiableRequestException(final String type, final String message) {
        super(message, null, true, false);
        this.type = type;
        if (type == null || type.length() == 0) {
            final var exception = new IllegalArgumentException("Expected type");
            exception.addSuppressed(this);
            throw exception;
        }
    }

    public UnsatisfiableRequestException(final String type, final String message, final Throwable cause) {
        super(message, cause, true, false);
        this.type = type;
        if (type == null || type.length() == 0) {
            final var exception = new IllegalArgumentException("Expected type");
            exception.addSuppressed(this);
            throw exception;
        }
    }

    public String type() {
        return type;
    }
}
