package se.arkalix.core.coprox.security;

import se.arkalix.dto.DtoEqualsHashCode;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.util.Base64;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoEqualsHashCode
@DtoToString
public interface HashBase64 {
    HashAlgorithm algorithm();

    String sum();

    default byte[] sumAsBytes() {
        return Base64.getDecoder().decode(sum());
    }

    static HashBase64Dto from(Hash hash) {
        return new HashBase64Builder()
            .algorithm(hash.algorithm())
            .sum(Base64.getEncoder().encodeToString(hash.sum()))
            .build();
    }

    default Hash toHash() {
        return new Hash(algorithm(), sumAsBytes());
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder.append("{\"algorithm\":\"")
            .append(algorithm())
            .append("\",\"sum\":\"")
            .append(sum())
            .append("\"}");
    }
}
