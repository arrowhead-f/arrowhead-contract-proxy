package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class ContractNegotiationTrustedSessionService {
    private ContractNegotiationTrustedSessionService() {}

    public static ArService createFor(final ArSystem system, final Model model) {
        return new HttpService()
            .name("contract-negotiation-trusted-session")
            .basePath("/negotiation-trusted-session")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .get("session-data", (request, response) -> {
                return done();
            });
    }
}
