package se.arkalix.core.cp.example.configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.net.http.HttpBodyReceiver;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.net.http.client.HttpClientResponse;
import se.arkalix.util.concurrent.Future;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.arkalix.dto.DtoEncoding.JSON;
import static se.arkalix.net.http.HttpMethod.POST;

public class Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(Orchestrator.class);

    private final HttpClient client;
    private final Registry registry;

    public Orchestrator(final HttpClient client, final Registry registry) {
        this.client = Objects.requireNonNull(client, "Expected client");
        this.registry = Objects.requireNonNull(registry, "Expected registry");
    }

    public Future<?> addRules(final ServiceConsumptionRule... rules) {
        return client.send(Config.OR, new HttpClientRequest()
            .method(POST)
            .uri("/orchestrator/mgmt/store")
            .body(JSON, Stream.of(rules)
                .flatMap(rule -> rule.providers()
                    .stream()
                    .flatMap(providerName -> {
                        final var srProvider = registry.getProviderOrThrow(providerName);
                        final var orProvider = new OrMgtProviderBuilder()
                            .address(srProvider.address())
                            .authenticationInfo(srProvider.authenticationInfo())
                            .port(srProvider.port())
                            .systemName(srProvider.systemName())
                            .build();

                        return rule.services()
                            .stream()
                            .map(serviceName -> new OrMgtRuleBuilder()
                                .consumerSystemId(registry.getSystemIdOrThrow(rule.consumer()))
                                .priority(1)
                                .providerSystem(orProvider)
                                .serviceDefinitionName(serviceName)
                                .serviceInterfaceName("HTTP-SECURE-JSON")
                                .build());
                    }))
                .collect(Collectors.toList())))
            .flatMap(HttpBodyReceiver::bodyAsString)
            .ifSuccess(body -> logger.info("Created orchestration rules; response: {}", body))
            .ifFailure(Throwable.class, throwable ->
                logger.warn("Failed to create orchestration rules from " + Arrays.toString(rules), throwable));
    }
}
