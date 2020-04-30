package se.arkalix.core.coprox.model;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Party {
    private final X509Certificate certificate;
    private final Map<Long, Session> idToSession = new ConcurrentHashMap<>();
    private final String name;

    public Party(final Certificate certificate) {
        Objects.requireNonNull(certificate, "Expected certificate");
        if (!(certificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("Given certificate not of type x.509");
        }
        this.certificate = (X509Certificate) certificate;
        try {
            this.name = (String) new LdapName(this.certificate.getSubjectX500Principal().getName())
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
    }

    public String name() {
        return name;
    }

    public X509Certificate certificate() {
        return certificate;
    }

    public Session updateSession(final long sessionId, final Candidate candidate) throws BadSessionException {
        final var isBad = new AtomicBoolean(false);
        final var previousSession = new AtomicReference<Session>(null);

        idToSession.compute(sessionId, (sessionId0, session0) -> {
            if (candidate instanceof Offer) {
                if (session0 != null && session0.isClosedAt(candidate.createdAt())) {
                    isBad.set(true);
                    return session0;
                }
            }
            else if (candidate instanceof Acceptance) {
                if (session0 != null && !session0.isAcceptableAt(candidate.createdAt())) {
                    isBad.set(true);
                    return session0;
                }
            }
            else if (candidate instanceof Rejection) {
                if (session0 != null && session0.isClosedAt(candidate.createdAt())) {
                    return session0;
                }
            }
            else {
                throw new IllegalStateException("Unexpected candidate type: " + candidate.getClass());
            }
            previousSession.set(session0);
            return new Session(sessionId, candidate);
        });

        if (isBad.get()) {
            throw new BadSessionException(sessionId);
        }

        return previousSession.get();
    }
}
