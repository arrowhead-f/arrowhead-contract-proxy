package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.core.coprox.security.SignatureBase64;
import se.arkalix.dto.DtoEqualsHashCode;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoEqualsHashCode
@DtoToString
public interface SignedContractOffer {
    long sessionId();

    HashBase64 offerorFingerprint();

    HashBase64 receiverFingerprint();

    Instant validAfter();

    Instant validUntil();

    List<ContractBase64> contracts();

    SignatureBase64 signature();

    default SignedContractOfferBuilder rebuild() {
        final var self = (SignedContractOfferDto) this;
        return new SignedContractOfferBuilder()
            .sessionId(self.sessionId())
            .offerorFingerprint(self.offerorFingerprint())
            .receiverFingerprint(self.receiverFingerprint())
            .validAfter(self.validAfter())
            .validUntil(self.validUntil())
            .contracts(self.contractsAsDtos())
            .signature(self.signature());
    }

    default byte[] toCanonicalJson() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder, false);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default void writeCanonicalJson(final StringBuilder builder, final boolean includeSignatureSum) {
        builder
            .append("{\"sessionId\":")
            .append(sessionId());

        builder.append(",\"offerorFingerprint\":");
        offerorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"receiverFingerprint\":");
        receiverFingerprint().writeCanonicalJson(builder);

        builder
            .append(",\"validAfter\":\"")
            .append(validAfter())
            .append("\",\"validUntil\":\"")
            .append(validUntil())
            .append("\",\"contracts\":[");

        var i = 0;
        for (final var contract : contracts()) {
            if (i++ != 0) {
                builder.append(',');
            }
            contract.writeCanonicalJson(builder);
        }

        builder.append("],\"signature\":");
        signature().writeCanonicalJson(builder, includeSignatureSum);

        builder.append('}');
    }
}
