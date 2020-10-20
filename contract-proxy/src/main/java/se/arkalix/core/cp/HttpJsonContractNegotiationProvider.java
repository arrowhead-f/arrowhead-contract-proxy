package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.contract.SignedContractAcceptanceDto;
import se.arkalix.core.cp.contract.SignedContractOfferDto;
import se.arkalix.core.cp.contract.SignedContractRejectionDto;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.descriptor.EncodingDescriptor;

import java.util.Map;

import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;

public class HttpJsonContractNegotiationProvider {
    private HttpJsonContractNegotiationProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return HttpServices.newWithUnsatisfiableRequestCatcher()
            .name("contract-negotiation")
            .basePath("/contract-negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            // This is only advertising one party. TODO: Figure out way to advertise all owned parties.
            .metadata(Map.of("party", proxy.parties().getAllOwnedParties().get(0).commonName()))

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(SignedContractAcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        proxy.update(acceptance);
                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(SignedContractOfferDto.class)
                    .ifSuccess(offer -> {
                        proxy.update(offer);
                        response.status(NO_CONTENT);
                    }))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(SignedContractRejectionDto.class)
                    .ifSuccess(rejection -> {
                        proxy.update(rejection);
                        response.status(NO_CONTENT);
                    }));
    }
}
