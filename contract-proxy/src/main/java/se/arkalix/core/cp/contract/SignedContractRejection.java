package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.bank.Definition;
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
public interface SignedContractRejection extends SignedMessage, Definition {
    HashBase64 rejectorFingerprint();

    HashBase64 offerorFingerprint();

    HashBase64 offerHash();

    default SignedContractRejectionDto sign(final OwnedParty party) {
        if (!party.signatureScheme().equals(signature().scheme())) {
            throw new IllegalArgumentException("Signature scheme in this " +
                "rejection must match that of the provided party; " +
                signature().scheme() + " != " + party.signatureScheme());
        }
        final var self = (SignedContractRejectionDto) this;
        return new SignedContractRejectionBuilder()
            .negotiationId(self.negotiationId())
            .rejectorFingerprint(self.rejectorFingerprint())
            .offerorFingerprint(self.offerorFingerprint())
            .offerHash(self.offerHash())
            .signature(SignatureBase64.from(party.sign(
                self.signature().timestamp(),
                self.canonicalizeWithoutSignatureSum()
            )))
            .build();
    }

    @Override
    default byte[] canonicalize() {
        return writeCanonicalJson(new StringBuilder(), true)
            .toString()
            .getBytes(StandardCharsets.UTF_8);
    }

    @Override
    default byte[] canonicalizeWithoutSignatureSum() {
        return writeCanonicalJson(new StringBuilder(), false)
            .toString()
            .getBytes(StandardCharsets.UTF_8);
    }

    default StringBuilder writeCanonicalJson(final StringBuilder builder, final boolean includeSignatureSum) {
        builder
            .append("{\"negotiationId\":")
            .append(negotiationId());

        builder.append(",\"rejectorFingerprint\":");
        rejectorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"offerorFingerprint\":");
        offerorFingerprint().writeCanonicalJson(builder);

        builder.append(",\"offerHash\":");
        offerHash().writeCanonicalJson(builder);

        builder.append(",\"signature\":");
        signature().writeCanonicalJson(builder, includeSignatureSum);

        return builder
            .append('}');
    }
}
