package se.arkalix.core.coprox.model;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ContractSessions {
    private final SecureRandom random = new SecureRandom();
    private final Map<PairKey, Map<Long, ContractSession>> sessions = new ConcurrentHashMap<>();

    public ContractSession createFor(final OwnedParty ownedParty, final Party counterParty) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        final var pairSessions = sessions.computeIfAbsent(key, key0 -> new ConcurrentHashMap<>());

        // We make the assumption that random number collisions are rare and
        // that it is better for performance to recover from detected
        // collisions than to block while searching for a unique random id.

        long id;
        var attempts = 32;
        ContractSession newSession;
        ContractSession existingSession;
        do {
            do {
                id = random.nextLong();
            } while (pairSessions.containsKey(id) && attempts-- != 0);

            if (attempts < 0) {
                throw new IllegalStateException("Failed to find unique session " +
                    "id; cannot create new session for \"" +
                    ownedParty.commonName() + "\" and \"" +
                    counterParty.commonName() + "\"");
            }

            newSession = new ContractSession(ownedParty, counterParty, id);
            existingSession = pairSessions.putIfAbsent(id, newSession);
        } while (existingSession != null);

        return newSession;
    }

    public Optional<ContractSession> getBy(final OwnedParty ownedParty, final Party counterParty, final long id) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        return Optional.ofNullable(sessions.get(key))
            .flatMap(pairSessions -> Optional.ofNullable(pairSessions.get(id)));
    }

    public ContractSession getOrCreateBy(final OwnedParty ownedParty, final Party counterParty, final long id) {
        final var key = new PairKey(ownedParty.commonName(), counterParty.commonName());
        final var pairSessions = sessions.computeIfAbsent(key, key0 -> new ConcurrentHashMap<>());
        return pairSessions.computeIfAbsent(id, id0 -> new ContractSession(ownedParty, counterParty, id));
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
