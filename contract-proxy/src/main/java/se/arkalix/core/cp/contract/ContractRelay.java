package se.arkalix.core.cp.contract;

import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.TrustedContractOffer;
import se.arkalix.util.concurrent.Future;

public interface ContractRelay {
    Future<?> sendToEventHandler(long negotiationId, TrustedContractOffer offer, ContractNegotiationStatus status);
    Future<?> sendToCounterParty(final SignedContractAcceptanceDto acceptance, final Party counterParty);
    Future<?> sendToCounterParty(final SignedContractOfferDto offer, final Party counterParty);
    Future<?> sendToCounterParty(final SignedContractRejectionDto rejection, final Party counterParty);
}
