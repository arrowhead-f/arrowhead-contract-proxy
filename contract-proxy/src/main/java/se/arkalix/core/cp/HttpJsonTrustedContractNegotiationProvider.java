package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.bank.DefinitionMessage;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.cp.TrustedContractAcceptanceDto;
import se.arkalix.core.plugin.cp.TrustedContractCounterOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractRejectionDto;
import se.arkalix.descriptor.EncodingDescriptor;

import java.util.Arrays;
import java.util.stream.Collectors;

import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonTrustedContractNegotiationProvider {
    private HttpJsonTrustedContractNegotiationProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return HttpServices.newWithUnsatisfiableRequestCatcher()
            .name("trusted-contract-negotiation")
            .basePath("/trusted-contract-negotiation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            .post("/acceptances", (request, response) ->
                request
                    .bodyAs(TrustedContractAcceptanceDto.class)
                    .flatMap(proxy::update)
                    .ifSuccess(ignored -> response.status(NO_CONTENT)))

            .post("/offers", (request, response) ->
                request
                    .bodyAs(TrustedContractOfferDto.class)
                    .flatMap(proxy::update)
                    .ifSuccess(negotiationId -> response
                        .status(NO_CONTENT)
                        .header("location", "/trusted-contract-negotiation/offers/" + negotiationId)))

            .post("/counter-offers", (request, response) ->
                request
                    .bodyAs(TrustedContractCounterOfferDto.class)
                    .flatMap(proxy::update)
                    .ifSuccess(ignored -> response.status(NO_CONTENT)))

            .post("/rejections", (request, response) ->
                request
                    .bodyAs(TrustedContractRejectionDto.class)
                    .flatMap(proxy::update)
                    .ifSuccess(ignored -> response.status(NO_CONTENT)))

            .get("/definitions", (request, response) -> {
                final var idParameters = request.queryParameters()
                    .get("id");

                if (idParameters == null || idParameters.size() == 0) {
                    throw new UnsatisfiableRequestException("NO_IDS", "" +
                        "At least one query parameter named \"id\" must " +
                        "be specified in the request, each of which must " +
                        "have a value consisting of a comma-separated list " +
                        "of negotiation identifiers");
                }

                final var definitions = idParameters.stream()
                    .flatMap(value -> Arrays.stream(value.split(","))
                        .map(String::trim))
                    .map(Long::parseUnsignedLong)
                    .flatMap(id -> proxy.bank().get(id).stream())
                    .map(DefinitionMessage::from)
                    .collect(Collectors.toUnmodifiableList());

                response.status(OK)
                    .body(definitions);

                return done();
            });
    }
}
