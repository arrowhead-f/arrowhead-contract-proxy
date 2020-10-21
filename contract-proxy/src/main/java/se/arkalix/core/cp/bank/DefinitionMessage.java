package se.arkalix.core.cp.bank;

import se.arkalix.core.cp.contract.*;
import se.arkalix.core.cp.security.HashBase64;
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

    static DefinitionMessageDto from(final SignedContractAcceptanceDto acceptance) {
        return new DefinitionMessageBuilder()
            .acceptance(acceptance)
            .build();
    }

    static DefinitionMessageDto from(final SignedContractOfferDto offer) {
        return new DefinitionMessageBuilder()
            .offer(offer)
            .build();
    }

    static DefinitionMessageDto from(final SignedContractRejectionDto rejection) {
        return new DefinitionMessageBuilder()
            .rejection(rejection)
            .build();
    }

    static DefinitionMessageDto from(final Definition definition) {
        if (definition instanceof SignedContractAcceptanceDto) {
            return from((SignedContractAcceptanceDto) definition);
        }
        if (definition instanceof SignedContractOfferDto) {
            return from((SignedContractOfferDto) definition);
        }
        if (definition instanceof SignedContractRejectionDto) {
            return from((SignedContractRejectionDto) definition);
        }
        throw new InternalException("Cannot create DefinitionMessageDto from " + definition);
    }
}

