package se.arkalix.core.coprox.dto;

import se.arkalix.core.coprox.security.HashFunction;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.util.Base64;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Hash_ {
    @JsonName("HashFunction")
    HashFunction hashFunction();

    @JsonName("Sum")
    String sumAsBase64();

    default byte[] sum() {
        return Base64.getDecoder().decode(sumAsBase64());
    }

    default se.arkalix.core.coprox.security.Hash toHash() {
        return new se.arkalix.core.coprox.security.Hash(hashFunction(), sum());
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder.append("{\"HashFunction\":\"")
            .append(hashFunction())
            .append("\",\"Sum\":\"")
            .append(sumAsBase64())
            .append("\"}");
    }
}
