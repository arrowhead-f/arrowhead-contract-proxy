package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.ContractNegotiation;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.contract.Party;
import se.arkalix.core.cp.contract.PartyBase64;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.core.cp.util.NegotiationQueryParameters;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.dto.DtoWritable;
import se.arkalix.net.http.service.HttpServiceRequestException;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.stream.Collectors;

import static se.arkalix.net.http.HttpStatus.NOT_FOUND;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

/**
 * TODO: Decide on the future of this service.
 */
@Deprecated
public class HttpJsonContractSharingProvider {
    private HttpJsonContractSharingProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return HttpServices.newWithUnsatisfiableRequestCatcher()
            .name("contract-sharing")
            .basePath("/contract-sharing")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            .get("/acceptances", (request, response) -> {
                final var query = NegotiationQueryParameters.readOrThrow(request);

                final var body = new ArrayList<DtoWritable>(1);
                proxy.getNegotiationByNamesAndId(query.name1(), query.name2(), query.id())
                    .flatMap(ContractNegotiation::acceptance)
                    .ifPresent(body::add);

                response.status(OK).body(body);

                return done();
            })

            .get("/rejections", (request, response) -> {
                final var query = NegotiationQueryParameters.readOrThrow(request);

                final var body = new ArrayList<DtoWritable>(1);
                proxy.getNegotiationByNamesAndId(query.name1(), query.name2(), query.id())
                    .flatMap(ContractNegotiation::rejection)
                    .ifPresent(body::add);

                response.status(OK).body(body);

                return done();
            })

            .get("/offers", (request, response) -> {
                final var query = NegotiationQueryParameters.readOrThrow(request);

                final var body = new ArrayList<DtoWritable>();
                proxy.getNegotiationByNamesAndId(query.name1(), query.name2(), query.id())
                    .map(ContractNegotiation::offers)
                    .ifPresent(body::addAll);

                response.status(OK).body(body);

                return done();
            })

            .get("/owned-parties", (request, response) -> {
                response.status(OK).body(proxy.parties()
                    .getAllOwnedParties()
                    .stream()
                    .map(PartyBase64::from)
                    .collect(Collectors.toUnmodifiableList()));
                return done();
            })

            .get("/parties", (request, response) -> {
                response.status(OK).body(proxy.parties()
                    .getAllParties()
                    .stream()
                    .map(PartyBase64::from)
                    .collect(Collectors.toUnmodifiableList()));
                return done();
            })

            .get("/parties/#hashOrName", (request, response) -> {
                final var rawHashOrName = request.pathParameter(0);
                final var parts = rawHashOrName.split(":", 2);

                String name;
                byte[] sum;
                Hash hash;
                Party party;
                switch (parts.length) {
                case 1:
                    try {
                        name = URLDecoder.decode(parts[0], StandardCharsets.UTF_8);
                    }
                    catch (final IllegalArgumentException ignored) {
                        throw new UnsatisfiableRequestException("BAD_NAME",
                            "Not a URL-encoded name: " + parts[0]);
                    }
                    party = proxy.parties().getAnyByCommonName(name).orElse(null);
                    break;

                case 2:
                    try {
                        sum = Base64.getUrlDecoder().decode(parts[1]);
                    }
                    catch (final IllegalArgumentException ignored) {
                        throw new UnsatisfiableRequestException("BAD_HASH",
                            "Not a URL-encoded base64 string: " + parts[1]);
                    }
                    hash = new Hash(HashAlgorithm.valueOf(parts[0]), sum);
                    party = proxy.parties().getAnyByFingerprint(hash).orElse(null);
                    break;

                default:
                    throw new UnsatisfiableRequestException("NO_HASH_OR_NAME", "" +
                        "Expected either a party name or a hash on the form " +
                        "<algorithm>:<base-64-sum>");
                }

                if (party == null) {
                    throw new HttpServiceRequestException(NOT_FOUND);
                }

                response.status(OK).body(PartyBase64.from(party));
                return done();
            });
    }
}
