package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.bank.DefinitionEntry;
import se.arkalix.core.cp.bank.DefinitionMessage;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.cp.TrustedContractAcceptanceDto;
import se.arkalix.core.plugin.cp.TrustedContractCounterOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractOfferDto;
import se.arkalix.core.plugin.cp.TrustedContractRejectionDto;
import se.arkalix.descriptor.EncodingDescriptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
                final var queryParameters = request.queryParameters();

                final var ids = Optional.ofNullable(queryParameters.get("id"))
                    .map(ids0 -> ids0.stream()
                        .flatMap(value -> Arrays.stream(value.split(","))
                            .map(String::trim))
                        .map(Long::parseUnsignedLong)
                        .collect(Collectors.toUnmodifiableList()))
                    .orElse(Collections.emptyList());

                final var hashes = Optional.ofNullable(queryParameters.get("hash"))
                    .map(hashes0 -> hashes0.stream()
                        .flatMap(value -> Arrays.stream(value.split(","))
                            .map(String::trim))
                        .map(Hash::valueOf)
                        .collect(Collectors.toUnmodifiableList()))
                    .orElse(Collections.emptyList());

                if (!ids.isEmpty()) {
                    response.status(OK)
                        .body(ids.stream()
                            .flatMap(id -> proxy.bank().get(id).stream())
                            .filter(entry -> {
                                if (hashes.isEmpty()) {
                                    return true;
                                }
                                for (final var hash : hashes) {
                                    if (entry.hashes().contains(hash)) {
                                        return true;
                                    }
                                }
                                return false;
                            })
                            .map(DefinitionEntry::toMessage)
                            .collect(Collectors.toUnmodifiableList()));
                }
                else if (!hashes.isEmpty()) {
                    response.status(OK)
                        .body(hashes.stream()
                            .map(hash -> Map.entry(hash, proxy.bank().get(hash)))
                            .filter(entry -> entry.getValue().isPresent())
                            .map(entry -> DefinitionMessage.from(entry.getValue().get(), entry.getKey()))
                            .collect(Collectors.toUnmodifiableList()));
                }
                else {
                    throw new UnsatisfiableRequestException("NO_ID_OR_HASH", "" +
                        "At least one query parameter named \"id\" or " +
                        "\"hash\" must be specified in the request, each of " +
                        "which must have a value consisting of a comma-" +
                        "separated list of negotiation identifiers or" +
                        "<hash-algorithm>:<base64-sum> pairs");
                }
                return done();
            });
    }
}
