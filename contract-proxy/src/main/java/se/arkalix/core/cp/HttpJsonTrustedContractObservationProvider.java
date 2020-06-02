package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.ErrorResponseBuilder;
import se.arkalix.core.plugin.cp.TrustedContractNegotiationBuilder;
import se.arkalix.descriptor.EncodingDescriptor;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.NOT_FOUND;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.security.access.AccessPolicy.unrestricted;
import static se.arkalix.util.concurrent.Future.done;

public class HttpJsonTrustedContractObservationProvider {
    private HttpJsonTrustedContractObservationProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return new HttpService()
            .name("trusted-contract-observation")
            .basePath("/trusted-contract-observation")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            .get("/negotiations", (request, response) -> {
                final var name1 = request.queryParameter("name1")
                    .orElseThrow(() -> new UnsatisfiableRequestException(
                        "NO_NAME1", "Expected query parameter \"name1\""));

                final var name2 = request.queryParameter("name2")
                    .orElseThrow(() -> new UnsatisfiableRequestException(
                        "NO_NAME2", "Expected query parameter \"name2\""));

                final long id;
                try {
                    id = request.queryParameter("id")
                        .map(Long::parseLong)
                        .orElseThrow(() -> new UnsatisfiableRequestException(
                            "NO_ID", "Expected query parameter \"id\""));
                }
                catch (final NumberFormatException exception) {
                    throw new UnsatisfiableRequestException(
                        "BAD_ID", "Expected query parameter \"id\" to be unsigned 63-bit integer", exception);
                }
                if (id < 0) {
                    throw new UnsatisfiableRequestException(
                        "BAD_ID", "Expected query parameter \"id\" to be a unsigned 63-bit integer");
                }

                final var optionalNegotiation = proxy.getNegotiationByNamesAndId(name1, name2, id);
                if (optionalNegotiation.isPresent()) {
                    final var negotiation = optionalNegotiation.get();
                    response
                        .status(OK)
                        .body(new TrustedContractNegotiationBuilder()
                            .id(negotiation.id())
                            .offer(negotiation.lastOfferAsTrusted())
                            .status(negotiation.status())
                            .build());
                }
                else {
                    response
                        .status(NOT_FOUND)
                        .body(new ErrorResponseBuilder()
                            .code(NOT_FOUND.code())
                            .message("No negotiation with id " + id + " is " +
                                "known to be or have been taking place " +
                                "between parties \"" + name1 + "\" and \"" +
                                name2 + "\"")
                            .type("NOT_FOUND")
                            .build());
                }

                return done();
            });
    }
}
