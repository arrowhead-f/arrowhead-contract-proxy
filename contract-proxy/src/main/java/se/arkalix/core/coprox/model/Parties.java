package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;

import java.util.*;

public class Parties {
    private final Map<String, Party> commonNameToParty;
    private final Map<Hash, Party> fingerprintToParty;

    public Parties(final Collection<Party> parties) {
        final var commonNameToParty = new HashMap<String, Party>();
        final var fingerprintToParty = new HashMap<Hash, Party>();
        for (final var party : parties) {
            var conflictingParty = commonNameToParty.put(party.commonName(), party);
            if (conflictingParty != null) {
                throw new IllegalArgumentException("There are at least two " +
                    "provided certificates with the same common name \"" +
                    conflictingParty.commonName() + "\"; this prevents " +
                    "the construction of a non-ambiguous mapping between " +
                    "common names and counter-parties");
            }

            for (final var fingerprint : party.acceptedFingerprints()) {
                conflictingParty = fingerprintToParty.put(fingerprint, party);
                if (conflictingParty != null) {
                    throw new IllegalArgumentException("There are at least " +
                        "two provided certificates that share the same " +
                        "fingerprint " + fingerprint + "; this prevents the " +
                        "construction of a non-ambiguous mapping between " +
                        "fingerprints and counter-parties");
                }
            }
        }
        this.commonNameToParty = Collections.unmodifiableMap(commonNameToParty);
        this.fingerprintToParty = Collections.unmodifiableMap(fingerprintToParty);
    }

    public Optional<Party> getAnyByCommonName(final String commonName) {
        return Optional.ofNullable(commonNameToParty.get(commonName));
    }

    public Optional<Party> getAnyByFingerprint(final Hash fingerprint) {
        return Optional.ofNullable(fingerprintToParty.get(fingerprint));
    }

    public Optional<OwnedParty> getOwnedPartyByCommonName(final String commonName) {
        return getAnyByCommonName(commonName)
            .flatMap(party -> party instanceof OwnedParty
                ? Optional.of((OwnedParty) party)
                : Optional.empty());
    }

    public Optional<OwnedParty> getOwnedPartyByFingerprint(final Hash fingerprint) {
        return getAnyByFingerprint(fingerprint)
            .flatMap(party -> party instanceof OwnedParty
                ? Optional.of((OwnedParty) party)
                : Optional.empty());
    }

    public Optional<Party> getCounterPartyByCommonName(final String commonName) {
        return getAnyByCommonName(commonName)
            .flatMap(party -> party instanceof OwnedParty
                ? Optional.empty()
                : Optional.of(party));
    }

    public Optional<Party> getCounterPartyByFingerprint(final Hash fingerprint) {
        return getAnyByFingerprint(fingerprint)
            .flatMap(party -> party instanceof OwnedParty
                ? Optional.empty()
                : Optional.of(party));
    }
}
