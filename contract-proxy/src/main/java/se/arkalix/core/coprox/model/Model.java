package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.dto.*;
import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.security.HashFunction;
import se.arkalix.internal.security.identity.X509Names;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;

public class Model {
    private final Certificate ownedCertificate;
    private final Set<Hash> ownedFingerprints;
    private final String ownedName;
    private final PrivateKey ownedPrivateKey;

    private final Publisher publisher;

    private final Map<Hash, Party> fingerprintToTrustedCounterParty;
    private final Map<Hash, Template> hashToTemplate;
    private final Set<HashFunction> trustedHashFunctions;


    private Model(final Builder builder) throws CertificateEncodingException {
        ownedCertificate = Objects.requireNonNull(builder.ownedCertificate, "Expected ownedCertificate");
        ownedPrivateKey = Objects.requireNonNull(builder.ownedPrivateKey, "Expected ownedPrivateKey");
        publisher = Objects.requireNonNull(builder.publisher, "Expected publisher");
        Objects.requireNonNull(builder.templates, "Expected templates");
        Objects.requireNonNull(builder.trustedCounterParties, "Expected trustedCounterParties");
        trustedHashFunctions = Objects.requireNonNull(builder.trustedHashFunctions, "Expected trustedHashFunctions");

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
            this.ownedName = X509Names.commonNameOf(((X509Certificate) ownedCertificate)
                .getSubjectX500Principal()
                .getName())
                .orElseThrow(() -> new IllegalArgumentException("Expected " +
                    "ownedCertificate to contain subject common name"));
        }

        {
            final var fingerprintToTrustedCounterParty = new HashMap<Hash, Party>();
            for (final var party : builder.trustedCounterParties) {
                final var encodedTrustedCertificate = party.certificate().getEncoded();
                for (final var function : trustedHashFunctions) {
                    final var fingerprint = function.hash(encodedTrustedCertificate);
                    fingerprintToTrustedCounterParty.put(fingerprint, party);
                }
            }
            this.fingerprintToTrustedCounterParty = Collections.unmodifiableMap(fingerprintToTrustedCounterParty);
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

    public void update(final TrustedAcceptance trustedAcceptance) {

    }

    public void update(final TrustedOffer trustedOffer) {

    }

    public void update(final TrustedRejection trustedRejection) {

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

        public Builder trustedFingerprintFunctions(final Set<HashFunction> trustedFingerprintFunctions) {
            this.trustedHashFunctions = trustedFingerprintFunctions;
            return this;
        }

        public Model build() throws CertificateEncodingException {
            return new Model(this);
        }
    }
}
