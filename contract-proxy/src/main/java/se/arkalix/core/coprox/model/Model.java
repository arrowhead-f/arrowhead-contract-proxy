package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class Model {
    private final Map<Hash, Map<Long, Session>> partySessions = new ConcurrentHashMap<>();

    public boolean update(final Hash counterPartyFingerprint, final long id, final Candidate candidate) {
        final var sessions = partySessions.computeIfAbsent(counterPartyFingerprint, hash0 -> new ConcurrentHashMap<>());
        final var session = sessions.compute(id, (id0, session0) -> {
            if (session0 != null && session0.isClosed()) {
                return session0;
            }
            return new Session(id, candidate);
        });
        return session.isClosed();
    }

    public Optional<Session> get(final Hash counterPartyFingerprint, final long id) {
        final var sessions = partySessions.get(counterPartyFingerprint);
        if (sessions == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(sessions.get(id));
    }
}
