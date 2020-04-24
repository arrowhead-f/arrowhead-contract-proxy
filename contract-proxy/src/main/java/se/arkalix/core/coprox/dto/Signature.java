package se.arkalix.core.coprox.dto;

import se.arkalix.core.coprox.security.SignatureScheme;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.time.Instant;
import java.util.Base64;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Signature {
    @JsonName("Timestamp")
    Instant timestamp();

    @JsonName("Scheme")
    SignatureScheme scheme();

    @JsonName("Sum")
    String sumAsBase64();

    default byte[] sum() {
        return Base64.getDecoder().decode(sumAsBase64());
    }

    default se.arkalix.core.coprox.security.Signature toSignature() {
        return new se.arkalix.core.coprox.security.Signature(timestamp(), scheme(), sum());
    }

    default void writeCanonicalJson(final StringBuilder builder, final boolean includeSum) {
        builder.append("{\"Timestamp\":\"")
            .append(timestamp())
            .append("\",\"Scheme\":\"")
            .append(scheme());

        if (includeSum) {
            builder
                .append("\",\"Sum\":\"")
                .append(sumAsBase64());
        }

        builder.append("\"}");
    }
}
