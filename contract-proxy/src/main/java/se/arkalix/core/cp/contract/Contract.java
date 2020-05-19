package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.Hash;

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
