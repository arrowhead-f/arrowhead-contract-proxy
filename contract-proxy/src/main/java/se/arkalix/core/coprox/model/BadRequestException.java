package se.arkalix.core.coprox.model;

public class BadRequestException extends Exception {
    public BadRequestException(final String message) {
        super(message);
    }
}
