package se.arkalix.core.coprox.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public final class HashFunction {
    private final String id;
    private final String name;
    private MessageDigest digest;

    private HashFunction(final String id, final String name, final MessageDigest digest) {
        this.id = id;
        this.name = name;
        this.digest = digest;
    }

    public static final HashFunction MD5 = new HashFunction("md5", "MD5", null);
    public static final HashFunction SHA1 = new HashFunction("sha1", "SHA-1", null);
    public static final HashFunction SHA256 = new HashFunction("sha256", "SHA-256", null);

    public Hash hash(final byte[] data) {
        synchronized (this) {
            if (this.digest == null) {
                try {
                    this.digest = MessageDigest.getInstance(this.name);
                }
                catch (final NoSuchAlgorithmException exception) {
                    throw new HashFunctionUnsupportedException(name, exception);
                }
            }
        }
        return new Hash(this, this.digest.digest(data));
    }

    public static HashFunction valueOf(final String string) {
        switch (Objects.requireNonNull(string, "Expected string").toLowerCase()) {
        case "md5": return MD5;
        case "sha1": return SHA1;
        case "sha256": return SHA256;
        }
        throw new IllegalArgumentException("Unsupported hash function: " + string);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final HashFunction that = (HashFunction) other;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return id;
    }
}
