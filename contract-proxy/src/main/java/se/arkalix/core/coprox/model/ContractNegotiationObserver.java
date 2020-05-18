package se.arkalix.core.coprox.model;

import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.TrustedContract;
import se.arkalix.core.plugin.cp.TrustedContractOffer;

import java.util.stream.Collectors;

@FunctionalInterface
public interface ContractNegotiationObserver {
    void onEvent(ContractNegotiationEvent event);

    default void onEvent(
        final long negotiationId,
        final TrustedContractOffer offer,
        final ContractNegotiationStatus status)
    {
        onEvent(new ContractNegotiationEvent.Builder()
            .negotiationId(negotiationId)
            .offerorName(offer.offerorName())
            .receiverName(offer.receiverName())
            .status(status)
            .templateNames(offer.contracts()
                .stream()
                .map(TrustedContract::templateName)
                .collect(Collectors.toSet()))
            .build());
    }
}
