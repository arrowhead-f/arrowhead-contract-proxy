package se.arkalix.core.cp.example.configurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.net.MessageIncoming;
import se.arkalix.net.http.client.HttpClient;
import se.arkalix.net.http.client.HttpClientRequest;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;
import se.arkalix.util.concurrent.Futures;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.logging.Level;

import static se.arkalix.dto.DtoEncoding.JSON;
import static se.arkalix.net.http.HttpMethod.POST;

/**
 * This system exists solely for the purpose of registering all non-mandatory
 * core services and other services, as well as creating authorization and
 * orchestration rules for those services. When done, it starts to accept
 * incoming connections via port 9999, which allows other systems to wait for
 * it to complete by using the `wait-for.sh` script.
 *
 * As this system uses the management services provided by the service registry,
 * authorization system and orchestrator, it must use a so-called "sysop"
 * (System Operator) certificate. The particulars of this procedure are likely
 * to change with future versions of Eclipse Arrowhead.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        try {
            final var password = new char[]{'1', '2', '3', '4', '5', '6'};
            final var client = new HttpClient.Builder()
                .identity(new OwnedIdentity.Loader()
                    .keyStorePath("keystore.p12")
                    .keyStorePassword(password)
                    .keyPassword(password)
                    .load())
                .trustStore(TrustStore.read("truststore.p12", password))
                .build();

            final var registry = new Registry(client);
            final var authorizer = new Authorizer(client, registry);
            final var orchestrator = new Orchestrator(client, registry);

            Futures.serialize(Config.SERVICES.stream()
                .map(service -> client.send(Config.SR, new HttpClientRequest()
                    .method(POST)
                    .uri("/serviceregistry/mgmt")
                    .body(JSON, service))
                    .flatMap(MessageIncoming::bodyAsString)
                    .ifSuccess(logger::debug)
                    .ifFailure(Throwable.class, fault -> logger.error("Failed to register service", fault))))
                .flatMap(ignored -> registry.refresh())
                .flatMap(ignored -> authorizer.authorize(Config.RULES))
                .flatMap(ignored -> orchestrator.addRules(Config.RULES))
                .fork(ignored -> {
                    // Allow for other systems to determine if configuration is
                    // done by connecting to port 9999 via TCP.
                    final ServerSocket server;
                    try {
                        server = new ServerSocket(9999);
                    }
                    catch (final IOException exception) {
                        exception.printStackTrace();
                        return;
                    }
                    while (!Thread.interrupted()) {
                        try {
                            server.accept().close();
                        }
                        catch (final Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
                })
                .onFailure(Main::panic);
        }
        catch (final Throwable e) {
            panic(e);
        }
    }

    private static void panic(final Throwable cause) {
        logger.error("Configuration failed", cause);
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
