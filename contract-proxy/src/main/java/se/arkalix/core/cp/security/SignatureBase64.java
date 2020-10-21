package se.arkalix.core.cp.security;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.security.PublicKey;
import java.security.cert.Certificate;
import java.time.Instant;
import java.util.Base64;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SignatureBase64 {
    Instant timestamp();

    SignatureScheme scheme();

    String sum();

    default byte[] sumToBytes() {
        return Base64.getDecoder().decode(sum());
    }

    static SignatureBase64Dto emptyFrom(final Instant timestamp, final SignatureScheme scheme) {
        return new SignatureBase64Builder()
            .timestamp(timestamp)
            .scheme(scheme)
            .sum("")
            .build();
    }

    static SignatureBase64Dto from(final Signature signature) {
        return new SignatureBase64Builder()
            .timestamp(signature.timestamp())
            .scheme(signature.scheme())
            .sum(Base64.getEncoder().encodeToString(signature.sum()))
            .build();
    }

    default Signature toSignature() {
        return new Signature(timestamp(), scheme(), sumToBytes());
    }

    default boolean verify(final Certificate certificate, final byte[] data) {
        return verify(certificate.getPublicKey(), data);
    }

    default boolean verify(final PublicKey publicKey, final byte[] data) {
        return toSignature().verify(publicKey, data);
    }

    default void writeCanonicalJson(final StringBuilder builder, final boolean includeSum) {
        builder.append("{\"timestamp\":\"")
            .append(timestamp())
            .append("\",\"scheme\":\"")
            .append(scheme())
            .append("\",\"sum\":\"");

        if (includeSum) {
            builder.append(sum());
        }

        builder.append("\"}");
    }
}
