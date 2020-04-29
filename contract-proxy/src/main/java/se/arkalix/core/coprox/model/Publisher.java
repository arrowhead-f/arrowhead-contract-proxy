package se.arkalix.core.coprox.model;

public interface Publisher {
    void onOffer(final long sessionId, final String offerorName, final String receiverName, final String templateName);

    void onAccept(final long sessionId, final String offerorName, final String receiverName, final String templateName);

    void onReject(final long sessionId, final String offerorName, final String receiverName, final String templateName);
}
