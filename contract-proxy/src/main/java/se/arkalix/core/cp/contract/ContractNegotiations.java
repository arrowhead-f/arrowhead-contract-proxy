package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.HashAlgorithm;

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

    public Optional<ContractNegotiation> getBy(final Party party1, final Party party2, final long id) {
        final var key = new PairKey(party1.commonName(), party2.commonName());
        return Optional.ofNullable(negotiations.get(key))
            .flatMap(pairNegotiations -> Optional.ofNullable(pairNegotiations.get(id)));
    }

    public Optional<ContractNegotiation> getBy(final String name1, final String name2, final long id) {
        final var key = new PairKey(name1, name2);
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
        private final String name1;
        private final String name2;

        private PairKey(final String name1, final String name2) {
            this.name1 = Objects.requireNonNull(name1, "Expected name1");
            this.name2 = Objects.requireNonNull(name2, "Expected name2");
        }

        @Override
        public int hashCode() {
            return name1.hashCode() ^ name2.hashCode();
        }

        @Override
        public boolean equals(final Object other) {
            if (this == other) { return true; }
            if (other == null || getClass() != other.getClass()) { return false; }
            final var that = (PairKey) other;
            return (name1.equals(that.name1) && name2.equals(that.name2)) ||
                (name1.equals(that.name2) && name2.equals(that.name1));
        }
    }
}
