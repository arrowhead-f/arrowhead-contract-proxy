package se.arkalix.core.cp.example.initiator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpStatus.OK;
import static se.arkalix.security.access.AccessPolicy.token;
import static se.arkalix.util.concurrent.Future.done;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var system = new ArSystem.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .localHostnamePort("172.23.2.10", 9001)
                .plugins(
                    HttpJsonCloudPlugin.viaServiceRegistryAt(new InetSocketAddress("172.23.1.12", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            // Unless a service is registered this system cannot have authorization or orchestration rules.
            system.provide(new HttpService()
                .name("contract-initiation")
                .encodings(JSON)
                .basePath("/contract-initiation")
                .accessPolicy(token())

                .get("/hello", (request, response) -> {
                    response
                        .status(OK)
                        .body("{\"hello\",\"" + request.consumer().name() + "\"}");
                    return done();
                }))

                .ifSuccess(ignored -> {
                    final var facade = system.pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                        .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                        .orElseThrow(() -> new IllegalStateException("No " +
                            "HttpJsonTrustedContractNegotiatorPlugin is " +
                            "available; cannot negotiate"));

                    final var socket = new Socket();
                    socket.connect(new InetSocketAddress("172.23.1.9", 9999));
                    socket.close();

                    final var contract0 = new TrustedContractBuilder()
                        .templateName("simple-purchase.txt")
                        .arguments(Map.of()) // TODO ...
                        .build();

                    facade.offer("initiator", "reactor", Duration.ofHours(2), List.of(contract0),
                        new TrustedContractNegotiatorHandler() {
                            @Override
                            public void onAccept(final TrustedContractNegotiationDto negotiation) {
                                // TODO ...
                            }

                            @Override
                            public void onOffer(
                                final TrustedContractNegotiationDto negotiation,
                                final TrustedContractNegotiatorResponder responder)
                            {
                                // TODO ...
                            }

                            @Override
                            public void onReject(final TrustedContractNegotiationDto negotiation) {
                                // TODO ...
                            }
                        });
                })

                .onFailure(Main::panic);
        }
        catch (final Throwable throwable) {
            panic(throwable);
        }
    }

    private static void panic(final Throwable cause) {
        logger.error("Failed to start contract initiator", cause);
        System.exit(1);
    }

    static {
        final var logLevel = Level.INFO;
        System.setProperty("java.util.logging.SimpleFormatter.format", "%1$tF %1$tT %4$s %5$s%6$s%n");
        final var root = java.util.logging.Logger.getLogger("");
        root.setLevel(logLevel);
        for (final var handler : root.getHandlers()) {
            handler.setLevel(logLevel);
        }
    }
}
