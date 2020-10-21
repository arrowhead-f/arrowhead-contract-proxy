package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.bank.Definition;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.TrustedContractOffer;
import se.arkalix.util.concurrent.Future;

import java.util.Collection;
import java.util.List;

public interface ContractRelay {
    Future<List<Definition>> getFromCounterParty(final Collection<Hash> hashes);
    Future<?> sendToEventHandler(long negotiationId, TrustedContractOffer offer, ContractNegotiationStatus status);
    Future<?> sendToCounterParty(final SignedContractAcceptanceDto acceptance, final Party counterParty);
    Future<?> sendToCounterParty(final SignedContractOfferDto offer, final Party counterParty);
    Future<?> sendToCounterParty(final SignedContractRejectionDto rejection, final Party counterParty);
}
