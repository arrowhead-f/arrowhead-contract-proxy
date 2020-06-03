package se.arkalix.core.cp.example.reactor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.plugin.HttpJsonCloudPlugin;
import se.arkalix.core.plugin.cp.*;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.logging.Level;

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
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress("172.23.1.12", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            final var facade = system.pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                .orElseThrow(() -> new IllegalStateException("No " +
                    "HttpJsonTrustedContractNegotiatorPlugin is " +
                    "available; cannot negotiate"));

            // The handler is invoked for all offers with "Reactor System" as intended receiver.
            facade.listen("Reactor System", () -> new TrustedContractNegotiatorHandler() {
                @Override
                public void onAccept(final TrustedContractNegotiationDto negotiation) {
                    // This method can only be invoked if the onOffer() method below produces
                    // counter-offers. As it does not in this example, there is nothing to do
                    // here.
                    throw new IllegalStateException();
                }

                @Override
                public void onOffer(
                    final TrustedContractNegotiationDto negotiation,
                    final TrustedContractNegotiatorResponder responder)
                {
                    final var offer = negotiation.offer();
                    var rejectReason = (String) null;
                    reject:
                    {
                        if (offer.contracts().size() != 1) {
                            rejectReason = "Expected 1 contract";
                            break reject;
                        }
                        final var contract = offer.contracts().get(0);
                        if (!contract.templateName().equalsIgnoreCase("simple-purchase.txt")) {
                            rejectReason = "Expected contract to be named simple-purchase.txt";
                            break reject;
                        }
                        final var arguments = contract.arguments();
                        if (!offer.offerorName().equalsIgnoreCase(arguments.get("Buyer"))) {
                            rejectReason = "Expected Buyer to be \"" + offer.offerorName() + "\"";
                            break reject;
                        }
                        if (!offer.receiverName().equalsIgnoreCase(arguments.get("Seller"))) {
                            rejectReason = "Expected Seller to be \"" + offer.receiverName() + "\"";
                            break reject;
                        }
                        if (!"XYZ-123".equalsIgnoreCase(arguments.get("ArticleNumber"))) {
                            rejectReason = "Expected ArticleNumber to be \"XYZ-123\"";
                            break reject;
                        }
                        final long quantity;
                        try {
                            quantity = Long.parseLong(arguments.get("Quantity"));
                        }
                        catch (final NumberFormatException exception) {
                            rejectReason = "Expected Quantity to be long integer (" + exception + ")";
                            break reject;
                        }
                        if (quantity <= 0 || quantity > 9000) {
                            rejectReason = "Expected Quantity to be between 0 and 9000, inclusive";
                            break reject;
                        }
                        final long price;
                        try {
                            price = Long.parseLong(arguments.get("Price"));
                        }
                        catch (final NumberFormatException exception) {
                            rejectReason = "Expected Price to be long integer (" + exception + ")";
                            break reject;
                        }
                        if (price / quantity > 860) {
                            rejectReason = "Expected Price to be lower ...";
                            break reject;
                        }
                        if (!"EUR".equalsIgnoreCase(arguments.get("Currency"))) {
                            rejectReason = "Expected Currency to be \"EUR\"";
                            break reject;
                        }
                        final Instant paymentDate;
                        try {
                            paymentDate = Instant.parse(arguments.get("PaymentDate"));
                        }
                        catch (final DateTimeParseException exception) {
                            rejectReason = "Expected PaymentDate to be " +
                                "valid ISO8601 string (" + exception + ")";
                            break reject;
                        }
                        final var now = Instant.now();
                        if (paymentDate.isBefore(now.plus(Duration.ofDays(15))) ||
                            paymentDate.isAfter(now.plus(Duration.ofDays(90))))
                        {
                            rejectReason = "Expected PaymentData to " +
                                "be within 15 and 90 days from now";
                            break reject;
                        }
                        responder.accept()
                            .ifSuccess(ignored -> logger.info("== DEMO == Accepted {}", offer))
                            .onFailure(fault -> {
                                if (logger.isErrorEnabled()) {
                                    logger.error("== DEMO == Failed to accept offer in " + negotiation, fault);
                                }
                            });
                        return;
                    }
                    if (logger.isWarnEnabled()) {
                        logger.warn("== DEMO == Received offer issue: " + rejectReason +
                            "; rejecting offer in " + negotiation);
                    }
                    responder.reject()
                        .ifSuccess(ignored -> logger.info("== DEMO == Rejected {}", offer))
                        .onFailure(fault -> {
                            if (logger.isErrorEnabled()) {
                                logger.error("== DEMO == Failed to reject offer in " + negotiation, fault);
                            }
                        });
                }

                @Override
                public void onReject(final TrustedContractNegotiationDto negotiation) {
                    // This method can only be invoked if the onOffer() method above produces
                    // counter-offers. As it does not in this example, there is nothing to do
                    // here.
                    throw new IllegalStateException();
                }
            });
        }
        catch (final Throwable throwable) {
            panic(throwable);
        }
    }

    private static void panic(final Throwable cause) {
        logger.error("Failed to start contract reactor", cause);
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
