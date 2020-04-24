package se.arkalix.core.coprox.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.nio.charset.StandardCharsets;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Acceptance {
    @JsonName("Offer")
    Offer offer();

    @JsonName("Signature")
    Signature signature();

    default se.arkalix.core.coprox.model.Acceptance toAcceptance() {
        return new se.arkalix.core.coprox.model.Acceptance(offer().toOffer(), signature().toSignature());
    }

    default byte[] toCanonicalJson() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder.append("{\"Offer\":");
        offer().writeCanonicalJson(builder, true);
        builder.append(",\"Signature\":");
        signature().writeCanonicalJson(builder, false);
        builder.append('}');
    }
}
