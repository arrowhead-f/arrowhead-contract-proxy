package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashAlgorithm;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class Party {
    private static final Duration CLOCK_SKEW_TOLERANCE = Duration.ofHours(9);

    private final X509Certificate certificate;
    private final String commonName;
    private final List<Hash> acceptedFingerprints;
    private final Hash preferredFingerprint;

    public Party(final Certificate certificate, final Set<HashAlgorithm> supportedHashAlgorithms) {
        Objects.requireNonNull(certificate, "Expected certificate");
        Objects.requireNonNull(supportedHashAlgorithms, "Expected supportedHashAlgorithms");

        if (!(certificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("Given certificate not of type x.509");
        }
        if (supportedHashAlgorithms.isEmpty()) {
            throw new IllegalArgumentException("At least one hash algorithm must be provided");
        }

        this.certificate = (X509Certificate) certificate;

        try {
            commonName = (String) new LdapName(this.certificate.getSubjectX500Principal().getName())
                .getRdns()
                .stream()
                .filter(rdn -> rdn.getType().equalsIgnoreCase("CN"))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Certificate does not contain subject common name"))
                .getValue();
        }
        catch (final InvalidNameException exception) {
            throw new RuntimeException(exception);
        }

        final var now = Instant.now();

        final var notBefore = this.certificate.getNotBefore().toInstant();
        if (now.plus(CLOCK_SKEW_TOLERANCE).isBefore(notBefore)) {
            throw new IllegalArgumentException("Certificate [CN=" +
                commonName + "] does not become valid until " + notBefore +
                "; cannot use certificate");
        }

        final var notAfter = this.certificate.getNotAfter().toInstant();
        if (now.minus(CLOCK_SKEW_TOLERANCE).isAfter(notAfter)) {
            throw new IllegalArgumentException("Certificate [CN=" +
                commonName + " ceased to be valid at " + notAfter +
                "; cannot use certificate");
        }

        try {
            final var certificateAsBytes = certificate.getEncoded();
            acceptedFingerprints = supportedHashAlgorithms.stream()
                .map(hashAlgorithm -> hashAlgorithm.hash(certificateAsBytes))
                .collect(Collectors.toUnmodifiableList());
            preferredFingerprint = acceptedFingerprints.stream()
                .filter(fingerprint -> fingerprint.algorithm().isCollisionSafe())
                .findFirst()
                .orElseGet(() -> acceptedFingerprints.get(0));
        }
        catch (final CertificateEncodingException exception) {
            throw new RuntimeException("Could not get canonical encoded " +
                "form of given certificate [commonName=" + commonName + "]");
        }
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public String commonName() {
        return commonName;
    }

    public List<Hash> acceptedFingerprints() {
        return acceptedFingerprints;
    }

    public Hash preferredFingerprint() {
        return preferredFingerprint;
    }
}
