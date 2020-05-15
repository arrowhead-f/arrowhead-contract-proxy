package se.arkalix.core.coprox.model;

public class ModelException extends Exception {
    private final String type;

    public ModelException(final String type, final String message) {
        super(message, null, true, false);
        this.type = type;
        if (type == null || type.length() == 0) {
            final var exception = new IllegalArgumentException("Expected type");
            exception.addSuppressed(this);
            throw exception;
        }
    }

    public ModelException(final String type, final String message, final Throwable cause) {
        super(message, cause, true, false);
        this.type = type;
        if (type == null || type.length() == 0) {
            final var exception = new IllegalArgumentException("Expected type");
            if (cause != null) {
                exception.addSuppressed(cause);
            }
            exception.addSuppressed(this);
            throw exception;
        }
    }

    public String type() {
        return type;
    }
}
