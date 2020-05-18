package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashAlgorithm;
import se.arkalix.core.coprox.security.HashAlgorithmUnsupportedException;
import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.core.coprox.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.cp.TrustedContractAcceptance;
import se.arkalix.core.plugin.cp.TrustedContractCounterOffer;
import se.arkalix.core.plugin.cp.TrustedContractOffer;
import se.arkalix.core.plugin.cp.TrustedContractRejectionDto;

import java.util.*;
import java.util.stream.Collectors;

import static se.arkalix.core.plugin.cp.ContractNegotiationStatus.*;

public class Model {
    private final Set<HashAlgorithm> acceptedHashAlgorithms;
    private final ContractNegotiationObserver observer;
    private final Parties parties;
    private final Templates templates;
    private final ContractNegotiations negotiations;

    public Model(final Builder builder) {
        acceptedHashAlgorithms = Objects.requireNonNullElseGet(builder.acceptedHashAlgorithms, () ->
            HashAlgorithm.ALL.stream().filter(HashAlgorithm::isCollisionSafe).collect(Collectors.toList()))
            .stream()
            .collect(Collectors.toUnmodifiableSet());
        if (acceptedHashAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("Expected acceptedHashAlgorithms.size() > 0");
        }
        observer = Objects.requireNonNull(builder.observer, "Expected observer");

        Objects.requireNonNull(builder.ownedParties, "Expected ownedParties");
        if (builder.ownedParties.isEmpty()) {
            throw new IllegalArgumentException("Expected ownedParties.size() > 0");
        }
        Objects.requireNonNull(builder.counterParties, "Expected counterParties");
        if (builder.counterParties.isEmpty()) {
            throw new IllegalArgumentException("Expected counterParties.size() > 0");
        }
        Objects.requireNonNull(builder.templates, "Expected templates");
        if (builder.templates.isEmpty()) {
            throw new IllegalArgumentException("Expected templates.size() > 0");
        }
        final var allParties = new ArrayList<Party>(builder.ownedParties.size() + builder.counterParties.size());
        allParties.addAll(builder.ownedParties);
        allParties.addAll(builder.counterParties);
        parties = new Parties(allParties);
        templates = new Templates(builder.templates);
        negotiations = new ContractNegotiations(templates, acceptedHashAlgorithms);
    }

    public void update(final SignedContractAcceptanceDto acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");

        final var acceptor = getCounterPartyByFingerprintOrThrow(acceptance.acceptorFingerprint());
        final var offeror = getOwnedPartyByFingerprintOrThrow(acceptance.offerorFingerprint());
        final var negotiation = getNegotiationOrThrow(offeror, acceptor, acceptance.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(acceptance);
        observer.onEvent(acceptance.negotiationId(), negotiation.lastOfferAsTrusted(), ACCEPTED);
    }

    public void update(final SignedContractOfferDto offer) {
        Objects.requireNonNull(offer, "Expected offer");

        final var offeror = getCounterPartyByFingerprintOrThrow(offer.offerorFingerprint());
        final var receiver = getOwnedPartyByFingerprintOrThrow(offer.receiverFingerprint());
        final var negotiation = getNegotiationOrThrow(receiver, offeror, offer.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(offer);
        observer.onEvent(offer.negotiationId(), negotiation.lastOfferAsTrusted(), OFFERING);
    }

    public void update(final SignedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");

        final var rejector = getCounterPartyByFingerprintOrThrow(rejection.rejectorFingerprint());
        final var offeror = getOwnedPartyByFingerprintOrThrow(rejection.offerorFingerprint());
        final var negotiation = getNegotiationOrThrow(offeror, rejector, rejection.negotiationId());
        negotiation.updateOnBehalfOfCounterParty(rejection);
        observer.onEvent(rejection.negotiationId(), negotiation.lastOfferAsTrusted(), REJECTED);
    }

    public void update(final TrustedContractAcceptance acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");

        final var acceptor = getOwnedPartyByCommonNameOrThrow(acceptance.acceptorName());
        final var offeror = getCounterPartyByCommonNameOrThrow(acceptance.offerorName());
        final var negotiation = getNegotiationOrThrow(acceptor, offeror, acceptance.negotiationId());
        final var signedAcceptance = negotiation.updateOnBehalfOfOwnedParty(acceptance);
        // TODO: Send signedAcceptance to counter-party.
        observer.onEvent(acceptance.negotiationId(), negotiation.lastOfferAsTrusted(), ACCEPTED);
    }

    public void update(final TrustedContractOffer offer) {
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

        final var signedOffer = negotiation.updateOnBehalfOfOwnedParty(offer);
        // TODO: Send signedOffer to counter-party.
        observer.onEvent(negotiation.id(), negotiation.lastOfferAsTrusted(), OFFERING);
    }

    public void update(final TrustedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");

        final var rejector = getOwnedPartyByCommonNameOrThrow(rejection.rejectorName());
        final var offeror = getCounterPartyByCommonNameOrThrow(rejection.offerorName());
        final var negotiation = getNegotiationOrThrow(rejector, offeror, rejection.negotiationId());
        final var signedRejection = negotiation.updateOnBehalfOfOwnedParty(rejection);
        // TODO: Send signedRejection to counter-party.
        observer.onEvent(rejection.negotiationId(), negotiation.lastOfferAsTrusted(), REJECTED);
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
        final long negotiationId)
    {
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
        private List<HashAlgorithm> acceptedHashAlgorithms;
        private ContractNegotiationObserver observer;
        private List<Template> templates;
        private List<OwnedParty> ownedParties;
        private List<Party> counterParties;

        public Builder acceptedHashAlgorithms(final List<HashAlgorithm> acceptedHashAlgorithms) {
            this.acceptedHashAlgorithms = acceptedHashAlgorithms;
            return this;
        }

        public Builder acceptedHashAlgorithms(final HashAlgorithm... acceptedHashAlgorithms) {
            return acceptedHashAlgorithms(Arrays.asList(acceptedHashAlgorithms));
        }

        public Builder observer(final ContractNegotiationObserver observer) {
            this.observer = observer;
            return this;
        }

        public Builder templates(final List<Template> templates) {
            this.templates = templates;
            return this;
        }

        public Builder templates(final Template... templates) {
            return templates(Arrays.asList(templates));
        }

        public Builder ownedParties(final List<OwnedParty> ownedParties) {
            this.ownedParties = ownedParties;
            return this;
        }

        public Builder ownedParties(final OwnedParty... ownedParties) {
            return ownedParties(Arrays.asList(ownedParties));
        }

        public Builder counterParties(final List<Party> counterParties) {
            this.counterParties = counterParties;
            return this;
        }

        public Builder counterParties(final Party... counterParties) {
            return counterParties(Arrays.asList(counterParties));
        }

        public Model build() {
            return new Model(this);
        }
    }
}
