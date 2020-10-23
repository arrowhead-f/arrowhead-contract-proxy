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

    static DefinitionMessageDto from(final HashBase64Dto hash, final SignedContractAcceptanceDto acceptance) {
        return new DefinitionMessageBuilder()
            .acceptance(acceptance)
            .hashes(hash)
            .build();
    }

    static DefinitionMessageDto from(final HashBase64Dto hash, final SignedContractOfferDto offer) {
        return new DefinitionMessageBuilder()
            .offer(offer)
            .hashes(hash)
            .build();
    }

    static DefinitionMessageDto from(final HashBase64Dto hash, final SignedContractRejectionDto rejection) {
        return new DefinitionMessageBuilder()
            .rejection(rejection)
            .hashes(hash)
            .build();
    }

    static DefinitionMessageDto from(final Definition definition) {
        return from(null, definition);
    }

    static DefinitionMessageDto from(final Hash hash, final Definition definition) {
        final var hash0 = hash != null ? HashBase64.from(hash) : null;
        if (definition instanceof SignedContractAcceptanceDto) {
            return from(hash0, (SignedContractAcceptanceDto) definition);
        }
        if (definition instanceof SignedContractOfferDto) {
            return from(hash0, (SignedContractOfferDto) definition);
        }
        if (definition instanceof SignedContractRejectionDto) {
            return from(hash0, (SignedContractRejectionDto) definition);
        }
        throw new InternalException("Cannot create DefinitionMessageDto from " + definition);
    }
}

