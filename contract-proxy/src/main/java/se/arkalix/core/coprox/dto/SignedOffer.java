package se.arkalix.core.coprox.dto;

import se.arkalix.core.coprox.model.Offer;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SignedOffer {
    @JsonName("SessionId")
    long sessionId();

    @JsonName("OfferorFingerprint")
    Hash offerorFingerprint();

    @JsonName("ReceiverFingerprint")
    Hash receiverFingerprint();

    @JsonName("ValidAfter")
    Instant validAfter();

    @JsonName("ValidUntil")
    Instant validUntil();

    @JsonName("Contracts")
    List<Contract> contracts();

    @JsonName("Signature")
    Signature signature();

    default SignedOfferBuilder rebuild() {
        final var self = (SignedOfferDto) this;
        return new SignedOfferBuilder()
            .sessionId(self.sessionId())
            .offerorFingerprint((HashDto) self.offerorFingerprint())
            .receiverFingerprint((HashDto) self.receiverFingerprint())
            .validAfter(self.validAfter())
            .validUntil(self.validUntil())
            .contracts(self.contractsDto())
            .signature((SignatureDto) self.signature());
    }

    default byte[] toCanonicalJson() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder, false);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default Offer toOffer() {
        return new Offer.Builder()
            .offerorFingerprint(offerorFingerprint().toHash())
            .receiverFingerprint(receiverFingerprint().toHash())
            .validAfter(validAfter())
            .validUntil(validUntil())
            .contracts(contracts().stream().map(Contract::toContract).collect(Collectors.toList()))
            .signature(signature().toSignature())
            .build();
    }

    default void writeCanonicalJson(final StringBuilder builder, final boolean includeSignatureSum) {
        builder
            .append("{\"SessionId\":")
            .append(sessionId());

        builder.append(",\"OfferorFingerprint\":");
        offerorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"ReceiverFingerprint\":");
        receiverFingerprint().writeCanonicalJson(builder);

        builder
            .append(",\"ValidAfter\":\"")
            .append(validAfter())
            .append("\",\"ValidUntil\":\"")
            .append(validUntil())
            .append("\",\"Contracts\":[");

        var i = 0;
        for (final var contract : contracts()) {
            if (i++ != 0) {
                builder.append(',');
            }
            contract.writeCanonicalJson(builder);
        }

        builder.append("],\"Signature\":");
        signature().writeCanonicalJson(builder, includeSignatureSum);
        builder.append('}');
    }
}
