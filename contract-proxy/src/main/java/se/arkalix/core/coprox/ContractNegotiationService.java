package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.core.coprox.model.*;
import se.arkalix.core.coprox.security.HashAlgorithmUnsupportedException;
import se.arkalix.core.coprox.security.SignatureSchemeUnsupportedException;
import se.arkalix.core.plugin.ErrorBuilder;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.BAD_REQUEST;
import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class ContractNegotiationService {
    private ContractNegotiationService() {}

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

            .catcher(HashAlgorithmUnsupportedException.class, (exception, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(new ErrorBuilder()
                        .code(BAD_REQUEST.code())
                        .message(exception.getMessage())
                        .type("UNSUPPORTED_HASH_ALGORITHM")
                        .build());
                return done();
            })

            .catcher(SignatureSchemeUnsupportedException.class, (exception, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(new ErrorBuilder()
                        .code(BAD_REQUEST.code())
                        .message(exception.getMessage())
                        .type("UNSUPPORTED_SIGNATURE_SCHEME")
                        .build());
                return done();
            })

            .catcher(ModelException.class, (exception, request, response) -> {
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
