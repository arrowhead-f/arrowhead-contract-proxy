package se.arkalix.core.cp.example.initiator;

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
import java.util.List;
import java.util.Map;
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
                .localHostnamePort("172.23.2.10", 9001)
                .plugins(
                    HttpJsonCloudPlugin.joinViaServiceRegistryAt(new InetSocketAddress("172.23.1.12", 8443)),
                    new HttpJsonTrustedContractNegotiatorPlugin())
                .build();

            final var facade = system.pluginFacadeOf(HttpJsonTrustedContractNegotiatorPlugin.class)
                .map(f -> (ArTrustedContractNegotiatorPluginFacade) f)
                .orElseThrow(() -> new IllegalStateException("No " +
                    "HttpJsonTrustedContractNegotiatorPlugin is " +
                    "available; cannot negotiate"));

            logger.info("========== DEMO STARTING ==========");

            final var contract0 = new TrustedContractBuilder()
                .templateName("simple-purchase.txt")
                .arguments(Map.of(
                    "Buyer", "Initiator System",
                    "Seller", "Reactor System",
                    "ArticleNumber", "XYZ-123",
                    "Quantity", "200",
                    "Price", "1290",
                    "Currency", "EUR",
                    "PaymentDate", Instant.now().plus(Duration.ofDays(60)).toString()))
                .build();

            logger.info("== DEMO == Sending offer via \"contract-proxy-initiator\" " +
                "and \"contract-proxy-reactor\" to \"reactor\" ...");
            logger.info("== DEMO == Offer contains the following contract: {}", contract0);

            // Here we are telling this systems's (the Initiator System's) Contract Proxy to send
            // an offer with "Initiator System" and sender (or offeror) to "Reactor System", which
            // is connected to another Contract Proxy.
            facade.offer("Initiator System", "Reactor System", Duration.ofHours(2), List.of(contract0),
                new TrustedContractNegotiatorHandler() {
                    @Override
                    public void onAccept(final TrustedContractNegotiationDto negotiation) {
                        logger.info("== DEMO == Offer accepted by counter-party " + negotiation);
                    }

                    @Override
                    public void onOffer(
                        final TrustedContractNegotiationDto negotiation,
                        final TrustedContractNegotiatorResponder responder)
                    {
                        logger.info("== DEMO == Received " + negotiation);
                        logger.info("== DEMO == Rejecting counter-offer ...");
                        responder.reject()
                            .ifSuccess(ignored -> logger.info("== DEMO == Counter-offer rejected"))
                            .onFailure(throwable -> logger.info("== DEMO == Failed to reject counter-offer", throwable));
                    }

                    @Override
                    public void onReject(final TrustedContractNegotiationDto negotiation) {
                        logger.info("== DEMO == Offer rejected by counter-party " + negotiation);
                    }
                });
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
