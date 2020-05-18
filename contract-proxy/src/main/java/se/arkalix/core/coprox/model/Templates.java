package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.util.UnsatisfiableRequestException;

import java.util.*;

public class Templates {
    private final Map<String, Template> nameToTemplate;
    private final Map<Hash, Template> hashToTemplate;

    public Templates(final Collection<Template> templates) {
        Objects.requireNonNull(templates, "Expected templates");

        final var nameToTemplate = new HashMap<String, Template>();
        final var hashToTemplate = new HashMap<Hash, Template>();

        for (final var template : templates) {
            var conflictingTemplate = nameToTemplate.put(template.name(), template);
            if (conflictingTemplate != null) {
                throw new IllegalArgumentException("There are at least two " +
                    "provided templates with the same name \"" +
                    conflictingTemplate.name() + "\"; this prevents " +
                    "the construction of a non-ambiguous mapping between " +
                    "names and templates");
            }

            for (final var hash : template.acceptedHashes()) {
                conflictingTemplate = hashToTemplate.put(hash, template);
                if (conflictingTemplate != null) {
                    throw new IllegalArgumentException("There are at least " +
                        "two provided certificates that share the same " +
                        "fingerprint " + hash + "; this prevents the " +
                        "construction of a non-ambiguous mapping between " +
                        "fingerprints and counter-parties");
                }
            }
        }

        this.nameToTemplate = Collections.unmodifiableMap(nameToTemplate);
        this.hashToTemplate = Collections.unmodifiableMap(hashToTemplate);
    }

    public Optional<Template> getByName(final String name) throws UnsatisfiableRequestException {
        return Optional.ofNullable(nameToTemplate.get(name));
    }

    public Optional<Template> getByHash(final Hash hash) throws UnsatisfiableRequestException {
        return Optional.ofNullable(hashToTemplate.get(hash));
    }
}
