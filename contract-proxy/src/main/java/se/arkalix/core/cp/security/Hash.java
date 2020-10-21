package se.arkalix.core.cp.security;

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

    public static Hash valueOf(final String string) {
        if (string == null || string.isEmpty()) {
            throw new IllegalStateException("Expected " +
                "\"<hash-algorithm>:<base64-checksum\"; got " + (string == null
                ? "null"
                : "an empty string"));
        }
        final var parts = string.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalStateException("Expected colon (:) in \"" +
                string + "\"; none found");
        }
        try {
            final var algorithm = HashAlgorithm.valueOf(parts[0]);
            final var sum = Base64.getDecoder().decode(parts[1]);
            return new Hash(algorithm, sum);
        }
        catch (final Throwable throwable) {
            throw new IllegalStateException("Expected " +
                "\"<hash-algorithm>:<base64-checksum>\"; got \"" +
                string + "\"", throwable);
        }
    }
}
