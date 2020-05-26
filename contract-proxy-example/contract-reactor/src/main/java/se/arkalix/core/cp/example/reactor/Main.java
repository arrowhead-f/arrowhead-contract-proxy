package se.arkalix.core.cp.example.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.net.http.service.HttpService;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
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
                .localHostnamePort("172.23.3.10", 9002)
                .plugins(
                    HttpJsonCloudPlugin.viaServiceRegistryAt(new InetSocketAddress("172.23.1.12", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            // Unless a service is registered this system cannot have authorization or orchestration rules.
            system.provide(new HttpService()
                .name("contract-reaction")
                .encodings(JSON)
                .basePath("/contract-reaction")
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

                    facade.listen("reactor", () -> new TrustedContractNegotiatorHandler() {
                        @Override
                        public void onAccept(final TrustedContractNegotiationDto negotiation) {
                            throw new IllegalStateException();
                        }

                        @Override
                        public void onOffer(
                            final TrustedContractNegotiationDto negotiation,
                            final TrustedContractNegotiatorResponder responder)
                        {
                            final var offer = negotiation.offer();
                            reject:
                            {
                                if (offer.contracts().size() != 1) {
                                    break reject;
                                }
                                final var contract = offer.contracts().get(0);
                                if (!contract.templateName().equalsIgnoreCase("simple-purchase.txt")) {
                                    break reject;
                                }
                                final var arguments = contract.arguments();
                                if (!offer.offerorName().equalsIgnoreCase(arguments.get("Buyer"))) {
                                    break reject;
                                }
                                if (!offer.receiverName().equalsIgnoreCase(arguments.get("Seller"))) {
                                    break reject;
                                }
                                if (!"XYZ-123".equalsIgnoreCase(arguments.get("ArticleNumber"))) {
                                    break reject;
                                }
                                final long quantity;
                                try {
                                    quantity = Long.parseLong(arguments.get("Quantity"));
                                }
                                catch (final NumberFormatException ignored) {
                                    break reject;
                                }
                                if (quantity <= 0 || quantity > 9000) {
                                    break reject;
                                }
                                final long price;
                                try {
                                    price = Long.parseLong(arguments.get("Price"));
                                }
                                catch (final NumberFormatException ignored) {
                                    break reject;
                                }
                                if (price / quantity < 860) {
                                    break reject;
                                }
                                if (!"EUR".equalsIgnoreCase(arguments.get("Currency"))) {
                                    break reject;
                                }
                                final Instant paymentDate;
                                try {
                                    paymentDate = Instant.parse(arguments.get("PaymentDate"));
                                }
                                catch (final DateTimeParseException ignored) {
                                    break reject;
                                }
                                final var now = Instant.now();
                                if (paymentDate.isBefore(now.plus(Duration.ofDays(15))) ||
                                    paymentDate.isAfter(now.plus(Duration.ofDays(90))))
                                {
                                    break reject;
                                }
                                responder.accept()
                                    .onFailure(fault ->
                                        logger.error("Failed to accept offer in " + negotiation, fault));
                                return;
                            }
                            responder.reject()
                                .onFailure(fault ->
                                    logger.error("Failed to reject offer in " + negotiation, fault));
                        }

                        @Override
                        public void onReject(final TrustedContractNegotiationDto negotiation) {
                            throw new IllegalStateException();
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
