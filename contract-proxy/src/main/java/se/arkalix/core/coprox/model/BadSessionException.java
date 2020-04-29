package se.arkalix.core.coprox.model;

public class BadSessionException extends BadRequestException {
    private final long sessionId;

    public BadSessionException(final long sessionId) {
        super("Session " + sessionId + " has already closed");
        this.sessionId = sessionId;
    }

    public long sessionId() {
        return sessionId;
    }
}
