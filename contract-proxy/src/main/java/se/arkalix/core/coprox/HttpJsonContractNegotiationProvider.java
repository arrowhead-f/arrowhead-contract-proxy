package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.core.coprox.model.SignedContractAcceptanceDto;
import se.arkalix.core.coprox.model.SignedContractOfferDto;
import se.arkalix.core.coprox.model.SignedContractRejectionDto;
import se.arkalix.core.coprox.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.ErrorBuilder;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.BAD_REQUEST;
import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonContractNegotiationProvider {
    private HttpJsonContractNegotiationProvider() {}

    public static ArService createFor(final Model model) {
        return new HttpService()
            .name("contract-negotiation")
            .basePath("/negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(SignedContractAcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        model.update(acceptance);
                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(SignedContractOfferDto.class)
                    .ifSuccess(offer -> {
                        model.update(offer);
                        response.status(NO_CONTENT);
                    }))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(SignedContractRejectionDto.class)
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
