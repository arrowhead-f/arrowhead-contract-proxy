package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.bank.DefinitionMessage;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.contract.SignedContractAcceptanceDto;
import se.arkalix.core.cp.contract.SignedContractOfferDto;
import se.arkalix.core.cp.contract.SignedContractRejectionDto;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.descriptor.EncodingDescriptor;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.NO_CONTENT;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonContractNegotiationProvider {
    private HttpJsonContractNegotiationProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return HttpServices.newWithUnsatisfiableRequestCatcher()
            .name("contract-negotiation")
            .basePath("/contract-negotiation")
            .encodings(JSON)
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
                    }))

            .get("/definitions", (request, response) -> {
                final var hashParameters = request.queryParameters()
                    .get("hash");

                if (hashParameters == null || hashParameters.size() == 0) {
                    throw new UnsatisfiableRequestException("NO_HASHES", "" +
                        "At least one query parameter named \"hash\" must " +
                        "be specified in the request, each of which must " +
                        "have value consisting of a comma-separated list of " +
                        "<hash-algorithm>:<base64-checksum> pairs");
                }

                final var definitions = hashParameters.stream()
                    .flatMap(value -> Arrays.stream(value.split(","))
                        .map(String::trim))
                    .map(Hash::valueOf)
                    .map(hash -> proxy.bank().get(hash))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .map(DefinitionMessage::from)
                    .collect(Collectors.toUnmodifiableList());

                response.status(OK)
                    .body(definitions);

                return done();
            });
    }
}
