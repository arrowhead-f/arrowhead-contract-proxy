package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonTrustedContractObservationProvider {
    private HttpJsonTrustedContractObservationProvider() {}

    public static ArService createFor(final ArSystem system, final Model model) {
        return new HttpService()
            .name("trusted-contract-observation")
            .basePath("/trusted-observation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .get("/negotiations", (request, response) -> {
                return done();
            });
    }
}
