package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class Contract {
    private final Hash templateHash;
    private final Map<String, String> arguments;

    public Contract(final Hash templateHash, final Map<String, String> arguments) {
        this.templateHash = Objects.requireNonNull(templateHash, "Expected templateHash");
        this.arguments = Collections.unmodifiableMap(Objects.requireNonNull(arguments, "Expected arguments"));
    }

    public Hash templateHash() {
        return templateHash;
    }

    public Optional<String> argument(final String name) {
        return Optional.ofNullable(arguments.get(name));
    }

    public Map<String, String> arguments() {
        return arguments;
    }
}
