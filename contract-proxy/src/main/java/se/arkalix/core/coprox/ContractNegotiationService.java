package se.arkalix.core.coprox;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.coprox.dto.AcceptanceDto;
import se.arkalix.core.coprox.dto.Error;
import se.arkalix.core.coprox.dto.OfferDto;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.core.coprox.model.Rejection;
import se.arkalix.core.coprox.security.HashFunction;
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

    public static ArService createFor(final ArSystem system, final Model model) {
        return new HttpService()
            .name("contract-negotiation")
            .basePath("/negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(token())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(AcceptanceDto.class)
                    .ifSuccess(acceptance -> {
                        final var counterPartyIdentity = request.consumer().identity();
                        final var acceptance0 = acceptance.toAcceptance();
                        if (!acceptance0.signature().verify(counterPartyIdentity, acceptance.toCanonicalJson())) {
                            response.status(BAD_REQUEST)
                                .body(Error.badSignatureSum(counterPartyIdentity.commonName(),
                                    acceptance.signature().sumAsBase64()));
                            return;
                        }

                        final var offer = acceptance.offer();
                        final var offer0 = acceptance0.offer();
                        if (!offer0.signature().verify(system.identity(), offer.toCanonicalJson())) {
                            response.status(BAD_REQUEST)
                                .body(Error.badSignatureSum(system.identity().commonName(),
                                    offer.signature().sumAsBase64()));
                            return;
                        }

                        final var counterPartyFingerprint = HashFunction.SHA256
                            .hash(counterPartyIdentity.certificate().getEncoded());

                        if (!model.update(counterPartyFingerprint, offer.sessionId(), acceptance0)) {
                            response.status(BAD_REQUEST)
                                .body(Error.badSession(offer.sessionId()));
                            return;
                        }

                        response.status(NO_CONTENT);
                    }))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(OfferDto.class)
                    .ifSuccess(offer -> {
                        final var counterPartyIdentity = request.consumer().identity();
                        final var offer0 = offer.toOffer();
                        if (!offer0.signature().verify(counterPartyIdentity, offer.toCanonicalJson())) {
                            response.status(BAD_REQUEST)
                                .body(Error.badSignatureSum(counterPartyIdentity.commonName(),
                                    offer.signature().sumAsBase64()));
                            return;
                        }

                        final var counterPartyFingerprint = HashFunction.SHA256
                            .hash(counterPartyIdentity.certificate().getEncoded());

                        if (!model.update(counterPartyFingerprint, offer.sessionId(), offer0)) {
                            response.status(BAD_REQUEST)
                                .body(Error.badSession(offer.sessionId()));
                            return;
                        }

                        response.status(NO_CONTENT);
                    }))

            .delete("/offers/#id", (request, response) -> {
                final var counterPartyIdentity = request.consumer().identity();
                final var counterPartyFingerprint = HashFunction.SHA256
                    .hash(counterPartyIdentity.certificate().getEncoded());
                final var sessionId = Long.parseLong(request.pathParameter(0));

                if (!model.update(counterPartyFingerprint, sessionId, new Rejection())) {
                    response.status(BAD_REQUEST)
                        .body(Error.badSession(sessionId));
                }

                return done();
            })

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
            });
    }
}
