package se.arkalix.core.coprox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArServiceCache;
import se.arkalix.ArSystem;
import se.arkalix.core.coprox.model.Model;
import se.arkalix.core.coprox.util.Properties;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        final String args0;
        switch (args.length) {
        case 0:
            args0 = "./application.properties";
            break;
        case 1:
            args0 = args[0];
            break;
        default:
            logger.error("Expected either 0 or 1 arguments, but the " +
                    "following were provided: {}; Usage: java [flags...] " +
                    "-jar contract-proxy.jar [path to .properties file]",
                Arrays.toString(args));
            System.exit(1);
            return;
        }
        try {
            final var pathToProperties = Path.of(args0).toAbsolutePath().normalize();

            logger.info("Reading contract proxy properties from {}", args0);
            final var properties = Properties.read(pathToProperties);

            logger.info("Setting up contract proxy system");
            final var system = createContractProxySystem(properties);

            logger.info("Loading contract proxy data model");
            final var model = new Model.Builder()
                //.trustedHashAlgorithms(Set.of(HashAlgorithm.SHA_1, HashAlgorithm.SHA_256))
                .build();

            system.provide(HttpJsonContractNegotiationProvider.createFor(model));
            system.provide(HttpJsonTrustedContractNegotiationProvider.createFor(model));
            system.provide(HttpJsonTrustedContractObservationProvider.createFor(system, model));

            logger.info("Contract proxy system served via: {}", system.localAddress());
        }
        catch (final Throwable throwable) {
            logger.error("Failed to start contract proxy", throwable);
        }
    }

    private static ArSystem createContractProxySystem(final Properties properties)
        throws GeneralSecurityException, IOException
    {
        final var builder = new ArSystem.Builder();

        final var isSecure = properties.getBoolean("kalix.is-secure")
            .orElseGet(() -> properties.isDefined("kalix.identity.keystore-path"));

        if (isSecure) {
            final var keyStorePath = properties.getPathOrThrow("kalix.identity.keystore-path");
            final var keyStorePassword = properties.getString("kalix.identity.keystore-password");
            final var keyAlias = properties.getString("kalix.identity.key-alias");
            final var keyPassword = properties.getString("kalix.identity.key-password");

            final var identity = new OwnedIdentity.Loader()
                .keyStorePath(keyStorePath)
                .keyStorePassword(keyStorePassword.map(String::toCharArray).orElse(null))
                .keyAlias(keyAlias.orElse(null))
                .keyPassword(keyPassword.map(String::toCharArray).orElse(null))
                .load();

            final var trustStorePath = properties.getPathOrThrow("kalix.truststore.path");
            final var trustStorePassword = properties.getString("kalix.truststore.password");

            final var trustStore = TrustStore.read(
                trustStorePath,
                trustStorePassword.map(String::toCharArray).orElse(null));

            builder
                .identity(identity)
                .trustStore(trustStore);
        }
        else {
            builder.name(properties.getStringOrThrow("kalix.name"));
        }

        var serviceRegistrySocketAddress = properties
            .getInetSocketAddressOrThrow("kalix.integrator.service-registry.host");
        if (serviceRegistrySocketAddress.getPort() == 0) {
            serviceRegistrySocketAddress = InetSocketAddress.createUnresolved(
                serviceRegistrySocketAddress.getHostString(),
                properties.getInteger("kalix.integrator.service-registry.port").orElse(0));
        }

        return builder
            .localSocketAddress(properties.getInetSocketAddress("kalix.host")
                .map(host -> {
                    if (host.getPort() == 0) {
                        return InetSocketAddress.createUnresolved(
                            host.getHostString(),
                            properties.getInteger("kalix.port").orElse(0));
                    }
                    return host;
                })
                .orElse(null))
            .serviceCache(properties.getDuration("kalix.service-cache.entry-lifetime")
                .map(ArServiceCache::withEntryLifetimeLimit)
                .orElseGet(ArServiceCache::withDefaultEntryLifetimeLimit))
            .plugins(HttpJsonCloudPlugin.viaServiceRegistryAt(serviceRegistrySocketAddress))
            .build();
    }
}
