package se.arkalix.core.coprox.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ContractSession {
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(30);

    private final OwnedParty ownedParty;
    private final Party counterParty;
    private final long id;

    private static final int STATE_INITIAL = 0;
    private static final int STATE_OFFERING = 1;
    private static final int STATE_ACCEPTED = 2;
    private static final int STATE_REJECTED = 3;
    private static final int STATE_EXPIRED = 4;
    private int state = STATE_INITIAL;

    private Party activeParty = null;

    private final List<SignedContractOffer> loggedOffers = new ArrayList<>();
    private SignedContractAcceptance loggedAcceptance = null;
    private SignedContractRejection loggedRejection = null;

    public ContractSession(final OwnedParty ownedParty, final Party counterParty, final long id) {
        this.id = id;
        this.ownedParty = ownedParty;
        this.counterParty = counterParty;
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

    public synchronized void updateForOwnedParty(final SignedContractAcceptance acceptance) throws ModelException {
        update(acceptance, ownedParty);
    }

    public synchronized void updateForOwnedParty(final SignedContractOffer offer) throws ModelException {
        update(offer, ownedParty, counterParty);
    }

    public synchronized void updateForOwnedParty(final SignedContractRejection rejection) throws ModelException {
        update(rejection, ownedParty);
    }

    public synchronized void updateForCounterParty(final SignedContractAcceptance acceptance) throws ModelException {
        update(acceptance, counterParty);
    }

    public synchronized void updateForCounterParty(final SignedContractOffer offer) throws ModelException {
        update(offer, counterParty, ownedParty);
    }

    public synchronized void updateForCounterParty(final SignedContractRejection rejection) throws ModelException {
        update(rejection, counterParty);
    }

    private void update(final SignedContractAcceptance acceptance, final Party acceptor)
        throws ModelException
    {
        Objects.requireNonNull(acceptance, "Expected acceptance");
        Objects.requireNonNull(acceptor, "Expected acceptor");
        if (acceptance.sessionId() != id) {
            throw new IllegalArgumentException("Session ID " + id + " not in " + acceptance);
        }

        final var now = Instant.now();

        verifyStateIsOffering(now);

        if (activeParty != acceptor) {
            throw new ModelException("AWAIT_TURN", "You made the last " +
                "offer and must, therefore, wait until the session " +
                "expires or your counter-party responds; cannot accept offer");
        }

        if (!(acceptor instanceof OwnedParty)) {
            final var acceptedOfferHash = acceptance.offerHash().toHash();
            final var acceptedOfferHashAlgorithm = acceptedOfferHash.algorithm();
            if (!acceptor.acceptedHashAlgorithms().contains(acceptedOfferHashAlgorithm)) {
                throw new ModelException("UNSUPPORTED_HASH_ALGORITHM", "" +
                    "The offer hash in the provided acceptance uses the " +
                    acceptedOfferHashAlgorithm + " algorithm, but only " +
                    acceptor.acceptedHashAlgorithms() + " are supported");
            }
            final var lastOffer = loggedOffers.get(loggedOffers.size() - 1);
            final var lastOfferHash = acceptedOfferHashAlgorithm.hash(lastOffer.toCanonicalJson());
            if (!acceptedOfferHash.equals(lastOfferHash)) {
                throw new ModelException("BAD_HASH", "The offer hash in the " +
                    "provided acceptance does not match that of the last " +
                    "session offer");
            }

            verifyIsApproximatelyEqualTo(acceptance.signature().timestamp(), now);

            if (!acceptance.signature().verify(acceptor.certificate(), acceptance.toCanonicalJson())) {
                throw new ModelException("BAD_SIGNATURE", "The signature " +
                    "in the provided acceptance does not match its contents " +
                    "when verified with the public key of \"" +
                    acceptor.commonName() + "\"");
            }
        }

        loggedAcceptance = acceptance;
        activeParty = null;
        state = STATE_ACCEPTED;
    }

    private void update(final SignedContractOffer offer, final Party offeror, final Party receiver)
        throws ModelException
    {
        Objects.requireNonNull(offer, "Expected offer");
        Objects.requireNonNull(offeror, "Expected offeror");
        Objects.requireNonNull(receiver, "Expected receiver");
        if (offer.sessionId() != id) {
            throw new IllegalArgumentException("Session ID " + id + " not in " + offer);
        }

        final var now = Instant.now();

        if (state != STATE_INITIAL) {
            verifyStateIsOffering(now);

            if (activeParty != offeror) {
                throw new ModelException("AWAIT_TURN", "You made the last " +
                    "offer and must, therefore, wait until the session " +
                    "expires or your counter-party responds; cannot make " +
                    "offer");
            }
        }

        if (offer.contracts().isEmpty()) {
            throw new ModelException("NO_CONTRACTS", "Provided offer " +
                "contains no contracts ");
        }

        // TODO: Already verified. Move.
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

        if (!(offeror instanceof OwnedParty)) {
            if (offer.validUntil().isBefore(now)) {
                throw new ModelException("ALREADY_EXPIRED", "The provided " +
                    "offer has already expired");
            }

            if (offer.validAfter().isAfter(offer.validUntil())) {
                throw new ModelException("BAD_TIME_CONSTRAINTS", "The " +
                    "provided offer is configured to expire before it becomes " +
                    "acceptable");
            }

            verifyIsApproximatelyEqualTo(offer.signature().timestamp(), now);

            if (!offer.signature().verify(offeror.certificate(), offer.toCanonicalJson())) {
                throw new ModelException("BAD_SIGNATURE", "The signature in " +
                    "the provided offer does not match its contents when " +
                    "verified with the public key of \"" +
                    offeror.commonName() + "\"");
            }
        }

        loggedOffers.add(offer);
        activeParty = receiver;
        state = STATE_OFFERING;
    }

    private void update(final SignedContractRejection rejection, final Party rejector)
        throws ModelException
    {
        Objects.requireNonNull(rejection, "Expected rejection");
        Objects.requireNonNull(rejector, "Expected rejector");
        if (rejection.sessionId() != id) {
            throw new IllegalArgumentException("Session ID " + id + " not in " + rejection);
        }

        final var now = Instant.now();

        verifyStateIsOffering(now);

        if (activeParty != rejector) {
            throw new ModelException("AWAIT_TURN", "You made the last " +
                "offer and must, therefore, wait until the session " +
                "expires or your counter-party responds; cannot reject offer");
        }

        if (!(rejector instanceof OwnedParty)) {
            final var rejectedOfferHash = rejection.offerHash().toHash();
            final var rejectedOfferHashAlgorithm = rejectedOfferHash.algorithm();
            if (!rejector.acceptedHashAlgorithms().contains(rejectedOfferHashAlgorithm)) {
                throw new ModelException("UNSUPPORTED_HASH_ALGORITHM", "" +
                    "The offer hash in the provided rejection uses the " +
                    rejectedOfferHashAlgorithm + " algorithm, but only " +
                    rejector.acceptedHashAlgorithms() + " are supported; cannot " +
                    "reject offer");
            }
            final var lastOffer = loggedOffers.get(loggedOffers.size() - 1);
            final var lastOfferHash = rejectedOfferHashAlgorithm.hash(lastOffer.toCanonicalJson());
            if (!rejectedOfferHash.equals(lastOfferHash)) {
                throw new ModelException("BAD_HASH", "The offer hash in the " +
                    "provided rejection does not match that of the last " +
                    "session offer; cannot reject offer");
            }

            verifyIsApproximatelyEqualTo(rejection.signature().timestamp(), now);

            if (!rejection.signature().verify(rejector.certificate(), rejection.toCanonicalJson())) {
                throw new ModelException("BAD_SIGNATURE", "The signature " +
                    "in the provided rejection does not match its contents " +
                    "when verified with the public key of \"" +
                    rejector.commonName() + "\"; cannot reject offer");
            }
        }

        loggedRejection = rejection;
        activeParty = null;
        state = STATE_REJECTED;
    }

    private void verifyIsApproximatelyEqualTo(final Instant timestamp, final Instant now) throws ModelException {
        if (timestamp.isBefore(now.minus(CLOCK_SKEW_TOLERANCE)) || timestamp.isAfter(now.plus(CLOCK_SKEW_TOLERANCE))) {
            throw new ModelException("BAD_TIMESTAMP", "The timestamp in " +
                "the provided message " + timestamp + " does not seem to " +
                "represent a time close to the current time " + now);
        }
    }

    private void verifyStateIsOffering(final Instant now) throws ModelException {
        if (state == STATE_OFFERING) {
            assert loggedOffers.size() > 0;
            final var lastOffer = loggedOffers.get(loggedOffers.size() - 1);
            if (lastOffer.validUntil().isAfter(now.minus(CLOCK_SKEW_TOLERANCE))) {
                return;
            }
            state = STATE_EXPIRED;
        }
        throw new ModelException("SESSION_CLOSED", "New messages " +
            "cannot be added to this session, it is closed");
    }

    @Override
    public String toString() {
        return "Session{" +
            "ownedParty=" + ownedParty.commonName() +
            ", counterParty=" + counterParty.commonName() +
            ", id=" + id +
            '}';
    }
}
