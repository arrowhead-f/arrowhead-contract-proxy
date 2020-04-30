package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.core.coprox.dto.Error;
import se.arkalix.core.coprox.dto.TrustedAcceptanceDto;
import se.arkalix.core.coprox.dto.TrustedOfferDto;
import se.arkalix.core.coprox.dto.TrustedRejectionDto;
import se.arkalix.core.coprox.model.BadRequestException;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.BAD_REQUEST;
import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class ContractNegotiationTrustedService {
    private ContractNegotiationTrustedService() {}

    public static ArService createFor(final Model model) {
        return new HttpService()
            .name("contract-negotiation-trusted")
            .basePath("/negotiation-trusted")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(TrustedAcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        model.update(acceptance);
                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(TrustedOfferDto.class)
                    .ifSuccess(offer -> {
                        model.update(offer);
                        response.status(NO_CONTENT);
                    }))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(TrustedRejectionDto.class)
                    .ifSuccess(rejection -> {
                        model.update(rejection);
                        response.status(NO_CONTENT);
                    }))

            .catcher(BadRequestException.class, (throwable, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(Error.from(throwable));
                return done();
            });
    }
}
