package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.plugin.ErrorBuilder;
import se.arkalix.core.plugin.cp.TrustedContractAcceptanceDto;
import se.arkalix.core.plugin.cp.TrustedContractCounterOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractRejectionDto;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.BAD_REQUEST;
import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonTrustedContractNegotiationProvider {
    private HttpJsonTrustedContractNegotiationProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return new HttpService()
            .name("trusted-contract-negotiation")
            .basePath("/trusted-contract-negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(TrustedContractAcceptanceDto.class)
                    .ifSuccess(acceptance -> proxy.update(acceptance)
                        .ifSuccess(ignored -> response.status(NO_CONTENT))))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(TrustedContractOfferDto.class)
                    .flatMap(offer -> proxy.update(offer)
                        .ifSuccess(ignored -> response.status(NO_CONTENT))))

            .post("/counter-offers", (request, response) ->
                request
                    .bodyAs(TrustedContractCounterOfferDto.class)
                    .ifSuccess(counterOffer -> proxy.update(counterOffer)
                        .ifSuccess(ignored -> response.status(NO_CONTENT))))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(TrustedContractRejectionDto.class)
                    .ifSuccess(rejection -> proxy.update(rejection)
                        .ifSuccess(ignored -> response.status(NO_CONTENT))))

            .catcher(UnsatisfiableRequestException.class, (exception, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(new ErrorBuilder()
                        .code(BAD_REQUEST.code())
                        .message(exception.getMessage())
                        .type(exception.type())
                        .build());
                return done();
            });
    }
}
