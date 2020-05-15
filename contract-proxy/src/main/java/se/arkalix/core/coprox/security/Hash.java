package se.arkalix.core.coprox.security;

import java.util.Arrays;
import java.util.Base64;
import java.util.Objects;

public class Hash {
    private final HashAlgorithm algorithm;
    private final byte[] sum;

    public Hash(final HashAlgorithm algorithm, final byte[] sum) {
        this.algorithm = Objects.requireNonNull(algorithm, "Expected function");
        this.sum = Objects.requireNonNull(sum, "Expected sum");
    }

    public HashAlgorithm algorithm() {
        return algorithm;
    }

    public byte[] sum() {
        return sum.clone();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final Hash hash = (Hash) o;
        return algorithm == hash.algorithm &&
            Arrays.equals(sum, hash.sum);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(algorithm);
        result = 31 * result + Arrays.hashCode(sum);
        return result;
    }

    @Override
    public String toString() {
        return algorithm.toString() + ":" + Base64.getEncoder().encodeToString(sum);
    }
}
