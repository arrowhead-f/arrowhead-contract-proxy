package se.arkalix.core.cp.bank;

import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.util.annotation.ThreadSafe;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefinitionBank {
    private final Map<Hash, Definition> hashToDefinition = new ConcurrentHashMap<>();
    private final Map<Long, List<Definition>> negotiationIdToDefinitions = new ConcurrentHashMap<>();
    private final Set<HashAlgorithm> acceptedHashAlgorithms;

    public DefinitionBank(final Set<HashAlgorithm> acceptedHashAlgorithms) {
        this.acceptedHashAlgorithms = Objects.requireNonNull(acceptedHashAlgorithms);
    }

    @ThreadSafe
    public void add(final Definition definition) {
        Objects.requireNonNull(definition, "Expected definition");

        final var hashes = definition.hashUsing(acceptedHashAlgorithms);
        int i0 = 0, i1 = hashes.size();
        while (i0 < i1) {
            final var hash = hashes.get(i0);
            final var previous = hashToDefinition.putIfAbsent(hash, definition);
            if (previous != null) {
                while (--i0 > 0) {
                    hashToDefinition.remove(hashes.get(i0));
                }
                throw new IllegalStateException("Hash collision detected " +
                    "for " + hash + "; cannot save " + definition + " in " +
                    "definition bank");
            }
            i0++;
        }

        negotiationIdToDefinitions.compute(definition.negotiationId(), (id, definitions) -> {
            if (definitions == null) {
                definitions = new CopyOnWriteArrayList<>();
            }
            definitions.add(definition);
            return definitions;
        });
    }

    @ThreadSafe
    public boolean contains(final Hash hash) {
        Objects.requireNonNull(hash, "Expected hash");

        return hashToDefinition.containsKey(hash);
    }

    @ThreadSafe
    public Optional<Definition> get(final Hash hash) {
        Objects.requireNonNull(hash, "Expected hash");

        return Optional.ofNullable(hashToDefinition.get(hash));
    }

    @ThreadSafe
    public Collection<Definition> get(final long negotiationId) {
        final var definitions = negotiationIdToDefinitions.get(negotiationId);
        if (definitions == null) {
            return Collections.emptyList();
        }
        return List.copyOf(definitions);
    }
}
