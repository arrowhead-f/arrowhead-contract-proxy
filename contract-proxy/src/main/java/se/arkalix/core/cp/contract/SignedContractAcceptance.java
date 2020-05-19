package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.core.cp.security.SignatureBase64;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.nio.charset.StandardCharsets;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface SignedContractAcceptance extends SignedMessage {
    long negotiationId();

    HashBase64 acceptorFingerprint();

    HashBase64 offerorFingerprint();

    HashBase64 offerHash();

    @Override
    SignatureBase64 signature();

    default SignedContractAcceptanceDto sign(final OwnedParty party) {
        if (!party.signatureScheme().equals(signature().scheme())) {
            throw new IllegalArgumentException("Signature scheme in this " +
                "acceptance must match that of the provided party; " +
                signature().scheme() + " != " + party.signatureScheme());
        }
        final var self = (SignedContractAcceptanceDto) this;
        return new SignedContractAcceptanceBuilder()
            .negotiationId(self.negotiationId())
            .acceptorFingerprint(self.acceptorFingerprint())
            .offerorFingerprint(self.offerorFingerprint())
            .offerHash(self.offerHash())
            .signature(SignatureBase64.from(party.sign(self.signature().timestamp(), self.toCanonicalForm())))
            .build();
    }

    @Override
    default byte[] toCanonicalForm() {
        final var builder = new StringBuilder();
        writeCanonicalJson(builder);
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder
            .append("{\"negotiationId\":")
            .append(negotiationId());

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
