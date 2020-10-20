package se.arkalix.core.cp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArServiceDescriptionCache;
import se.arkalix.ArServiceHandle;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.ContractProxy;
import se.arkalix.core.cp.contract.OwnedParty;
import se.arkalix.core.cp.contract.Party;
import se.arkalix.core.cp.contract.Template;
import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.core.cp.util.Properties;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.function.ThrowingConsumer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        final String args0;
        switch (args.length) {
        case 0:
            args0 = "application.properties";
            break;
        case 1:
            args0 = args[0];
            break;
        default:
            logger.error("Expected either 0 or 1 arguments, but the " +
                    "following were provided: {}; Usage: java [jvm-flags...] " +
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
            final var system = createSystem(properties);

            logger.info("Loading contract proxy data model");
            final var proxy = createContractProxy(system, properties);

            system.provide(HttpJsonContractNegotiationProvider.createFor(system, proxy))
                .ifSuccess(logIsProvidingService())
                .onFailure(Main::panic);

            system.provide(HttpJsonContractSharingProvider.createFor(system, proxy))
                .ifSuccess(logIsProvidingService())
                .onFailure(Main::panic);

            system.provide(HttpJsonTrustedContractNegotiationProvider.createFor(system, proxy))
                .ifSuccess(logIsProvidingService())
                .onFailure(Main::panic);

            system.provide(HttpJsonTrustedContractObservationProvider.createFor(system, proxy))
                .ifSuccess(logIsProvidingService())
                .onFailure(Main::panic);

            system.provide(HttpJsonTrustedContractSharingProvider.createFor(system, proxy))
                .ifSuccess(logIsProvidingService())
                .onFailure(Main::panic);

            logger.info("About to provide services via: {}", system.socketAddress());
        }
        catch (final Throwable throwable) {
            panic(throwable);
        }
    }

    private static ThrowingConsumer<ArServiceHandle> logIsProvidingService() {
        return handle -> logger.info("Providing service \"" + handle.description().name() + "\" ...");
    }

    private static void panic(final Throwable cause) {
        logger.error("Failed to start contract proxy", cause);
        System.exit(1);
    }

    private static ContractProxy createContractProxy(final ArSystem system, final Properties properties)
        throws GeneralSecurityException, IOException
    {
        final var acceptedHashAlgorithms = properties.getString("kalix.core.cp.accepted-hash-algorithms")
            .map(algorithms -> Stream.of(algorithms.split(","))
                .map(algorithm -> HashAlgorithm.valueOf(algorithm.trim())))
            .orElseGet(() -> HashAlgorithm.ALL.stream()
                .filter(HashAlgorithm::isCollisionSafe))
            .collect(Collectors.toUnmodifiableSet());

        final Set<Party> counterParties;
        {
            final var keyStorePath = properties.getPathOrThrow("kalix.core.cp.counter-parties.keystore-path");
            final var keyStorePassword = properties.getString("kalix.core.cp.counter-parties.keystore-password");

            final var keyStoreFile = keyStorePath.toFile();
            final var keyStore = keyStorePassword.isPresent()
                ? KeyStore.getInstance(keyStoreFile, keyStorePassword.get().toCharArray())
                : KeyStore.getInstance(keyStoreFile, (KeyStore.LoadStoreParameter) null);

            counterParties = new HashSet<>();
            for (final var alias : Collections.list(keyStore.aliases())) {
                counterParties.add(new Party(keyStore.getCertificate(alias), acceptedHashAlgorithms));
            }
        }

        final Set<OwnedParty> ownedParties;
        {
            final var keyStorePath = properties.getPathOrThrow("kalix.core.cp.owned-party.keystore-path");
            final var keyStorePassword = properties.getString("kalix.core.cp.owned-party.keystore-password");
            var keyAlias = properties.getString("kalix.core.cp.owned-party.key-alias").orElse(null);
            final var keyPassword = properties.getString("kalix.core.cp.owned-party.key-password");

            final var keyStoreFile = keyStorePath.toFile();
            final var keyStore = keyStorePassword.isPresent()
                ? KeyStore.getInstance(keyStoreFile, keyStorePassword.get().toCharArray())
                : KeyStore.getInstance(keyStoreFile, (KeyStore.LoadStoreParameter) null);

            if (keyAlias == null) {
                final var keyAliases = new StringBuilder(0);
                for (final var alias : Collections.list(keyStore.aliases())) {
                    if (keyStore.isKeyEntry(alias)) {
                        if (keyAlias == null) {
                            keyAlias = alias;
                        }
                        else {
                            keyAliases.append(alias).append(", ");
                        }
                    }
                }
                if (keyAlias == null) {
                    throw new KeyStoreException("No alias in the key store " +
                        "at \"" + keyStorePath + "\" is associated with a" +
                        "private key " +
                        (keyPassword.isPresent()
                            ? "accessible with the provided password"
                            : "without a password"));
                }
                if (keyAliases.length() > 0) {
                    throw new KeyStoreException("The following aliases are " +
                        "associated with private keys in the provided key" +
                        "store " + keyAliases + keyAlias + "; select which " +
                        "of them to use by specifying " +
                        "\"kalix.core.cp.owned-party.key-alias\"");
                }
            }

            final var protection = keyPassword
                .map(password -> new KeyStore.PasswordProtection(password.toCharArray()))
                .orElse(null);

            final var entry = keyStore.getEntry(keyAlias, protection);
            if (!(entry instanceof KeyStore.PrivateKeyEntry)) {
                throw new KeyStoreException("Alias \"" + keyAlias + "\" is " +
                    "not associated with a private key; cannot load key store");
            }
            final var privateKeyEntry = (KeyStore.PrivateKeyEntry) entry;

            ownedParties = Set.of(new OwnedParty(
                privateKeyEntry.getCertificate(),
                privateKeyEntry.getPrivateKey(),
                acceptedHashAlgorithms));
        }

        final Set<Template> templates;
        {
            final var parts = properties.getStringOrThrow("kalix.core.cp.template-paths").split(":");

            templates = new HashSet<>();
            for (final var part : parts) {
                final var path = Path.of(part.trim());
                final var name = path.getFileName().toString();
                final var text = Files.readString(path, StandardCharsets.UTF_8);
                templates.add(new Template(name, text, acceptedHashAlgorithms));
            }
        }

        return new ContractProxy.Builder()
            .acceptedHashAlgorithms(acceptedHashAlgorithms)
            .counterParties(counterParties)
            .ownedParties(ownedParties)
            .templates(templates)
            .relay(new HttpJsonContractRelay(system))
            .build();
    }

    private static ArSystem createSystem(final Properties properties)
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

            final var trustStorePath = properties.getPathOrThrow("kalix.truststore.keystore-path");
            final var trustStorePassword = properties.getString("kalix.truststore.keystore-password");

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
            .getInetSocketAddressOrThrow("kalix.core.service-registry.host");
        if (serviceRegistrySocketAddress.getPort() == 0) {
            serviceRegistrySocketAddress = InetSocketAddress.createUnresolved(
                serviceRegistrySocketAddress.getHostString(),
                properties.getInteger("kalix.core.service-registry.port").orElse(0));
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
                .map(ArServiceDescriptionCache::withEntryLifetimeLimit)
                .orElseGet(ArServiceDescriptionCache::withDefaultEntryLifetimeLimit))
            .plugins(HttpJsonCloudPlugin.joinViaServiceRegistryAt(serviceRegistrySocketAddress))
            .build();
    }
}
