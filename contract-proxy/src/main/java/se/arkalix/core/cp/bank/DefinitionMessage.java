package se.arkalix.core.cp.bank;

import se.arkalix.core.cp.contract.*;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.core.cp.security.HashBase64Dto;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.util.InternalException;

import java.util.List;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface DefinitionMessage {
    List<HashBase64> hashes();

    Optional<SignedContractAcceptance> acceptance();

    Optional<SignedContractOffer> offer();

    Optional<SignedContractRejection> rejection();

    static DefinitionMessageDto from(final SignedContractAcceptanceDto acceptance, final HashBase64Dto... hashes) {
        return new DefinitionMessageBuilder()
            .acceptance(acceptance)
            .hashes(hashes)
            .build();
    }

    static DefinitionMessageDto from(final SignedContractOfferDto offer, final HashBase64Dto... hashes) {
        return new DefinitionMessageBuilder()
            .offer(offer)
            .hashes(hashes)
            .build();
    }

    static DefinitionMessageDto from(final SignedContractRejectionDto rejection, final HashBase64Dto... hashes) {
        return new DefinitionMessageBuilder()
            .rejection(rejection)
            .hashes(hashes)
            .build();
    }

    static DefinitionMessageDto from(final Definition definition, final Hash... hashes) {
        final var hashes0 = new HashBase64Dto[hashes.length];
        for (int i = hashes.length; i-- > 0; ) {
            hashes0[i] = HashBase64.from(hashes[i]);
        }

        if (definition instanceof SignedContractAcceptanceDto) {
            return from((SignedContractAcceptanceDto) definition, hashes0);
        }
        if (definition instanceof SignedContractOfferDto) {
            return from((SignedContractOfferDto) definition, hashes0);
        }
        if (definition instanceof SignedContractRejectionDto) {
            return from((SignedContractRejectionDto) definition, hashes0);
        }

        throw new InternalException("Cannot create DefinitionMessageDto from " + definition);
    }
}

