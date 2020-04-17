package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class ContractNegotiationTrustedService {
    private ContractNegotiationTrustedService() {}

    public static ArService createFor(final ArSystem system, final Model model) {
        return new HttpService()
            .name("contract-negotiation-trusted")
            .basePath("/negotiation-trusted")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .post("session-data", (request, response) -> {
                return done();
            })

            .delete("session-data", (request, response) -> {
                return done();
            });
    }
}
