package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashAlgorithm;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ContractNegotiations {
    private final SecureRandom random = new SecureRandom();
    private final Map<PairKey, Map<Long, ContractNegotiation>> negotiations = new ConcurrentHashMap<>();
    private final Templates templates;
    private final Set<HashAlgorithm> acceptedHashAlgorithms;

    public ContractNegotiations(final Templates templates, final Set<HashAlgorithm> acceptedHashAlgorithms) {
        this.templates = templates;
        this.acceptedHashAlgorithms = acceptedHashAlgorithms;
    }

    public ContractNegotiation createFor(final OwnedParty ownedParty, final Party counterParty) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        final var pairNegotiations = negotiations.computeIfAbsent(key, key0 -> new ConcurrentHashMap<>());

        long id;
        var attempts = 32;
        ContractNegotiation newNegotiation;
        ContractNegotiation existingNegotiation;
        do {
            do {
                id = random.nextLong();
            } while (pairNegotiations.containsKey(id) && attempts-- != 0);

            if (attempts < 0) {
                throw new IllegalStateException("Failed to find unique negotiation " +
                    "id; cannot create new negotiation for \"" +
                    ownedParty.commonName() + "\" and \"" +
                    counterParty.commonName() + "\"");
            }

            newNegotiation = new ContractNegotiation(ownedParty, counterParty, id, templates, acceptedHashAlgorithms);
            existingNegotiation = pairNegotiations.putIfAbsent(id, newNegotiation);
        } while (existingNegotiation != null);

        return newNegotiation;
    }

    public Optional<ContractNegotiation> getBy(final OwnedParty ownedParty, final Party counterParty, final long id) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        return Optional.ofNullable(negotiations.get(key))
            .flatMap(pairNegotiations -> Optional.ofNullable(pairNegotiations.get(id)));
    }

    public ContractNegotiation getOrCreateBy(final OwnedParty ownedParty, final Party counterParty, final long id) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        final var pairNegotiations = negotiations.computeIfAbsent(key, key0 -> new ConcurrentHashMap<>());
        return pairNegotiations.computeIfAbsent(id, id0 ->
            new ContractNegotiation(ownedParty, counterParty, id, templates, acceptedHashAlgorithms));
    }

    private static class PairKey {
        private final String ownedPartyName;
        private final String counterPartyName;

        private PairKey(final String ownedPartyName, final String counterPartyName) {
            this.ownedPartyName = Objects.requireNonNull(ownedPartyName, "Expected ownedPartyName");
            this.counterPartyName = Objects.requireNonNull(counterPartyName, "Expected counterPartyName");
        }

        @Override
        public int hashCode() {
            return Objects.hash(ownedPartyName, counterPartyName);
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (other == null || getClass() != other.getClass()) { return false; }
            final var that = (PairKey) other;
            return ownedPartyName.equals(that.ownedPartyName) &&
                counterPartyName.equals(that.counterPartyName);
        }
    }
}
