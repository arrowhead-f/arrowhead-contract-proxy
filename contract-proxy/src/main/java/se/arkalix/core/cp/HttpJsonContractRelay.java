package se.arkalix.core.cp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.arkalix.ArSystem;
import se.arkalix.core.cp.contract.*;
import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.core.plugin.cp.TrustedContract;
import se.arkalix.core.plugin.cp.TrustedContractOffer;
import se.arkalix.core.plugin.eh.HttpJsonEventPublishService;
import se.arkalix.internal.core.plugin.HttpJsonServices;
import se.arkalix.internal.core.plugin.Paths;
import se.arkalix.net.http.consumer.HttpConsumer;
import se.arkalix.net.http.consumer.HttpConsumerRequest;
import se.arkalix.util.concurrent.Future;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static se.arkalix.core.plugin.cp.ContractNegotiationConstants.TOPIC_UPDATE;
import static se.arkalix.descriptor.EncodingDescriptor.JSON;
import static se.arkalix.net.http.HttpMethod.POST;

public class HttpJsonContractRelay implements ContractRelay {
    private final ArSystem system;

    public HttpJsonContractRelay(final ArSystem system) {
        this.system = Objects.requireNonNull(system, "Expected system");
    }

    @Override
    public Future<?> sendToEventHandler(
        final long negotiationId,
        final TrustedContractOffer offer,
        final ContractNegotiationStatus status)
    {
        final var metadata = Map.of(
            "offeror", offer.offerorName(),
            "receiver", offer.receiverName(),
            "templates", offer.contracts().stream().map(TrustedContract::templateName).collect(Collectors.joining(",")),
            "status", status.toString().toLowerCase());
        return system.consume()
            .using(HttpJsonEventPublishService.factory())
            .flatMap(service -> service.publish(TOPIC_UPDATE, system, metadata, Long.toString(negotiationId)));
    }

    @Override
    public Future<?> sendToCounterParty(final SignedContractAcceptanceDto acceptance, final Party counterParty) {
        return system.consume()
            .name("contract-negotiation")
            .encodings(JSON)

            // One party per system and use of unreliable identifier. Certificate registry instead of SR?
            .metadata(Map.of("party", counterParty.commonName()))

            .using(HttpConsumer.factory())
            .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                .method(POST)
                .uri(Paths.combine(consumer.service().uri(), "acceptances"))
                .body(acceptance)))
            .flatMap(HttpJsonServices::unwrap);
    }

    @Override
    public Future<?> sendToCounterParty(final SignedContractOfferDto offer, final Party counterParty) {
        return system.consume()
            .name("contract-negotiation")
            .encodings(JSON)

            // One party per system and use of unreliable identifier. Certificate registry instead of SR?
            .metadata(Map.of("party", counterParty.commonName()))

            .using(HttpConsumer.factory())
            .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                .method(POST)
                .uri(Paths.combine(consumer.service().uri(), "offers"))
                .body(offer)))
            .flatMap(HttpJsonServices::unwrap);
    }

    @Override
    public Future<?> sendToCounterParty(final SignedContractRejectionDto rejection, final Party counterParty) {
        return system.consume()
            .name("contract-negotiation")
            .encodings(JSON)

            // One party per system and use of unreliable identifier. Certificate registry instead of SR?
            .metadata(Map.of("party", counterParty.commonName()))

            .using(HttpConsumer.factory())
            .flatMap(consumer -> consumer.send(new HttpConsumerRequest()
                .method(POST)
                .uri(Paths.combine(consumer.service().uri(), "rejections"))
                .body(rejection)))
            .flatMap(HttpJsonServices::unwrap);
    }
}
