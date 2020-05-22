package se.arkalix.core.cp.example.configurator;

import se.arkalix.net.http.client.HttpClient;
import se.arkalix.security.identity.OwnedIdentity;
import se.arkalix.security.identity.TrustStore;

import java.net.ServerSocket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Main {
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

            final var rules = new ServiceConsumptionRule[]{
                new ServiceConsumptionRule()
                    .consumer("contract-initiator")
                    .services("event-subscribe", "event-unsubscribe")
                    .providers("event_handler"),
                new ServiceConsumptionRule()
                    .consumer("contract-initiator")
                    .services("trusted-contract-negotiation", "trusted-contract-observation")
                    .providers("contract-proxy-initiator"),

                new ServiceConsumptionRule()
                    .consumer("contract-proxy-initiator")
                    .services("contract-negotiation")
                    .providers("contract-proxy-reactor"),

                new ServiceConsumptionRule()
                    .consumer("contract-proxy-reactor")
                    .services("contract-negotiation")
                    .providers("contract-proxy-initiator"),

                new ServiceConsumptionRule()
                    .consumer("contract-reactor")
                    .services("event-subscribe", "event-unsubscribe")
                    .providers("event_handler"),
                new ServiceConsumptionRule()
                    .consumer("contract-reactor")
                    .services("trusted-contract-negotiation", "trusted-contract-observation")
                    .providers("contract-proxy-reactor"),
            };

            registry.refresh()
                .flatMap(ignored -> authorizer.authorize(rules))
                .flatMap(ignored -> orchestrator.addRules(rules))
                .fork(ignored -> {
                    try {
                        final var server = new ServerSocket(9999);
                        final var socket = server.accept();
                        final var channel = socket.getChannel();
                        channel.configureBlocking(true);
                        channel.write(ByteBuffer.wrap("I'm done!".getBytes(StandardCharsets.UTF_8)));
                        channel.close();
                        System.out.println("I'm done!");
                        System.exit(0);
                    }
                    catch (final Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
                .onFailure(Throwable::printStackTrace);
        }
        catch (final Throwable e) {
            e.printStackTrace();
            System.exit(1);
        }
    }
}
