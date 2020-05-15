package se.arkalix.core.coprox;

import se.arkalix.ArSystem;
import se.arkalix.core.coprox.model.ContractSessionEvent;
import se.arkalix.core.coprox.model.ContractSessionObserver;
import se.arkalix.core.plugin.cp.ArContractNegotiationConstants;
import se.arkalix.core.plugin.eh.ArEventPublish;

import java.util.HashMap;
import java.util.Objects;

public class ContractNegotiationPublisher implements ContractSessionObserver {
    private final ArSystem system;
    private final ArEventPublish eventPublish;

    public ContractNegotiationPublisher(final ArSystem system, final ArEventPublish eventPublish) {
        this.system = Objects.requireNonNull(system, "Expected system");
        this.eventPublish = Objects.requireNonNull(eventPublish, "Expected eventPublish");
    }

    @Override
    public void onEvent(final ContractSessionEvent event) {
        final var metadata = new HashMap<String, String>();
        metadata.put("offeror", event.offerorName());
        metadata.put("receiver", event.receiverName());
        metadata.put("template", String.join(",", event.templateNames()));
        metadata.put("status", event.status().toString());
        eventPublish.publish(ArContractNegotiationConstants.TOPIC_SESSION_UPDATE,
            system, metadata, Long.toString(event.sessionId()));
    }
}
