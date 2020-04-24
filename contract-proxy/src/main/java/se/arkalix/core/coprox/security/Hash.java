package se.arkalix.core.coprox.security;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class Hash {
    private final HashFunction function;
    private final byte[] sum;

    public Hash(final HashFunction function, final byte[] sum) {
        this.function = Objects.requireNonNull(function, "Expected function");
        this.sum = Objects.requireNonNull(sum, "Expected sum");
    }

    public HashFunction function() {
        return function;
    }

    public byte[] sum() {
        return sum.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final Hash hash = (Hash) o;
        return function == hash.function &&
            Arrays.equals(sum, hash.sum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(function);
        result = 31 * result + Arrays.hashCode(sum);
        return result;
    }

    @Override
    public String toString() {
        return function.toString() + ":" + Base64.getEncoder().encodeToString(sum);
    }
}
