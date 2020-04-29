package se.arkalix.core.coprox.dto;

import se.arkalix.core.coprox.model.Rejection;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.nio.charset.StandardCharsets;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface SignedRejection {
    @JsonName("SessionId")
    long sessionId();

    @JsonName("RejectorFingerprint")
    Hash rejectorFingerprint();

    @JsonName("ReceiverFingerprint")
    Hash receiverFingerprint();

    @JsonName("Signature")
    Signature signature();

    default byte[] toCanonicalJson() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder, false);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default Rejection toRejection() {
        return new Rejection(rejectorFingerprint().toHash(), receiverFingerprint().toHash(), signature().toSignature());
    }

    default void writeCanonicalJson(final StringBuilder builder, final boolean includeSignatureSum) {
        builder
            .append("{\"SessionId\":")
            .append(sessionId())
            .append(",\"RejectorFingerprint\":");

        rejectorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"ReceiverFingerprint\":");

        receiverFingerprint().writeCanonicalJson(builder);

        builder.append(",\"Signature\":");

        signature().writeCanonicalJson(builder, includeSignatureSum);

        builder.append('}');
    }
}
