package se.arkalix.core.cp.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.core.cp.bank.DefinitionBank;
import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.core.cp.security.HashAlgorithmUnsupportedException;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.cp.TrustedContractAcceptance;
import se.arkalix.core.plugin.cp.TrustedContractCounterOffer;
import se.arkalix.core.plugin.cp.TrustedContractOffer;
import se.arkalix.core.plugin.cp.TrustedContractRejectionDto;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

import java.util.*;
import java.util.stream.Collectors;

import static se.arkalix.core.plugin.cp.ContractNegotiationStatus.*;
import static se.arkalix.util.concurrent.Future.done;

public class ContractProxy {
    private static final Logger logger = LoggerFactory.getLogger(ContractProxy.class);

    private final Set<HashAlgorithm> acceptedHashAlgorithms;
    private final Parties parties;
    private final ContractRelay relay;
    private final Templates templates;
    private final DefinitionBank bank;

    private final ContractNegotiations negotiations;

    public ContractProxy(final Builder builder) {
        acceptedHashAlgorithms = Set.copyOf(Objects.requireNonNull(builder.acceptedHashAlgorithms,
            "Expected acceptedHashAlgorithms"));
        if (acceptedHashAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("Expected acceptedHashAlgorithms.size() > 0");
        }

        bank = new DefinitionBank(acceptedHashAlgorithms);

        Objects.requireNonNull(builder.ownedParties, "Expected ownedParties");
        if (builder.ownedParties.isEmpty()) {
            throw new IllegalArgumentException("Expected ownedParties.size() > 0");
        }
        Objects.requireNonNull(builder.counterParties, "Expected counterParties");
        if (builder.counterParties.isEmpty()) {
            throw new IllegalArgumentException("Expected counterParties.size() > 0");
        }
        final var allParties = new ArrayList<Party>(builder.ownedParties.size() + builder.counterParties.size());
        allParties.addAll(builder.ownedParties);
        allParties.addAll(builder.counterParties);
        parties = new Parties(allParties);

        relay = Objects.requireNonNull(builder.relay, "Expected relay");

        Objects.requireNonNull(builder.templates, "Expected templates");
        if (builder.templates.isEmpty()) {
            throw new IllegalArgumentException("Expected templates.size() > 0");
        }
        templates = new Templates(builder.templates);

        negotiations = new ContractNegotiations(templates, acceptedHashAlgorithms);
    }

    public Optional<ContractNegotiation> getNegotiationByNamesAndId(
        final String name1,
        final String name2,
        final long id
    ) {
        return negotiations.getBy(name1, name2, id);
    }

    public DefinitionBank bank() {
        return bank;
    }

    public Parties parties() {
        return parties;
    }

    public Templates templates() {
        return templates;
    }

    public void update(final SignedContractAcceptanceDto acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");

        final var acceptor = getCounterPartyByFingerprintOrThrow(acceptance.acceptorFingerprint());
        final var offeror = getOwnedPartyByFingerprintOrThrow(acceptance.offerorFingerprint());
        final var negotiation = getNegotiationOrThrow(offeror, acceptor, acceptance.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(acceptance);
        bank.add(acceptance);
        relay.sendToEventHandler(acceptance.negotiationId(), negotiation.lastOfferAsTrusted(), ACCEPTED)
            .onFailure(fault -> logger.warn("Failed to send " + acceptance + " to event handler", fault));
    }

    public void update(final SignedContractOfferDto offer) {
        Objects.requireNonNull(offer, "Expected offer");

        final var offeror = getCounterPartyByFingerprintOrThrow(offer.offerorFingerprint());
        final var receiver = getOwnedPartyByFingerprintOrThrow(offer.receiverFingerprint());
        final var negotiation = negotiations.getOrCreateBy(receiver, offeror, offer.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(offer);
        bank.add(offer);

        resolveUnknownDefinitionsReferencedIn(offer)
            .ifSuccess(ignored ->
                relay.sendToEventHandler(offer.negotiationId(), negotiation.lastOfferAsTrusted(), OFFERING)
                    .onFailure(fault -> logger.warn("Failed to send " + offer + " to event handler", fault)))
            .onFailure(fault -> logger.error("Failed to resolve definition referenced in " + offer, fault));
    }

    private Future<?> resolveUnknownDefinitionsReferencedIn(final SignedContractOfferDto offer) {
        final var hashes = offer.hashReferencesInArguments()
            .filter(hash -> !bank.contains(hash))
            .collect(Collectors.toUnmodifiableList());

        if (hashes.isEmpty()) {
            return done();
        }

        return relay.getFromCounterParty(hashes)
            .flatMap(definitions -> Futures.serialize(definitions.stream()
                .map(definition -> {
                    bank.add(definition);
                    return (definition instanceof SignedContractOfferDto)
                        ? resolveUnknownDefinitionsReferencedIn((SignedContractOfferDto) definition)
                        : done();
                })));
    }

    public void update(final SignedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");

        final var rejector = getCounterPartyByFingerprintOrThrow(rejection.rejectorFingerprint());
        final var offeror = getOwnedPartyByFingerprintOrThrow(rejection.offerorFingerprint());
        final var negotiation = getNegotiationOrThrow(offeror, rejector, rejection.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(rejection);
        bank.add(rejection);
        relay.sendToEventHandler(rejection.negotiationId(), negotiation.lastOfferAsTrusted(), REJECTED)
            .onFailure(fault -> logger.warn("Failed to send " + rejection + " to event handler", fault));
    }

    public Future<?> update(final TrustedContractAcceptance acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");

        final var acceptor = getOwnedPartyByCommonNameOrThrow(acceptance.acceptorName());
        final var offeror = getCounterPartyByCommonNameOrThrow(acceptance.offerorName());
        final var negotiation = getNegotiationOrThrow(acceptor, offeror, acceptance.negotiationId());
        final var signedAcceptance = negotiation.prepareOnBehalfOfOwnedParty(acceptance);
        bank.add(signedAcceptance);
        return relay.sendToCounterParty(signedAcceptance, offeror)
            .ifSuccess(ignored -> {
                negotiation.updateOnBehalfOfOwnedParty(signedAcceptance);
                relay.sendToEventHandler(negotiation.id(), negotiation.lastOfferAsTrusted(), ACCEPTED)
                    .onFailure(fault -> logger.warn("Failed to send " + acceptance + " to event handler", fault));
            });
    }

    public Future<Long> update(final TrustedContractOffer offer) {
        Objects.requireNonNull(offer, "Expected offer");

        final var offeror = getOwnedPartyByCommonNameOrThrow(offer.offerorName());
        final var receiver = getCounterPartyByCommonNameOrThrow(offer.receiverName());

        final ContractNegotiation negotiation;
        if (offer instanceof TrustedContractCounterOffer) {
            final var counterOffer = (TrustedContractCounterOffer) offer;
            negotiation = getNegotiationOrThrow(offeror, receiver, counterOffer.negotiationId());
        }
        else {
            negotiation = negotiations.createFor(offeror, receiver);
        }

        final var signedOffer = negotiation.prepareOnBehalfOfOwnedParty(offer);
        bank.add(signedOffer);
        return relay.sendToCounterParty(signedOffer, receiver)
            .ifSuccess(ignored -> {
                negotiation.updateOnBehalfOfOwnedParty(signedOffer);
                relay.sendToEventHandler(negotiation.id(), negotiation.lastOfferAsTrusted(), OFFERING)
                    .onFailure(fault -> logger.warn("Failed to send " + offer + " to event handler", fault));
            })
            .pass(negotiation.id());
    }

    public Future<?> update(final TrustedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");

        final var rejector = getOwnedPartyByCommonNameOrThrow(rejection.rejectorName());
        final var offeror = getCounterPartyByCommonNameOrThrow(rejection.offerorName());
        final var negotiation = getNegotiationOrThrow(rejector, offeror, rejection.negotiationId());
        final var signedRejection = negotiation.prepareOnBehalfOfOwnedParty(rejection);
        bank.add(signedRejection);
        return relay.sendToCounterParty(signedRejection, offeror)
            .ifSuccess(ignored -> {
                negotiation.updateOnBehalfOfOwnedParty(signedRejection);
                relay.sendToEventHandler(negotiation.id(), negotiation.lastOfferAsTrusted(), REJECTED)
                    .onFailure(fault -> logger.warn("Failed to send " + rejection + " to event handler", fault));
            });
    }

    private Party getCounterPartyByCommonNameOrThrow(final String commonName) {
        return parties.getCounterPartyByCommonName(commonName)
            .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_PARTY", "" +
                "No counter-party named \"" + commonName + "\" is known"));
    }

    private Party getCounterPartyByFingerprintOrThrow(final HashBase64 fingerprint) {
        throwIfNotAccepted(fingerprint.algorithm());
        return parties.getCounterPartyByFingerprint(fingerprint.toHash())
            .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_PARTY", "" +
                "No permitted counter-party with " + fingerprint + " is known"));
    }

    private OwnedParty getOwnedPartyByCommonNameOrThrow(final String commonName) {
        return parties.getOwnedPartyByCommonName(commonName)
            .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_PARTY", "" +
                "No owned party named \"" + commonName + "\" is known"));
    }

    private OwnedParty getOwnedPartyByFingerprintOrThrow(final HashBase64 fingerprint) {
        throwIfNotAccepted(fingerprint.algorithm());
        return parties.getOwnedPartyByFingerprint(fingerprint.toHash())
            .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_PARTY", "" +
                "No owned party with " + fingerprint + " is known"));
    }

    private ContractNegotiation getNegotiationOrThrow(
        final OwnedParty ownedParty,
        final Party counterParty,
        final long negotiationId
    ) {
        return negotiations.getBy(ownedParty, counterParty, negotiationId)
            .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_NEGOTIATION", "" +
                "No negotiation with ID " + negotiationId + " exists for " +
                "the parties \"" + counterParty.commonName() + "\" and \"" +
                ownedParty.commonName() + "\""));
    }

    private void throwIfNotAccepted(final HashAlgorithm hashAlgorithm) {
        if (!acceptedHashAlgorithms.contains(hashAlgorithm)) {
            throw new HashAlgorithmUnsupportedException(hashAlgorithm.toString());
        }
    }

    public static class Builder {
        private Collection<HashAlgorithm> acceptedHashAlgorithms;
        private Collection<Party> counterParties;
        private Collection<OwnedParty> ownedParties;
        private ContractRelay relay;
        private Collection<Template> templates;

        public Builder acceptedHashAlgorithms(final Collection<HashAlgorithm> acceptedHashAlgorithms) {
            this.acceptedHashAlgorithms = acceptedHashAlgorithms;
            return this;
        }

        public Builder acceptedHashAlgorithms(final HashAlgorithm... acceptedHashAlgorithms) {
            return acceptedHashAlgorithms(Arrays.asList(acceptedHashAlgorithms));
        }

        public Builder counterParties(final Collection<Party> counterParties) {
            this.counterParties = counterParties;
            return this;
        }

        public Builder counterParties(final Party... counterParties) {
            return counterParties(Arrays.asList(counterParties));
        }

        public Builder ownedParties(final Collection<OwnedParty> ownedParties) {
            this.ownedParties = ownedParties;
            return this;
        }

        public Builder ownedParties(final OwnedParty... ownedParties) {
            return ownedParties(Arrays.asList(ownedParties));
        }

        public Builder relay(final ContractRelay relay) {
            this.relay = relay;
            return this;
        }

        public Builder templates(final Collection<Template> templates) {
            this.templates = templates;
            return this;
        }

        public Builder templates(final Template... templates) {
            return templates(Arrays.asList(templates));
        }

        public ContractProxy build() {
            return new ContractProxy(this);
        }
    }
}
