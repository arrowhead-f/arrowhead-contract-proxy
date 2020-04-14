package se.arkalix.core.coprox;

import se.arkalix.core.coprox.util.Properties;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public class ContractProxyConfiguration {
    private final Properties properties;

    private ContractProxyConfiguration(final Properties properties) {
        this.properties = Objects.requireNonNull(properties, "Expected properties");
    }

    public static ContractProxyConfiguration read(final Path path) throws IOException {
        return new ContractProxyConfiguration(Properties.read(path));
    }

    public OwnedIdentity identity() throws GeneralSecurityException, IOException {
        final var keyStorePath = properties.getPathOrThrow("kalix.identity.keystore-path");
        final var keyStorePassword = properties.getString("kalix.identity.keystore-password");
        final var keyAlias = properties.getString("kalix.identity.key-alias");
        final var keyPassword = properties.getString("kalix.identity.key-password");

        return new OwnedIdentity.Loader()
            .keyStorePath(keyStorePath)
            .keyStorePassword(keyStorePassword.map(String::toCharArray).orElse(null))
            .keyAlias(keyAlias.orElse(null))
            .keyPassword(keyPassword.map(String::toCharArray).orElse(null))
            .load();
    }

    public boolean isSecure() {
        final var isSecure = properties.getBoolean("kalix.is-secure");
        if (isSecure.isEmpty()) {
            return properties.isDefined("kalix.identity.keystore-path");
        }
        return isSecure.get();
    }

    public Optional<InetSocketAddress> localSocketAddress() {
        return properties.getInetSocketAddress("kalix.host")
            .map(host -> {
                if (host.getPort() == 0) {
                    return InetSocketAddress.createUnresolved(
                        host.getHostString(),
                        properties.getInteger("kalix.port").orElse(0));
                }
                return host;
            });
    }

    public String name() {
        return properties.getStringOrThrow("kalix.name");
    }

    public Optional<Duration> serviceCacheEntryLifetime() {
        return properties.getDuration("kalix.service-cache.entry-lifetime");
    }

    public Optional<InetSocketAddress> serviceRegistrySocketAddress() {
        return properties.getInetSocketAddress("kalix.integrator.service-registry.host")
            .map(host -> {
                if (host.getPort() == 0) {
                    return InetSocketAddress.createUnresolved(
                        host.getHostString(),
                        properties.getInteger("kalix.integrator.service-registry.port").orElse(0));
                }
                return host;
            });
    }

    public TrustStore trustStore() throws GeneralSecurityException, IOException {
        final var path = properties.getPathOrThrow("kalix.truststore.path");
        final var password = properties.getString("kalix.truststore.password");

        return TrustStore.read(path, password.map(String::toCharArray).orElse(null));
    }
}
