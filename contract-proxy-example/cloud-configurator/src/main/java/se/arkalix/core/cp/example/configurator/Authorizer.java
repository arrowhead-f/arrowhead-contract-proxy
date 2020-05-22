package se.arkalix.core.cp.example.configurator;

import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.client.HttpClientResponse;
import se.arkalix.util.concurrent.Future;
import se.arkalix.util.concurrent.Futures;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.arkalix.dto.DtoEncoding.JSON;
import static se.arkalix.net.http.HttpMethod.POST;

public class Authorizer {
    private final HttpClient client;
    private final Registry registry;

    public Authorizer(final HttpClient client, final Registry registry) {
        this.client = Objects.requireNonNull(client, "Expected client");
        this.registry = Objects.requireNonNull(registry, "Expected registry");
    }

    public Future<?> authorize(final ServiceConsumptionRule... rules) {
        return Futures.serialize(Stream.of(rules).map(this::authorize));
    }

    public Future<?> authorize(final ServiceConsumptionRule rule) {
        return client.send(Config.AU, new HttpClientRequest()
            .method(POST)
            .uri("/authorization/mgmt/intracloud")
            .body(JSON, new AuMgtIntracloudRuleBuilder()
                .consumerId(registry.getSystemIdOrThrow(rule.consumer()))
                .interfaceIds(registry.getInterfaceIdOrThrow("HTTP-SECURE-JSON"))
                .serviceDefinitionIds(rule.services()
                    .stream()
                    .map(registry::getServiceIdOrThrow)
                    .collect(Collectors.toList()))
                .providerIds(rule.providers()
                    .stream()
                    .map(registry::getSystemIdOrThrow)
                    .collect(Collectors.toList()))
                .build()))
            .flatMap(HttpClientResponse::rejectIfNotSuccess);
    }
}
