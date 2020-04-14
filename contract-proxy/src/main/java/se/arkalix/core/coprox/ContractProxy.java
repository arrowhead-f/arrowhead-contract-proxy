package se.arkalix.core.coprox;

import se.arkalix.ArServiceCache;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCoreIntegrator;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class ContractProxy {
    private final ArSystem system;

    public ContractProxy(final ContractProxyConfiguration configuration) throws GeneralSecurityException, IOException {
        final var builder = new ArSystem.Builder();

        if (configuration.isSecure()) {
            builder
                .identity(configuration.identity())
                .trustStore(configuration.trustStore());
        }
        else {
            builder
                .insecure()
                .name(configuration.name());
        }

        configuration.localSocketAddress()
            .ifPresent(builder::localSocketAddress);

        configuration.serviceRegistrySocketAddress()
            .ifPresent(socketAddress -> builder.plugins(HttpJsonCoreIntegrator.viaServiceRegistryAt(socketAddress)));

        configuration.serviceCacheEntryLifetime()
            .ifPresent(duration -> builder.serviceCache(ArServiceCache.withEntryLifetimeLimit(duration)));

        system = builder.build();
    }

    public void start() {
        // TODO.
    }
}
