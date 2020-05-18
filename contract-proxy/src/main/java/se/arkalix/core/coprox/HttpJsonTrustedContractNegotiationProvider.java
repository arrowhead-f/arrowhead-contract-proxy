package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.core.coprox.util.UnsatisfiableRequestException;
import se.arkalix.core.coprox.model.Model;
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
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonTrustedContractNegotiationProvider {
    private HttpJsonTrustedContractNegotiationProvider() {}

    public static ArService createFor(final Model model) {
        return new HttpService()
            .name("trusted-contract-negotiation")
            .basePath("/trusted-negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(TrustedContractAcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        model.update(acceptance);
                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(TrustedContractOfferDto.class)
                    .ifSuccess(offer -> {
                        model.update(offer);
                        response.status(NO_CONTENT);
                    }))

            .post("/counter-offers", (request, response) ->
                request
                    .bodyAs(TrustedContractCounterOfferDto.class)
                    .ifSuccess(counterOffer -> {
                        model.update(counterOffer);
                        response.status(NO_CONTENT);
                    }))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(TrustedContractRejectionDto.class)
                    .ifSuccess(rejection -> {
                        model.update(rejection);
                        response.status(NO_CONTENT);
                    }))

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
