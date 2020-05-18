package se.arkalix.core.coprox;

import se.arkalix.ArSystem;
import se.arkalix.core.coprox.model.ContractNegotiationEvent;
import se.arkalix.core.coprox.model.ContractNegotiationObserver;
import se.arkalix.core.plugin.cp.ContractNegotiationConstants;
import se.arkalix.core.plugin.eh.ArEventPublishService;

import java.util.HashMap;
import java.util.Objects;

public class ContractNegotiationPublisher implements ContractNegotiationObserver {
    private final ArSystem system;
    private final ArEventPublishService eventPublish;

    public ContractNegotiationPublisher(final ArSystem system, final ArEventPublishService eventPublish) {
        this.system = Objects.requireNonNull(system, "Expected system");
        this.eventPublish = Objects.requireNonNull(eventPublish, "Expected eventPublish");
    }

    @Override
    public void onEvent(final ContractNegotiationEvent event) {
        final var metadata = new HashMap<String, String>();
        metadata.put("offeror", event.offerorName());
        metadata.put("receiver", event.receiverName());
        metadata.put("template", String.join(",", event.templateNames()));
        metadata.put("status", event.status().toString());
        eventPublish.publish(ContractNegotiationConstants.TOPIC_UPDATE,
            system, metadata, Long.toString(event.negotiationId()));
    }
}
