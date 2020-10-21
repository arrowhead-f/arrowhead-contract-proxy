package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.bank.Definition;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.core.cp.security.SignatureBase64;
import se.arkalix.dto.DtoEqualsHashCode;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoEqualsHashCode
@DtoToString
public interface SignedContractOffer extends SignedMessage, Definition {
    HashBase64 offerorFingerprint();

    HashBase64 receiverFingerprint();

    Instant validAfter();

    Instant validUntil();

    List<ContractBase64> contracts();

    default Stream<Hash> hashReferencesInArguments() {
        return contracts()
            .stream()
            .flatMap(ContractBase64::hashReferencesInArguments);
    }

    default SignedContractOfferDto sign(final OwnedParty party) {
        if (!party.signatureScheme().equals(signature().scheme())) {
            throw new IllegalArgumentException("Signature scheme in this " +
                "offer must match that of the provided party; " +
                signature().scheme() + " != " + party.signatureScheme());
        }
        final var self = (SignedContractOfferDto) this;
        return new SignedContractOfferBuilder()
            .negotiationId(self.negotiationId())
            .offerorFingerprint(self.offerorFingerprint())
            .receiverFingerprint(self.receiverFingerprint())
            .validAfter(self.validAfter())
            .validUntil(self.validUntil())
            .contracts(self.contractsAsDtos())
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

        return builder
            .append('}');
    }
}
