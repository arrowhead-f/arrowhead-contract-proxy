package se.arkalix.core.cp.example.configurator;

import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.util.concurrent.Future;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static se.arkalix.dto.DtoEncoding.JSON;
import static se.arkalix.net.http.HttpMethod.GET;

public class Registry {
    private final Map<String, Integer> interfaceNameToId = new ConcurrentHashMap<>();
    private final Map<String, Integer> serviceNameToId = new ConcurrentHashMap<>();
    private final Map<String, Integer> systemNameToId = new ConcurrentHashMap<>();
    private final Map<String, SrMgtProvider> systemNameToProvider = new ConcurrentHashMap<>();

    private final HttpClient client;

    public Registry(final HttpClient client) {
        this.client = Objects.requireNonNull(client, "Expected client");
    }

    public int getInterfaceIdOrThrow(final String name) {
        final var id = interfaceNameToId.get(name);
        if (id == null) {
            throw new IllegalStateException("No interface named \"" + name + "\" exists in registry");
        }
        return id;
    }

    public int getServiceIdOrThrow(final String name) {
        final var id = serviceNameToId.get(name);
        if (id == null) {
            throw new IllegalStateException("No service named \"" + name + "\" exists in registry");
        }
        return id;
    }

    public int getSystemIdOrThrow(final String name) {
        final var id = systemNameToId.get(name);
        if (id == null) {
            throw new IllegalStateException("No system named \"" + name + "\" exists in registry");
        }
        return id;
    }

    public SrMgtProvider getProviderOrThrow(final String name) {
        final var provider = systemNameToProvider.get(name);
        if (provider == null) {
            throw new IllegalStateException("No system named \"" + name + "\" exists in registry");
        }
        return provider;
    }

    public Future<?> refresh() {
        return client.send(Config.SR, new HttpClientRequest().method(GET).uri("/serviceregistry/mgmt"))
            .flatMap(response -> response.bodyAsIfSuccess(JSON, SrMgtQueryResultDto.class))
            .ifSuccess(result -> result.data().forEach(entry -> {
                final var sd = entry.serviceDefinition();
                serviceNameToId.put(sd.serviceDefinition(), sd.id());

                entry.interfaces().forEach(i -> interfaceNameToId.put(i.interfaceName(), i.id()));

                final var p = entry.provider();
                systemNameToId.put(p.systemName(), p.id());
                systemNameToProvider.put(p.systemName(), p);
            }));
    }
}
