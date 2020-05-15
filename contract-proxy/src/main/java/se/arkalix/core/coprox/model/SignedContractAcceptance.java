package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.core.coprox.security.SignatureBase64;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.nio.charset.StandardCharsets;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SignedContractAcceptance {
    long sessionId();

    HashBase64 acceptorFingerprint();

    HashBase64 offerorFingerprint();

    HashBase64 offerHash();

    SignatureBase64 signature();

    default byte[] toCanonicalJson() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder
            .append("{\"sessionId\":")
            .append(sessionId());

        builder.append(",\"acceptorFingerprint\":");
        acceptorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"offerorFingerprint\":");
        offerorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"offerHash\":");
        offerHash().writeCanonicalJson(builder);

        builder.append(",\"signature\":");
        signature().writeCanonicalJson(builder, false);

        builder.append('}');
    }
}
