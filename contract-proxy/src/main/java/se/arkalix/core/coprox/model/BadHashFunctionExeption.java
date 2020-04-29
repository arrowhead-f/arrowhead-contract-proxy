package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashFunction;

public class BadHashFunctionExeption extends BadRequestException {
    private final HashFunction function;

    public BadHashFunctionExeption(final HashFunction function) {
        super("Untrusted hash function: " + function);
        this.function = function;
    }

    public HashFunction function() {
        return function;
    }
}
