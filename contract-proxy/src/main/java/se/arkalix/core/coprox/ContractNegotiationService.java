package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.core.coprox.dto.Error;
import se.arkalix.core.coprox.dto.SignedAcceptanceDto;
import se.arkalix.core.coprox.dto.SignedOfferDto;
import se.arkalix.core.coprox.dto.SignedRejectionDto;
import se.arkalix.core.coprox.model.BadRequestException;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.core.coprox.security.HashFunctionUnsupportedException;
import se.arkalix.core.coprox.security.SignatureSchemeUnsupportedException;
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
                    .bodyAs(SignedAcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        model.update(acceptance);
                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(SignedOfferDto.class)
                    .ifSuccess(offer -> {
                        model.update(offer);
                        response.status(NO_CONTENT);
                    }))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(SignedRejectionDto.class)
                    .ifSuccess(rejection -> {
                        model.update(rejection);
                        response.status(NO_CONTENT);
                    }))

            .catcher(HashFunctionUnsupportedException.class, (throwable, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(Error.badHashFunction(throwable.getMessage()));
                return done();
            })

            .catcher(SignatureSchemeUnsupportedException.class, (throwable, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(Error.badSignatureScheme(throwable.getMessage()));
                return done();
            })

            .catcher(BadRequestException.class, (throwable, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(Error.from(throwable));
                return done();
            });
    }
}
