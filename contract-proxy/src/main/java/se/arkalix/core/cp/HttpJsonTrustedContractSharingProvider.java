package se.arkalix.core.cp;

import se.arkalix.ArService;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.contract.TrustedRenderedContractBuilder;
import se.arkalix.core.cp.contract.TrustedTemplate;
import se.arkalix.core.cp.util.HttpServices;
import se.arkalix.core.cp.util.NegotiationQueryParameters;
import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.ErrorResponseBuilder;
import se.arkalix.descriptor.EncodingDescriptor;

import java.util.stream.Collectors;

import static se.arkalix.net.http.HttpStatus.NOT_FOUND;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.*;
import static se.arkalix.util.concurrent.Future.done;

public final class HttpJsonTrustedContractSharingProvider {
    private HttpJsonTrustedContractSharingProvider() {}

    public static ArService createFor(final ArSystem system, final ContractProxy proxy) {
        return HttpServices.newWithUnsatisfiableRequestCatcher()
            .name("trusted-contract-sharing")
            .basePath("/trusted-contract-sharing")
            .encodings(EncodingDescriptor.JSON)
            .accessPolicy(system.isSecure() ? token() : unrestricted())

            .get("/contracts", (request, response) -> {
                final var query = NegotiationQueryParameters.readOrThrow(request);
                final var name1 = query.name1();
                final var name2 = query.name2();
                final var id = query.id();

                final var optionalNegotiation = proxy.getNegotiationByNamesAndId(name1, name2, id);
                if (optionalNegotiation.isEmpty()) {
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
                    return done();
                }
                final var negotiation = optionalNegotiation.get();
                response
                    .status(OK)
                    .body(negotiation.lastOfferContracts()
                        .stream()
                        .map(contract -> proxy.templates()
                            .getByHash(contract.templateHash())
                            .map(template -> new TrustedRenderedContractBuilder()
                                .name(template.name())
                                .text(template.render(contract))
                                .status(negotiation.status())
                                .build())
                            .orElseThrow(() -> new IllegalStateException(contract +
                                " refers to unknown template")))
                        .collect(Collectors.toUnmodifiableList()));
                return done();
            })

            .get("/templates", (request, response) -> {
                response
                    .status(OK)
                    .body(proxy.templates().getAsList()
                        .stream()
                        .map(TrustedTemplate::from)
                        .collect(Collectors.toUnmodifiableList()));
                return done();
            })

            .get("/templates/#name", (request, response) -> {
                final var optionalTemplate = proxy.templates().getByName(request.pathParameter(0));
                if (optionalTemplate.isPresent()) {
                    final var template = optionalTemplate.get();
                    response
                        .status(OK)
                        .body(TrustedTemplate.from(template));
                }
                else {
                    response.status(NOT_FOUND);
                }
                return done();
            });
    }
}
