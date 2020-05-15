package se.arkalix.core.coprox.security;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/**
 * A one-way algorithm useful for calculating unique and reproducible
 * fingerprints, or checksums, from arbitrary-length byte arrays.
 */
public final class HashAlgorithm {
    private final String ianaId;
    private final String javaId;
    private final boolean isCollisionSafe;
    private MessageDigest digest;

    private HashAlgorithm(
        final String ianaId,
        final String javaId,
        final boolean isCollisionSafe,
        final MessageDigest digest)
    {
        this.ianaId = ianaId;
        this.javaId = javaId;
        this.isCollisionSafe = isCollisionSafe;
        this.digest = digest;
    }

    /**
     * The MD2 message-digest algorithm. <i>Obsolete.</i>
     * <p>
     * This algorithm is <a href="https://tools.ietf.org/html/rfc6149">obsolete
     * </a> and is only included for the sake of completeness. <i>Do not use it
     * in production systems.</i>
     *
     * @see <a href="https://tools.ietf.org/html/rfc1319">RFC 1319</a>
     * @see <a href="https://tools.ietf.org/html/rfc6149">RFC 6149</a>
     */
    public static final HashAlgorithm MD2 = new HashAlgorithm("md2", "MD2", false, null);

    /**
     * The MD5 message-digest algorithm. <i>Not recommended.</i>
     * <p>
     * This algorithm is <a href="https://tools.ietf.org/html/rfc6151">not
     * recommended for most kinds of use cases</a> and is included mostly for
     * the sake of completeness. <i>Do not use it in production systems unless
     * it is known that its guarantees are sufficient.</i>
     *
     * @see <a href="https://tools.ietf.org/html/rfc1321">RFC 1321</a>
     * @see <a href="https://tools.ietf.org/html/rfc6151">RFC 6151</a>
     */
    public static final HashAlgorithm MD5 = new HashAlgorithm("md5", "MD5", false, null);

    /**
     * The US Secure Hash Algorithm 1 (SHA-1). <i>Not recommended.</i>
     * <p>
     * This algorithm is <i>not recommended for most kinds of use cases</i> and
     * is included mostly for the sake of completeness. <i>Do not use it in
     * production systems unless it is known that its guarantees are
     * sufficient.</i>
     *
     * @see <a href="https://tools.ietf.org/html/rfc6234">RFC 6234</a>
     * @see <a href="https://shattered.io/static/shattered.pdf"> Marc Stevens et al. "The first collision for full SHA-1". Google Research, 2017</a>
     */
    public static final HashAlgorithm SHA_1 = new HashAlgorithm("sha-1", "SHA-1", false, null);

    /**
     * 224-bit SHA-2.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6234">RFC 6234</a>
     */
    public static final HashAlgorithm SHA_224 = new HashAlgorithm("sha-224", "SHA-224", true, null);

    /**
     * 256-bit SHA-2.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6234">RFC 6234</a>
     */
    public static final HashAlgorithm SHA_256 = new HashAlgorithm("sha-256", "SHA-256", true, null);

    /**
     * 384-bit SHA-2.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6234">RFC 6234</a>
     */
    public static final HashAlgorithm SHA_384 = new HashAlgorithm("sha-384", "SHA-384", true, null);

    /**
     * 512-bit SHA-2.
     *
     * @see <a href="https://tools.ietf.org/html/rfc6234">RFC 6234</a>
     */
    public static final HashAlgorithm SHA_512 = new HashAlgorithm("sha-512", "SHA-512", true, null);

    /**
     * All supported hash algorithms, irrespective of whether or not they are
     * known to have been subjects of successful collision attempts or are
     * susceptible to other kinds of attacks.
     */
    public static final List<HashAlgorithm> ALL = List.of(
        MD2,
        MD5,
        SHA_1,
        SHA_224,
        SHA_256,
        SHA_384,
        SHA_512);

    /**
     * Computes {@link Hash} from given {@code data}.
     *
     * @param data Data to hash.
     * @return New hash object.
     */
    public Hash hash(final byte[] data) {
        synchronized (this) {
            if (digest == null) {
                try {
                    digest = MessageDigest.getInstance(javaId);
                }
                catch (final NoSuchAlgorithmException exception) {
                    throw new HashAlgorithmUnsupportedException(ianaId, exception);
                }
            }
        }
        return new Hash(this, digest.digest(data));
    }

    /**
     * Reports whether or not this algorithm, as of 2020-05-15, has been the
     * target of a publicly known and successful collision attack. Such
     * algorithms should <i>not</i> be used in most kinds of production
     * scenarios.
     *
     * @return {@code true} only if this algorithm is safe against collision
     * attacks, as described above.
     */
    public boolean isCollisionSafe() {
        return isCollisionSafe;
    }

    /**
     * Looks up supported {@link HashAlgorithm} by IANA hash algorithm textual
     * name.
     *
     * @param name Name to resolve.
     * @return Existing {@link HashAlgorithm} instance.
     * @throws HashAlgorithmUnsupportedException If given name does not match
     *                                           any supported hash algorithm.
     * @see <a href="https://www.iana.org/assignments/hash-function-text-names/hash-function-text-names.xhtml">IANA Hash Algorithm Textual Names</a>
     */
    public static HashAlgorithm valueOf(String name) {
        name = Objects.requireNonNull(name, "Expected name").toLowerCase();
        switch (name) {
        case "md2": return MD2;
        case "md5": return MD5;
        case "sha-1": return SHA_1;
        case "sha-224": return SHA_224;
        case "sha-256": return SHA_256;
        case "sha-384": return SHA_384;
        case "sha-512": return SHA_512;
        default:
            throw new HashAlgorithmUnsupportedException(name);
        }
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) { return true; }
        if (other == null || getClass() != other.getClass()) { return false; }
        final HashAlgorithm that = (HashAlgorithm) other;
        return ianaId.equals(that.ianaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ianaId);
    }

    @Override
    public String toString() {
        return ianaId;
    }
}
