package se.arkalix.core.coprox;

import se.arkalix.ArSystem;
import se.arkalix.core.coprox.model.Publisher;
import se.arkalix.core.plugin.ArEventPublish;

import java.util.HashMap;
import java.util.Objects;

public class ContractNegotiationPublisher implements Publisher {
    public static final String TOPIC = "coprox";

    private final ArSystem system;
    private final ArEventPublish eventPublish;

    public ContractNegotiationPublisher(final ArSystem system, final ArEventPublish eventPublish) {
        this.system = Objects.requireNonNull(system, "Expected system");
        this.eventPublish = Objects.requireNonNull(eventPublish, "Expected eventPublish");
    }

    @Override
    public void onOffer(
        final long sessionId,
        final String offerorName,
        final String receiverName,
        final String templateName)
    {
        onEvent(sessionId, offerorName, receiverName, templateName, "offer");
    }

    @Override
    public void onAccept(
        final long sessionId,
        final String offerorName,
        final String receiverName,
        final String templateName)
    {
        onEvent(sessionId, offerorName, receiverName, templateName, "accept");
    }

    @Override
    public void onReject(
        final long sessionId,
        final String offerorName,
        final String receiverName,
        final String templateName)
    {
        onEvent(sessionId, offerorName, receiverName, templateName, "reject");
    }

    private void onEvent(
        final long sessionId,
        final String offerorName,
        final String receiverName,
        final String templateName,
        final String action)
    {
        final var metadata = new HashMap<String, String>();
        metadata.put("offeror", offerorName);
        metadata.put("receiver", receiverName);
        metadata.put("template", templateName);
        metadata.put("action", action);
        eventPublish.publish(TOPIC, system, metadata, Long.toString(sessionId));
    }
}
