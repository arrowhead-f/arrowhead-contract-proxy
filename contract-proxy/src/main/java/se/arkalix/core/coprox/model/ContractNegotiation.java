package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashAlgorithm;
import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.core.coprox.security.HashBase64Dto;
import se.arkalix.core.coprox.security.SignatureBase64;
import se.arkalix.core.coprox.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.cp.*;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ContractNegotiation {
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(30);

    private final OwnedParty ownedParty;
    private final Party counterParty;
    private final long id;
    private final Templates templates;

    private final HashBase64Dto preferredOwnedPartyFingerprintBase64;
    private final HashBase64Dto preferredCounterPartyFingerprintBase64;
    private final Set<HashAlgorithm> acceptedHashAlgorithms;
    private final HashAlgorithm preferredHashAlgorithm;

    private static final int STATE_INITIAL = 0;
    private static final int STATE_OFFERING = 1;
    private static final int STATE_ACCEPTED = 2;
    private static final int STATE_REJECTED = 3;
    private static final int STATE_EXPIRED = 4;
    private int state = STATE_INITIAL;

    private Party activeParty = null;

    private final List<SignedContractOfferDto> loggedOffers = new ArrayList<>();
    private SignedContractAcceptanceDto loggedAcceptance = null;
    private SignedContractRejectionDto loggedRejection = null;

    public ContractNegotiation(
        final OwnedParty ownedParty,
        final Party counterParty,
        final long id,
        final Templates templates,
        final Set<HashAlgorithm> acceptedHashAlgorithms)
    {
        this.ownedParty = Objects.requireNonNull(ownedParty, "Expected ownedParty");
        this.counterParty = Objects.requireNonNull(counterParty, "Expected counterParty");
        this.id = id;
        this.templates = Objects.requireNonNull(templates, "Expected templates");
        this.acceptedHashAlgorithms = Objects.requireNonNull(acceptedHashAlgorithms, "Expected acceptedHashAlgorithms");

        preferredOwnedPartyFingerprintBase64 = HashBase64.from(ownedParty.preferredFingerprint());
        preferredCounterPartyFingerprintBase64 = HashBase64.from(counterParty.preferredFingerprint());
        preferredHashAlgorithm = ownedParty.preferredFingerprint().algorithm();
    }

    public OwnedParty ownedParty() {
        return ownedParty;
    }

    public Party counterParty() {
        return counterParty;
    }

    public long id() {
        return id;
    }

    public synchronized TrustedContractOfferDto lastOfferAsTrusted() {
        if (state == STATE_INITIAL) {
            throw new IllegalStateException("This negotiation session does " +
                "not contain any offers; cannot fulfill request");
        }
        final var lastOffer = lastOffer();
        return new TrustedContractOfferBuilder()
            .offerorName((ownedParty == activeParty ? ownedParty : counterParty).commonName())
            .receiverName(activeParty.commonName())
            .validAfter(lastOffer.validAfter())
            .validUntil(lastOffer.validUntil())
            .offeredAt(lastOffer.signature().timestamp())
            .contracts(lastOffer.contracts()
                .stream()
                .map(contract -> templates.getByHash(contract.templateHash().toHash())
                    .map(template -> new TrustedContractBuilder()
                        .templateName(template.name())
                        .arguments(contract.arguments())
                        .build())
                    .orElseThrow(() -> new IllegalStateException("Template " +
                        "with " + contract.templateHash() + " not found; " +
                        "expected it to be available at this point as no " +
                        "offers should be able to refer to non-existent " +
                        "contract templates")))
                .collect(Collectors.toList()))
            .build();
    }

    public synchronized SignedContractAcceptanceDto updateOnBehalfOfOwnedParty(
        final TrustedContractAcceptance acceptance)
    {
        Objects.requireNonNull(acceptance, "Expected acceptance");
        if (acceptance.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + acceptance);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(ownedParty, now);
        throwIfNotCloseTo(acceptance.acceptedAt(), now);

        loggedAcceptance = new SignedContractAcceptanceBuilder()
            .negotiationId(acceptance.negotiationId())
            .acceptorFingerprint(preferredOwnedPartyFingerprintBase64)
            .offerorFingerprint(preferredCounterPartyFingerprintBase64)
            .offerHash(HashBase64.from(preferredHashAlgorithm.hash(lastOffer().toCanonicalForm())))
            .signature(SignatureBase64.emptyFrom(acceptance.acceptedAt(), ownedParty.signatureScheme()))
            .build()
            .sign(ownedParty);
        state = STATE_ACCEPTED;

        return loggedAcceptance;
    }

    public synchronized SignedContractOfferDto updateOnBehalfOfOwnedParty(final TrustedContractOffer offer) {
        Objects.requireNonNull(offer, "Expected offer");
        if (offer instanceof TrustedContractCounterOffer &&
            ((TrustedContractCounterOffer) offer).negotiationId() != id)
        {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + offer);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(ownedParty, now);
        if (offer.contracts().isEmpty()) {
            throw new UnsatisfiableRequestException("NO_CONTRACTS", "" +
                "Provided offer contains no contracts ");
        }
        if (offer.validUntil().isBefore(now)) {
            throw new UnsatisfiableRequestException("ALREADY_EXPIRED", "The " +
                "provided offer has already expired");
        }
        if (offer.validAfter().isAfter(offer.validUntil())) {
            throw new UnsatisfiableRequestException("CONFLICTING_TIME_CONSTRAINTS", "" +
                "The provided offer is configured to expire before it " +
                "becomes acceptable");
        }
        throwIfNotCloseTo(offer.offeredAt(), now);

        final var signedOffer = new SignedContractOfferBuilder()
            .negotiationId(id)
            .offerorFingerprint(preferredOwnedPartyFingerprintBase64)
            .receiverFingerprint(preferredCounterPartyFingerprintBase64)
            .validAfter(offer.validAfter())
            .validUntil(offer.validUntil())
            .contracts(offer.contracts()
                .stream()
                .map(contract -> templates.getByName(contract.templateName())
                    .map(template -> {
                        template.validate(contract.arguments());
                        return new ContractBase64Builder()
                            .templateHash(HashBase64.from(template.preferredHash()))
                            .arguments(contract.arguments())
                            .build();
                    })
                    .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_TEMPLATE", "" +
                        "No known contract template named \"" + contract.templateName() +
                        "\" exists; cannot make offer")))
                .collect(Collectors.toList()))
            .signature(SignatureBase64.emptyFrom(offer.offeredAt(), ownedParty.signatureScheme()))
            .build()
            .sign(ownedParty);

        loggedOffers.add(signedOffer);
        activeParty = counterParty;
        state = STATE_OFFERING;

        return signedOffer;
    }

    public synchronized SignedContractRejectionDto updateOnBehalfOfOwnedParty(
        final TrustedContractRejection rejection)
    {
        Objects.requireNonNull(rejection, "Expected rejection");
        if (rejection.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + rejection);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(ownedParty, now);
        throwIfNotCloseTo(rejection.rejectedAt(), now);

        loggedRejection = new SignedContractRejectionBuilder()
            .negotiationId(rejection.negotiationId())
            .rejectorFingerprint(preferredOwnedPartyFingerprintBase64)
            .offerorFingerprint(preferredCounterPartyFingerprintBase64)
            .offerHash(HashBase64.from(preferredHashAlgorithm.hash(lastOffer().toCanonicalForm())))
            .signature(SignatureBase64.emptyFrom(rejection.rejectedAt(), ownedParty.signatureScheme()))
            .build()
            .sign(ownedParty);
        state = STATE_REJECTED;

        return loggedRejection;
    }

    public synchronized void updateOnBehalfOfCounterParty(final SignedContractAcceptanceDto acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");
        if (acceptance.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + acceptance);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(counterParty, now);
        throwIfNotMatchingLastOffer(acceptance.offerHash());
        throwIfNotSignedByPartyAt(acceptance, counterParty, now);

        loggedAcceptance = acceptance;
        state = STATE_ACCEPTED;
    }

    public synchronized void updateOnBehalfOfCounterParty(final SignedContractOfferDto offer) {
        Objects.requireNonNull(offer, "Expected offer");
        if (offer.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + offer);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(counterParty, now);
        if (offer.contracts().isEmpty()) {
            throw new UnsatisfiableRequestException("NO_CONTRACTS", "" +
                "Provided offer contains no contracts ");
        }
        if (offer.validUntil().isBefore(now)) {
            throw new UnsatisfiableRequestException("ALREADY_EXPIRED", "The " +
                "provided offer has already expired");
        }
        if (offer.validAfter().isAfter(offer.validUntil())) {
            throw new UnsatisfiableRequestException("CONFLICTING_TIME_CONSTRAINTS", "" +
                "The provided offer is configured to expire before it " +
                "becomes acceptable");
        }
        for (final var contract : offer.contracts()) {
            final var template = templates.getByHash(contract.templateHash().toHash())
                .orElseThrow(() -> new UnsatisfiableRequestException("UNKNOWN_TEMPLATE", "" +
                    "No template with " + contract.templateHash() + " is known to exist; " +
                    "cannot make offer"));
            template.validate(contract.arguments());
        }
        throwIfNotSignedByPartyAt(offer, counterParty, now);

        loggedOffers.add(offer);
        activeParty = ownedParty;
        state = STATE_OFFERING;
    }

    public synchronized void updateOnBehalfOfCounterParty(final SignedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");
        if (rejection.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + rejection);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(counterParty, now);
        throwIfNotMatchingLastOffer(rejection.offerHash());
        throwIfNotSignedByPartyAt(rejection, counterParty, now);

        loggedRejection = rejection;
        state = STATE_REJECTED;
    }

    private SignedContractOfferDto lastOffer() {
        assert loggedOffers.size() > 0;
        return loggedOffers.get(loggedOffers.size() - 1);
    }

    private void throwIfNotCloseTo(final Instant timestamp, final Instant now) {
        if (timestamp.isBefore(now.minus(CLOCK_SKEW_TOLERANCE)) || timestamp.isAfter(now.plus(CLOCK_SKEW_TOLERANCE))) {
            throw new UnsatisfiableRequestException("BAD_TIMESTAMP", "The timestamp in " +
                "the provided message " + timestamp + " does not seem to " +
                "represent a time close to the current time " + now);
        }
    }

    private void throwIfNotMatchingLastOffer(final HashBase64 hashBase64) {
        final var hash = hashBase64.toHash();
        final var algorithm = hash.algorithm();
        if (!acceptedHashAlgorithms.contains(algorithm)) {
            throw new UnsatisfiableRequestException("UNSUPPORTED_HASH_ALGORITHM", "" +
                "The offer hash in the provided message uses the " + algorithm +
                " algorithm, but only " + acceptedHashAlgorithms +
                " are supported for this negotiation session");
        }
        final var lastOfferHash = algorithm.hash(lastOffer().toCanonicalForm());
        if (!hash.equals(lastOfferHash)) {
            throw new UnsatisfiableRequestException("BAD_HASH", "The offer " +
                "hash in the provided message does not match that of the last " +
                "negotiation offer");
        }
    }

    private void throwIfNotSignedByPartyAt(final SignedMessage message, final Party signer, final Instant now) {
        final var signature = message.signature();
        throwIfNotCloseTo(signature.timestamp(), now);
        if (signature.verify(signer.certificate(), message.toCanonicalForm())) {
            return;
        }
        throw new UnsatisfiableRequestException("BAD_SIGNATURE", "The " +
            "signature in the provided message does not match its " +
            "contents when verified with the public key of \"" +
            signer.commonName() + "\"");
    }

    private void throwIfPartyCannotUpdateAt(final Party updater, final Instant now) {
        closed:
        {
            if (state == STATE_INITIAL) {
                return;
            }
            if (state != STATE_OFFERING) {
                break closed;
            }
            if (activeParty != updater) {
                throw new UnsatisfiableRequestException("AWAIT_TURN", "It " +
                    "is expected that \"" + activeParty.commonName() + "\" " +
                    "is to make the next negotiation update; update " +
                    "currently not allowed");
            }
            if (lastOffer().validUntil().isBefore(now.plus(CLOCK_SKEW_TOLERANCE))) {
                state = STATE_EXPIRED;
                break closed;
            }
            return;
        }
        final String stateLabel;
        final String stateName;
        switch (state) {
        case STATE_ACCEPTED:
            stateName = "NEGOTIATION_ACCEPTED";
            stateLabel = "been accepted by \"" + activeParty.commonName() + "\"";
            break;

        case STATE_REJECTED:
            stateName = "NEGOTIATION_REJECTED";
            stateLabel = "been rejected by \"" + activeParty.commonName() + "\"";
            break;

        case STATE_EXPIRED:
            stateName = "NEGOTIATION_EXPIRED";
            stateLabel = "expired";
            break;

        default:
            throw new IllegalStateException("Illegal session state: " + state);
        }
        throw new UnsatisfiableRequestException(stateName, "This " +
            "negotiation has " + stateLabel + " and can, as a consequence, " +
            "no longer be updated");
    }

    @Override
    public String toString() {
        return "ContractNegotiation{" +
            "ownedParty=" + ownedParty.commonName() +
            ", counterParty=" + counterParty.commonName() +
            ", id=" + id +
            '}';
    }

            /*
        final var offerorFingerprint = offer.offerorFingerprint().toHash();
        if (!offeror.acceptedFingerprints().contains(offerorFingerprint)) {
            if (!offeror.acceptedFingerprintAlgorithms().contains(offerorFingerprint.algorithm())) {
                throw new SessionException("UNSUPPORTED_HASH_ALGORITHM", "" +
                    "The offeror fingerprint in the provided offer uses the " +
                    offerorFingerprint.algorithm() + " algorithm, but only " +
                    offeror.acceptedFingerprintAlgorithms() + " are " +
                    "supported");
            }
            throw new SessionException("UNEXPECTED_OFFEROR", "Expected the" +
                "offeror identified in provided offer to be \"" +
                offeror.commonName() + "\"");
        }

        final var receiverFingerprint = offer.receiverFingerprint().toHash();
        if (!receiver.acceptedFingerprints().contains(receiverFingerprint)) {
            if (!receiver.acceptedFingerprintAlgorithms().contains(receiverFingerprint.algorithm())) {
                throw new SessionException("UNSUPPORTED_HASH_ALGORITHM", "" +
                    "The receiver fingerprint in the provided offer uses the " +
                    receiverFingerprint.algorithm() + " algorithm, but only " +
                    receiver.acceptedFingerprintAlgorithms() + " are " +
                    "supported");
            }
            throw new SessionException("UNEXPECTED_RECEIVER", "Expected the" +
                "receiver identified in provided offer to be \"" +
                receiver.commonName() + "\"");
        }
        */
}
