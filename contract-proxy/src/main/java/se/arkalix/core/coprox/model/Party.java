package se.arkalix.core.coprox.model;

import se.arkalix.internal.security.identity.X509Names;

import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class Party {
    private final X509Certificate certificate;
    private final Map<Long, Session> idToSession = new ConcurrentHashMap<>();
    private final String name;

    public Party(final Certificate certificate) {
        if (!(certificate instanceof X509Certificate)) {
            throw new IllegalArgumentException("Given certificate not of type x.509");
        }
        this.certificate = Objects.requireNonNull((X509Certificate) certificate, "Expected certificate");
        this.name = X509Names.commonNameOf(this.certificate.getSubjectX500Principal().getName())
            .orElseThrow(() -> new IllegalArgumentException("Certificate does not contain subject common name"));
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
            if (session0 == null && !(candidate instanceof Offer) ||
                session0 != null && session0.isClosed())
            {
                isBad.set(true);
                return session0;
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
