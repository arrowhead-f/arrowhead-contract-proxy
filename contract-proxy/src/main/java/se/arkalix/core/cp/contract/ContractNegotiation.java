package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.core.cp.security.HashBase64Dto;
import se.arkalix.core.cp.security.SignatureBase64;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
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
    private Party waitingParty = null;

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

    public synchronized Optional<SignedContractAcceptanceDto> acceptance() {
        return Optional.ofNullable(loggedAcceptance);
    }

    public synchronized Optional<SignedContractRejectionDto> rejection() {
        return Optional.ofNullable(loggedRejection);
    }

    public synchronized List<SignedContractOfferDto> offers() {
        return List.copyOf(loggedOffers);
    }

    public synchronized SignedContractOfferDto lastOffer() {
        if (state == STATE_INITIAL) {
            throw new IllegalStateException("This negotiation session does " +
                "not contain any offers; cannot fulfill request");
        }
        return loggedOffers.get(loggedOffers.size() - 1);
    }

    public synchronized TrustedContractOfferDto lastOfferAsTrusted() {
        final var lastOffer = lastOffer();
        return new TrustedContractOfferBuilder()
            .offerorName(waitingParty.commonName())
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

    public synchronized List<Contract> lastOfferContracts() {
        return lastOffer()
            .contracts()
            .stream()
            .map(ContractBase64::toContract)
            .collect(Collectors.toUnmodifiableList());
    }

    public synchronized SignedContractAcceptanceDto prepareOnBehalfOfOwnedParty(
        final TrustedContractAcceptance acceptance)
    {
        Objects.requireNonNull(acceptance, "Expected acceptance");
        if (acceptance.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + acceptance);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(ownedParty, now);
        throwIfNotCloseTo(acceptance.acceptedAt(), now);

        return new SignedContractAcceptanceBuilder()
            .negotiationId(acceptance.negotiationId())
            .acceptorFingerprint(preferredOwnedPartyFingerprintBase64)
            .offerorFingerprint(preferredCounterPartyFingerprintBase64)
            .offerHash(HashBase64.from(preferredHashAlgorithm.hash(lastOffer().toCanonicalForm())))
            .signature(SignatureBase64.emptyFrom(acceptance.acceptedAt(), ownedParty.signatureScheme()))
            .build()
            .sign(ownedParty);
    }

    public synchronized SignedContractOfferDto prepareOnBehalfOfOwnedParty(final TrustedContractOffer offer) {
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

        return new SignedContractOfferBuilder()
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
    }

    public synchronized SignedContractRejectionDto prepareOnBehalfOfOwnedParty(
        final TrustedContractRejection rejection)
    {
        Objects.requireNonNull(rejection, "Expected rejection");
        if (rejection.negotiationId() != id) {
            throw new IllegalArgumentException("Negotiation ID " + id + " not in " + rejection);
        }

        final var now = Instant.now();
        throwIfPartyCannotUpdateAt(ownedParty, now);
        throwIfNotCloseTo(rejection.rejectedAt(), now);

        return new SignedContractRejectionBuilder()
            .negotiationId(rejection.negotiationId())
            .rejectorFingerprint(preferredOwnedPartyFingerprintBase64)
            .offerorFingerprint(preferredCounterPartyFingerprintBase64)
            .offerHash(HashBase64.from(preferredHashAlgorithm.hash(lastOffer().toCanonicalForm())))
            .signature(SignatureBase64.emptyFrom(rejection.rejectedAt(), ownedParty.signatureScheme()))
            .build()
            .sign(ownedParty);
    }

    public ContractNegotiationStatus status() {
        switch (state) {
        case STATE_OFFERING: return ContractNegotiationStatus.OFFERING;
        case STATE_ACCEPTED: return ContractNegotiationStatus.ACCEPTED;
        case STATE_REJECTED: return ContractNegotiationStatus.REJECTED;
        case STATE_EXPIRED: return ContractNegotiationStatus.EXPIRED;
        default:
            throw new IllegalStateException("Illegal negotiation state: " + state);
        }
    }

    public synchronized void updateOnBehalfOfOwnedParty(final SignedContractAcceptanceDto acceptance) {
        Objects.requireNonNull(acceptance, "Expected acceptance");

        throwIfOwnedPartyCannotUpdateIgnoringExpiration();

        loggedAcceptance = acceptance;
        state = STATE_ACCEPTED;
    }

    public synchronized void updateOnBehalfOfOwnedParty(final SignedContractOfferDto offer) {
        Objects.requireNonNull(offer, "Expected offer");

        throwIfOwnedPartyCannotUpdateIgnoringExpiration();

        loggedOffers.add(offer);
        activeParty = counterParty;
        waitingParty = ownedParty;
        state = STATE_OFFERING;
    }

    public synchronized void updateOnBehalfOfOwnedParty(final SignedContractRejectionDto rejection) {
        Objects.requireNonNull(rejection, "Expected rejection");

        throwIfOwnedPartyCannotUpdateIgnoringExpiration();

        loggedRejection = rejection;
        state = STATE_REJECTED;
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
        waitingParty = counterParty;
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

    private void throwIfOwnedPartyCannotUpdateIgnoringExpiration() {
        if (state == STATE_INITIAL) {
            return;
        }
        if (state != STATE_OFFERING) {
            throw unexpectedStateException();
        }
        if (activeParty != ownedParty) {
            throw new UnsatisfiableRequestException("AWAIT_TURN", "It " +
                "is expected that \"" + activeParty.commonName() + "\" " +
                "is to make the next negotiation update; update " +
                "currently not allowed");
        }
    }

    private void throwIfPartyCannotUpdateAt(final Party updater, final Instant now) {
        if (state == STATE_INITIAL) {
            return;
        }
        if (state != STATE_OFFERING) {
            throw unexpectedStateException();
        }
        if (activeParty != updater) {
            throw new UnsatisfiableRequestException("AWAIT_TURN", "It " +
                "is expected that \"" + activeParty.commonName() + "\" " +
                "is to make the next negotiation update; update " +
                "currently not allowed");
        }
        if (lastOffer().validUntil().isBefore(now.plus(CLOCK_SKEW_TOLERANCE))) {
            state = STATE_EXPIRED;
            throw unexpectedStateException();
        }
    }

    private RuntimeException unexpectedStateException() {
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
            return new IllegalStateException("Illegal negotiation state: " + state);
        }
        return new UnsatisfiableRequestException(stateName, "This " +
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
}
