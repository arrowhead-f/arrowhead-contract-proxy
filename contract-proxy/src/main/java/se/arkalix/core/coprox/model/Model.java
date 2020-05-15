package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.core.coprox.security.SignatureBase64;
import se.arkalix.core.coprox.security.SignatureBase64Builder;
import se.arkalix.core.plugin.cp.*;

import java.util.*;

public class Model {
    private final ContractSessionObserver observer;
    private final Parties parties;
    private final ContractSessions sessions = new ContractSessions();
    private final Templates templates;

    public Model(final Builder builder) {
        observer = builder.observer;
        Objects.requireNonNull(builder.ownedParties, "Expected ownedParties");
        Objects.requireNonNull(builder.counterParties, "Expected counterParties");
        Objects.requireNonNull(builder.templates, "Expected templates");

        if (builder.ownedParties.isEmpty()) {
            throw new IllegalArgumentException("Expected ownedParties.size() > 0");
        }
        if (builder.counterParties.isEmpty()) {
            throw new IllegalArgumentException("Expected counterParties.size() > 0");
        }
        if (builder.templates.isEmpty()) {
            throw new IllegalArgumentException("Expected templates.size() > 0");
        }

        final var allParties = new ArrayList<Party>(builder.ownedParties.size() + builder.counterParties.size());
        allParties.addAll(builder.ownedParties);
        allParties.addAll(builder.counterParties);
        parties = new Parties(allParties);
        templates = new Templates(builder.templates);
    }

    public void update(final SignedContractAcceptanceDto acceptance) throws ModelException {

    }

    public void update(final SignedContractOfferDto offer) throws ModelException {

    }

    public void update(final SignedContractRejectionDto rejection) throws ModelException {

    }

    public void update(final TrustedContractAcceptanceDto acceptance) throws ModelException {

    }

    public void update(final TrustedContractCounterOfferDto counterOffer) throws ModelException {
        Objects.requireNonNull(counterOffer, "Expected counterOffer");

        final var offeror = parties.getOwnedPartyByCommonName(counterOffer.offerorName()).orElse(null);
        if (offeror == null) {
            throw new ModelException("UNKNOWN_OFFEROR", "This ContractProxy " +
                "does not own any identity named \"" + counterOffer.offerorName() +
                "\"; cannot make counter-offer");
        }

        final var receiver = parties.getCounterPartyByCommonName(counterOffer.receiverName()).orElse(null);
        if (receiver == null) {
            throw new ModelException("UNKNOWN_RECEIVER", "This ContractProxy " +
                "does not know of any counter-party identity named \"" +
                counterOffer.receiverName() + "\"; cannot make counter-offer");
        }

        final var session = sessions.getBy(offeror, receiver, counterOffer.sessionId()).orElse(null);
        if (session == null) {
            throw new ModelException("UNKNOWN_SESSION", "No session with ID " +
                counterOffer.sessionId() + " exists for the parties \"" +
                offeror.commonName() + "\" and \"" + receiver.commonName() +
                "\"; cannot make counter-offer");
        }

        final var contracts = new ArrayList<ContractBase64Dto>(counterOffer.contracts().size());
        final var templateNames = new HashSet<String>(counterOffer.contracts().size());
        for (final var contract : counterOffer.contracts()) {
            final var templateName = contract.templateName();
            final var template = templates.getByName(contract.templateName()).orElse(null);
            if (template == null) {
                throw new ModelException("UNKNOWN_TEMPLATE", "This " +
                    "ContractProxy does not know of any contract template " +
                    "named \"" + contract.templateName() + "\"; cannot make " +
                    "offer");
            }
            contracts.add(new ContractBase64Builder()
                .templateHash(HashBase64.from(template.preferredHash()))
                .arguments(contract.arguments())
                .build());
            templateNames.add(templateName);
        }

        final var unsignedOffer = new SignedContractOfferBuilder()
            .sessionId(session.id())
            .offerorFingerprint(HashBase64.from(offeror.preferredFingerprint()))
            .receiverFingerprint(HashBase64.from(receiver.preferredFingerprint()))
            .validAfter(counterOffer.validAfter())
            .validUntil(counterOffer.validUntil())
            .contracts(contracts)
            .signature(new SignatureBase64Builder()
                .timestamp(counterOffer.offeredAt())
                .scheme(offeror.signatureScheme())
                .sum("")
                .build())
            .build();

        final var signature = offeror.sign(unsignedOffer.signature().timestamp(), unsignedOffer.toCanonicalJson());

        final var signedOffer = unsignedOffer.rebuild()
            .signature(SignatureBase64.from(signature))
            .build();

        session.updateForOwnedParty(signedOffer);

        if (observer != null) {
            observer.onEvent(new ContractSessionEvent.Builder()
                .sessionId(session.id())
                .offerorName(counterOffer.offerorName())
                .receiverName(counterOffer.receiverName())
                .status(ContractSessionStatus.OFFERING)
                .templateNames(templateNames)
                .build());
        }
    }

    public void update(final TrustedContractOfferDto offer) throws ModelException {
        Objects.requireNonNull(offer, "Expected offer");

        final var offeror = parties.getOwnedPartyByCommonName(offer.offerorName()).orElse(null);
        if (offeror == null) {
            throw new ModelException("UNKNOWN_OFFEROR", "This ContractProxy " +
                "does not own any identity named \"" + offer.offerorName() +
                "\"; cannot make offer");
        }

        final var receiver = parties.getCounterPartyByCommonName(offer.receiverName()).orElse(null);
        if (receiver == null) {
            throw new ModelException("UNKNOWN_RECEIVER", "This ContractProxy " +
                "does not know of any counter-party identity named \"" +
                offer.receiverName() + "\"; cannot make offer");
        }

        final var session = sessions.createFor(offeror, receiver);

        final var contracts = new ArrayList<ContractBase64Dto>(offer.contracts().size());
        final var templateNames = new HashSet<String>(offer.contracts().size());
        for (final var contract : offer.contracts()) {
            final var templateName = contract.templateName();
            final var template = templates.getByName(contract.templateName()).orElse(null);
            if (template == null) {
                throw new ModelException("UNKNOWN_TEMPLATE", "This " +
                    "ContractProxy does not know of any contract template " +
                    "named \"" + contract.templateName() + "\"; cannot make " +
                    "offer");
            }
            contracts.add(new ContractBase64Builder()
                .templateHash(HashBase64.from(template.preferredHash()))
                .arguments(contract.arguments())
                .build());
            templateNames.add(templateName);
        }

        final var unsignedOffer = new SignedContractOfferBuilder()
            .sessionId(session.id())
            .offerorFingerprint(HashBase64.from(offeror.preferredFingerprint()))
            .receiverFingerprint(HashBase64.from(receiver.preferredFingerprint()))
            .validAfter(offer.validAfter())
            .validUntil(offer.validUntil())
            .contracts(contracts)
            .signature(new SignatureBase64Builder()
                .timestamp(offer.offeredAt())
                .scheme(offeror.signatureScheme())
                .sum("")
                .build())
            .build();

        final var signature = offeror.sign(unsignedOffer.signature().timestamp(), unsignedOffer.toCanonicalJson());

        final var signedOffer = unsignedOffer.rebuild()
            .signature(SignatureBase64.from(signature))
            .build();

        session.updateForOwnedParty(signedOffer);

        if (observer != null) {
            observer.onEvent(new ContractSessionEvent.Builder()
                .sessionId(session.id())
                .offerorName(offer.offerorName())
                .receiverName(offer.receiverName())
                .status(ContractSessionStatus.OFFERING)
                .templateNames(templateNames)
                .build());
        }
    }

    public void update(final TrustedContractRejectionDto rejection) throws ModelException {

    }

    public static class Builder {
        private ContractSessionObserver observer;
        private List<Template> templates;
        private List<OwnedParty> ownedParties;
        private List<Party> counterParties;

        public Builder observer(final ContractSessionObserver observer) {
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

    /*
    public static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofSeconds(30);

    private final SignatureScheme defaultSignatureScheme;
    private final Certificate ownedCertificate;
    private final Set<Hash> ownedFingerprints;
    private final String ownedName;
    private final PrivateKey ownedPrivateKey;

    private final Publisher publisher;

    private final Map<String, CounterParty> commonNameToTrustedCounterParty;
    private final Map<Hash, CounterParty> fingerprintToTrustedCounterParty;
    private final Map<Hash, Template> hashToTemplate;
    private final Map<CounterParty, Hash> trustedCounterPartyToDefaultFingerprint;
    private final Set<HashAlgorithm> trustedHashAlgorithms;

    private Model(final Builder builder) throws CertificateEncodingException {
        ownedCertificate = Objects.requireNonNull(builder.ownedCertificate, "Expected ownedCertificate");
        ownedPrivateKey = Objects.requireNonNull(builder.ownedPrivateKey, "Expected ownedPrivateKey");
        publisher = Objects.requireNonNull(builder.publisher, "Expected publisher");
        Objects.requireNonNull(builder.templates, "Expected templates");
        Objects.requireNonNull(builder.trustedCounterParties, "Expected trustedCounterParties");
        trustedHashAlgorithms = Objects.requireNonNull(builder.trustedHashAlgorithms, "Expected trustedHashAlgorithms");
        if (trustedHashAlgorithms.size() == 0) {
            throw new IllegalArgumentException("Expected at least one item in trustedHashAlgorithms");
        }

        {
            defaultSignatureScheme = SignatureScheme.ALL
                .stream()
                .filter(scheme -> ownedPrivateKey.getAlgorithm().equalsIgnoreCase(scheme.keyAlgorithmName()) &&
                    trustedHashAlgorithms.contains(scheme.hashAlgorithm()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Expected " +
                    "ownedPrivateKey to support at least one known signature " +
                    "scheme that relies on one of trustedHashAlgorithms; the " +
                    "known signature schemes are: " + SignatureScheme.ALL));
        }

        {
            final var ownedFingerprints = new HashSet<Hash>();
            final var encodedOwnedCertificate = ownedCertificate.getEncoded();
            for (final var function : trustedHashAlgorithms) {
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
                    "ownedCertificate to contain a subject common name"));
        }

        {
            final var commonNameToTrustedCounterParty = new HashMap<String, CounterParty>();
            for (final var party : builder.trustedCounterParties) {
                commonNameToTrustedCounterParty.put(party.name(), party);
            }
            this.commonNameToTrustedCounterParty = Collections.unmodifiableMap(commonNameToTrustedCounterParty);
        }

        {
            final var fingerprintToTrustedCounterParty = new HashMap<Hash, CounterParty>();
            final var trustedCounterPartyToDefaultFingerprint = new HashMap<CounterParty, Hash>();
            for (final var party : builder.trustedCounterParties) {
                final var encodedTrustedCertificate = party.certificate().getEncoded();
                Hash defaultFingerprint = null;
                for (final var function : trustedHashAlgorithms) {
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
                for (final var function : trustedHashAlgorithms) {
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
            throw new BadSignatureSumException(signedOffer.signature().sum());
        }

        if (!acceptance.signature().verify(counterParty.certificate(), signedAcceptance.toCanonicalJson())) {
            throw new BadSignatureSumException(signedAcceptance.signature().sum());
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
            throw new BadSignatureSumException(signedOffer.signature().sum());
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
            throw new BadSignatureSumException(signedRejection.signature().sum());
        }

        final var sessionId = signedRejection.sessionId();

        final var previousSession = counterParty.updateSession(sessionId, rejection);

        final var rejectedOffer = (Offer) previousSession.offer();
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

    public void update(final TrustedAcceptanceDto trustedAcceptance) throws BadRequestException {
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
            .filter(fingerprint -> Objects.equals(fingerprint.algorithm(), counterPartyFingerprint.algorithm()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No default " +
                "fingerprint of type \"" + counterPartyFingerprint.algorithm() +
                "\" associated with offeror; cannot make offer"));

        final var unsignedOffer = new SignedOfferBuilder()
            .sessionId(trustedOffer.sessionId())
            .offerorFingerprint(HashBase64.from(ownedFingerprint))
            .receiverFingerprint(HashBase64.from(counterPartyFingerprint))
            .validAfter(trustedOffer.validAfter())
            .validUntil(trustedOffer.validUntil())
            //.contracts(trustedOffer.contractsAsDtos()) TODO: Map trusted contract to signed contract (base64)
            .signature(new SignatureBase64Builder()
                .timestamp(trustedOffer.offeredAt())
                .scheme(defaultSignatureScheme)
                .sum("")
                .build())
            .build();

        final var signature = defaultSignatureScheme
            .sign(ownedPrivateKey, trustedOffer.offeredAt(), unsignedOffer.toCanonicalJson());

        final var signedOffer = unsignedOffer.rebuild()
            .signature(new SignatureBase64Builder()
                .timestamp(signature.timestamp())
                .scheme(signature.scheme())
                .sum(Base64.getEncoder().encodeToString(signature.sum()))
                .build())
            .build();

        // TODO: Send message to counter-party. If fails, do not call update.


        update(signedOffer);
    }

    public void update(final TrustedRejectionDto trustedRejection) throws BadRequestException {
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
        final var function = fingerprint.algorithm();
        if (!trustedHashAlgorithms.contains(function)) {
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
        private List<CounterParty> trustedCounterParties;
        private Set<HashAlgorithm> trustedHashAlgorithms;

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

        public Builder trustedCounterParties(final List<CounterParty> trustedCounterParties) {
            this.trustedCounterParties = trustedCounterParties;
            return this;
        }

        public Builder trustedHashAlgorithms(final Set<HashAlgorithm> trustedHashAlgorithms) {
            this.trustedHashAlgorithms = trustedHashAlgorithms;
            return this;
        }

        public Model build() throws CertificateEncodingException {
            return new Model(this);
        }
    }

     */
}
