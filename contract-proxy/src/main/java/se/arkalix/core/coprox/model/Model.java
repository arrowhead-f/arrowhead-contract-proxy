package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.dto.*;
import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.security.HashFunction;
import se.arkalix.core.coprox.security.SignatureScheme;
import se.arkalix.internal.security.identity.X509Names;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Model {
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(30);

    private final SignatureScheme defaultSignatureScheme;
    private final Certificate ownedCertificate;
    private final Set<Hash> ownedFingerprints;
    private final String ownedName;
    private final PrivateKey ownedPrivateKey;

    private final Publisher publisher;

    private final Map<String, Party> commonNameToTrustedCounterParty;
    private final Map<Hash, Party> fingerprintToTrustedCounterParty;
    private final Map<Hash, Template> hashToTemplate;
    private final Map<Party, Hash> trustedCounterPartyToDefaultFingerprint;
    private final Set<HashFunction> trustedHashFunctions;


    private Model(final Builder builder) throws CertificateEncodingException {
        ownedCertificate = Objects.requireNonNull(builder.ownedCertificate, "Expected ownedCertificate");
        ownedPrivateKey = Objects.requireNonNull(builder.ownedPrivateKey, "Expected ownedPrivateKey");
        publisher = Objects.requireNonNull(builder.publisher, "Expected publisher");
        Objects.requireNonNull(builder.templates, "Expected templates");
        Objects.requireNonNull(builder.trustedCounterParties, "Expected trustedCounterParties");
        trustedHashFunctions = Objects.requireNonNull(builder.trustedHashFunctions, "Expected trustedHashFunctions");
        if (trustedHashFunctions.size() == 0) {
            throw new IllegalArgumentException("Expected at least one item in trustedHashFunctions");
        }

        {
            defaultSignatureScheme = SignatureScheme.all()
                .stream()
                .filter(scheme -> ownedPrivateKey.getAlgorithm().equalsIgnoreCase(scheme.algorithm()) &&
                    trustedHashFunctions.contains(scheme.hashFunction()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expected " +
                    "ownedPrivateKey to support at least one known signature " +
                    "scheme that relies on one of trustedHashFunctions; the " +
                    "known signature schemes are: " + SignatureScheme.all()));
        }

        {
            final var ownedFingerprints = new HashSet<Hash>();
            final var encodedOwnedCertificate = ownedCertificate.getEncoded();
            for (final var function : trustedHashFunctions) {
                final var fingerprint = function.hash(encodedOwnedCertificate);
                ownedFingerprints.add(fingerprint);
            }
            this.ownedFingerprints = Collections.unmodifiableSet(ownedFingerprints);
        }

        {
            if (!(ownedCertificate instanceof X509Certificate)) {
                throw new IllegalArgumentException("Expected ownedCertificate to be of type x.509");
            }
            final var certificate = (X509Certificate) ownedCertificate;
            this.ownedName = X509Names.commonNameOf(certificate.getSubjectX500Principal().getName())
                .orElseThrow(() -> new IllegalArgumentException("Expected " +
                    "ownedCertificate to contain subject common name"));
        }

        {
            final var commonNameToTrustedCounterParty = new HashMap<String, Party>();
            for (final var party : builder.trustedCounterParties) {
                commonNameToTrustedCounterParty.put(party.name(), party);
            }
            this.commonNameToTrustedCounterParty = Collections.unmodifiableMap(commonNameToTrustedCounterParty);
        }

        {
            final var fingerprintToTrustedCounterParty = new HashMap<Hash, Party>();
            final var trustedCounterPartyToDefaultFingerprint = new HashMap<Party, Hash>();
            for (final var party : builder.trustedCounterParties) {
                final var encodedTrustedCertificate = party.certificate().getEncoded();
                Hash defaultFingerprint = null;
                for (final var function : trustedHashFunctions) {
                    final var fingerprint = function.hash(encodedTrustedCertificate);
                    if (defaultFingerprint == null) {
                        defaultFingerprint = fingerprint;
                    }
                    fingerprintToTrustedCounterParty.put(fingerprint, party);
                }
                trustedCounterPartyToDefaultFingerprint.put(party, defaultFingerprint);
            }
            this.fingerprintToTrustedCounterParty = Collections.unmodifiableMap(fingerprintToTrustedCounterParty);
            this.trustedCounterPartyToDefaultFingerprint =
                Collections.unmodifiableMap(trustedCounterPartyToDefaultFingerprint);
        }

        {
            final var hashToTemplate = new HashMap<Hash, Template>();
            for (final var template : builder.templates) {
                final var textAsBytes = template.text().getBytes(StandardCharsets.UTF_8);
                for (final var function : trustedHashFunctions) {
                    final var hash = function.hash(textAsBytes);
                    hashToTemplate.put(hash, template);
                }
            }
            this.hashToTemplate = Collections.unmodifiableMap(hashToTemplate);
        }
    }

    public void update(final SignedAcceptance signedAcceptance) throws BadRequestException {
        final var signedOffer = signedAcceptance.offer();

        final var offerorFingerprint = signedOffer.offerorFingerprint().toHash();
        validateFingerprintFunction(offerorFingerprint);
        if (!ownedFingerprints.contains(offerorFingerprint)) {
            throw new BadRequestException("Acceptance must be sent to original offeror");
        }

        final var receiverFingerprint = signedOffer.receiverFingerprint().toHash();
        validateFingerprintFunction(receiverFingerprint);
        final var counterParty = fingerprintToTrustedCounterParty.get(receiverFingerprint);
        if (counterParty == null) {
            throw new BadRequestException("Accepted offer receiver (acceptor) seems to no longer be whitelisted");
        }

        final var acceptance = signedAcceptance.toAcceptance();

        if (!acceptance.offer().signature().verify(ownedCertificate, signedOffer.toCanonicalJson())) {
            throw new BadSignatureSumException(signedOffer.signature().sumAsBase64());
        }

        if (!acceptance.signature().verify(counterParty.certificate(), signedAcceptance.toCanonicalJson())) {
            throw new BadSignatureSumException(signedAcceptance.signature().sumAsBase64());
        }

        final var templates = lookupContractTemplates(acceptance.offer().contracts());
        final var sessionId = signedOffer.sessionId();

        counterParty.updateSession(sessionId, acceptance);

        for (final var template : templates) {
            publisher.onAccept(sessionId, ownedName, counterParty.name(), template.name());
        }
    }

    public void update(final SignedOffer signedOffer) throws BadRequestException {
        final var offerorFingerprint = signedOffer.offerorFingerprint().toHash();
        validateFingerprintFunction(offerorFingerprint);
        final var counterParty = fingerprintToTrustedCounterParty.get(offerorFingerprint);
        if (counterParty == null) {
            throw new BadRequestException("Identified offeror not whitelisted");
        }

        final var receiverFingerprint = signedOffer.receiverFingerprint().toHash();
        validateFingerprintFunction(receiverFingerprint);
        if (!ownedFingerprints.contains(receiverFingerprint)) {
            throw new BadRequestException("Identified offer receiver is not the actual receiver");
        }

        final var offer = signedOffer.toOffer();

        if (!offer.signature().verify(counterParty.certificate(), signedOffer.toCanonicalJson())) {
            throw new BadSignatureSumException(signedOffer.signature().sumAsBase64());
        }

        final var templates = lookupContractTemplates(offer.contracts());
        final var sessionId = signedOffer.sessionId();

        counterParty.updateSession(sessionId, offer);

        for (final var template : templates) {
            publisher.onOffer(sessionId, counterParty.name(), ownedName, template.name());
        }
    }

    public void update(final SignedRejection signedRejection) throws BadRequestException {
        final var rejectorFingerprint = signedRejection.rejectorFingerprint().toHash();
        validateFingerprintFunction(rejectorFingerprint);
        final var counterParty = fingerprintToTrustedCounterParty.get(rejectorFingerprint);
        if (counterParty == null) {
            throw new BadRequestException("Identified rejector not whitelisted");
        }

        final var receiverFingerprint = signedRejection.receiverFingerprint().toHash();
        validateFingerprintFunction(receiverFingerprint);
        if (!ownedFingerprints.contains(receiverFingerprint)) {
            throw new BadRequestException("Identified offer receiver is not the actual receiver");
        }

        final var rejection = signedRejection.toRejection();

        if (!rejection.signature().verify(counterParty.certificate(), signedRejection.toCanonicalJson())) {
            throw new BadSignatureSumException(signedRejection.signature().sumAsBase64());
        }

        final var sessionId = signedRejection.sessionId();

        final var previousSession = counterParty.updateSession(sessionId, rejection);

        final var rejectedOffer = (Offer) previousSession.candidate();
        final String offerorName;
        final String receiverName;
        if (ownedFingerprints.contains(rejectedOffer.offerorFingerprint())) {
            offerorName = ownedName;
            receiverName = fingerprintToTrustedCounterParty.get(rejectedOffer.receiverFingerprint()).name();
        }
        else {
            offerorName = fingerprintToTrustedCounterParty.get(rejectedOffer.offerorFingerprint()).name();
            receiverName = ownedName;
        }

        if (offerorName == null || receiverName == null) {
            throw new IllegalStateException("Failed to resolve names of " +
                "offeror and receiver of rejected session with id " + sessionId);
        }

        final var templates = lookupContractTemplates(rejectedOffer.contracts());
        for (final var template : templates) {
            publisher.onOffer(sessionId, offerorName, receiverName, template.name());
        }
    }

    public void update(final TrustedAcceptance trustedAcceptance) throws BadRequestException {
        validateTimestamp(trustedAcceptance.acceptedAt());
    }

    public void update(final TrustedOfferDto trustedOffer) throws BadRequestException {
        validateTimestamp(trustedOffer.offeredAt());

        if (!Objects.equals(trustedOffer.offerorName(), ownedName)) {
            throw new BadRequestException("Expected offerorName to be \"" +
                ownedName + "\", but was \"" + trustedOffer.offerorName() +
                "\"; cannot make offer");
        }

        final var counterParty = commonNameToTrustedCounterParty.get(trustedOffer.receiverName());
        if (counterParty == null) {
            throw new BadRequestException("No known counter-party has the " +
                "common name \"" + trustedOffer.receiverName() +
                "\"; cannot make offer");
        }

        final var counterPartyFingerprint = trustedCounterPartyToDefaultFingerprint.get(counterParty);
        if (counterPartyFingerprint == null) {
            throw new IllegalStateException("No default fingerprint " +
                "associated with offer receiver \"" +
                trustedOffer.receiverName() + "\"; cannot make offer");
        }

        final var ownedFingerprint = ownedFingerprints.stream()
            .filter(fingerprint -> Objects.equals(fingerprint.function(), counterPartyFingerprint.function()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No default " +
                "fingerprint of type \"" + counterPartyFingerprint.function() +
                "\" associated with offeror; cannot make offer"));

        final var unsignedOffer = new SignedOfferBuilder()
            .sessionId(trustedOffer.sessionId())
            .offerorFingerprint(se.arkalix.core.coprox.dto.Hash.fromHash(ownedFingerprint))
            .receiverFingerprint(se.arkalix.core.coprox.dto.Hash.fromHash(counterPartyFingerprint))
            .validAfter(trustedOffer.validAfter())
            .validUntil(trustedOffer.validUntil())
            .contracts(trustedOffer.contractsDto())
            .signature(new SignatureBuilder()
                .timestamp(trustedOffer.offeredAt())
                .scheme(defaultSignatureScheme)
                .sumAsBase64("")
                .build())
            .build();

        final var signature = defaultSignatureScheme
            .sign(ownedPrivateKey, trustedOffer.offeredAt(), unsignedOffer.toCanonicalJson());

        final var signedOffer = unsignedOffer.rebuild()
            .signature(new SignatureBuilder()
                .timestamp(signature.timestamp())
                .scheme(signature.scheme())
                .sumAsBase64(Base64.getEncoder().encodeToString(signature.sum()))
                .build())
            .build();

        // TODO: Send message to counter-party. If fails, do not call update.


        update(signedOffer);
    }

    public void update(final TrustedRejection trustedRejection) throws BadRequestException {
        validateTimestamp(trustedRejection.rejectedAt());
    }

    private Set<Template> lookupContractTemplates(final Collection<Contract> contracts) throws BadRequestException {
        final var templates = new HashSet<Template>(contracts.size());
        for (final var contract : contracts) {
            final var template = hashToTemplate.get(contract.templateHash());
            if (template == null) {
                throw new BadRequestException("Unknown contract template identified: " + contract.templateHash());
            }
            templates.add(template);
        }
        return templates;
    }

    private void validateFingerprintFunction(final Hash fingerprint) throws BadHashFunctionExeption {
        final var function = fingerprint.function();
        if (!trustedHashFunctions.contains(function)) {
            throw new BadHashFunctionExeption(function);
        }
    }

    private void validateTimestamp(final Instant timestamp) throws BadRequestException {
        if (!Duration.between(Instant.now(), timestamp).abs().minus(CLOCK_SKEW_TOLERANCE).isNegative()) {
            throw new BadRequestException("Time difference too significant");
        }
    }

    public static class Builder {
        private Certificate ownedCertificate;
        private PrivateKey ownedPrivateKey;
        private Publisher publisher;
        private List<Template> templates;
        private List<Party> trustedCounterParties;
        private Set<HashFunction> trustedHashFunctions;

        public Builder ownedCertificate(final Certificate ownedCertificate) {
            this.ownedCertificate = ownedCertificate;
            return this;
        }

        public Builder ownedPrivateKey(final PrivateKey ownedPrivateKey) {
            this.ownedPrivateKey = ownedPrivateKey;
            return this;
        }

        public Builder publisher(final Publisher publisher) {
            this.publisher = publisher;
            return this;
        }

        public Builder templates(final List<Template> templates) {
            this.templates = templates;
            return this;
        }

        public Builder trustedCounterParties(final List<Party> trustedCounterParties) {
            this.trustedCounterParties = trustedCounterParties;
            return this;
        }

        public Builder trustedHashFunctions(final Set<HashFunction> trustedHashFunctions) {
            this.trustedHashFunctions = trustedHashFunctions;
            return this;
        }

        public Model build() throws CertificateEncodingException {
            return new Model(this);
        }
    }
}
