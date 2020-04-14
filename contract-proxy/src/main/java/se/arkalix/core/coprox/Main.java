package se.arkalix.core.coprox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.Arrays;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(final String[] args) {
        final String arg0;
        switch (args.length) {
        case 0:
            arg0 = "./application.properties";
            break;
        case 1:
            arg0 = args[0];
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
            final var pathToConfiguration = Path.of(arg0).toAbsolutePath().normalize();

            if (logger.isInfoEnabled()) {
                logger.info("Reading contract proxy configuration at {}", arg0);
            }
            final var configuration = ContractProxyConfiguration.read(pathToConfiguration);

            if (logger.isInfoEnabled()) {
                logger.info("Creating contract proxy instance");
            }
            final var contractProxy = new ContractProxy(configuration);
            contractProxy.start();
        }
        catch (final Throwable throwable) {
            logger.error("Failed to start contract proxy", throwable);
        }
    }
}
