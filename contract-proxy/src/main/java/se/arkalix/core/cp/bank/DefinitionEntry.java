package se.arkalix.core.cp.bank;

import se.arkalix.core.cp.contract.SignedContractAcceptanceDto;
import se.arkalix.core.cp.contract.SignedContractOfferDto;
import se.arkalix.core.cp.contract.SignedContractRejectionDto;
import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.util.InternalException;

import java.util.List;
import java.util.stream.Collectors;

public class DefinitionEntry {
    private final List<Hash> hashes;
    private final Definition definition;

    DefinitionEntry(final List<Hash> hashes, final Definition definition) {
        this.hashes = hashes;
        this.definition = definition;
    }

    public List<Hash> hashes() {
        return hashes;
    }

    public Definition definition() {
        return definition;
    }

    public DefinitionMessageDto toMessage() {
        final var builder = new DefinitionMessageBuilder()
            .hashes(hashes.stream().map(HashBase64::from).collect(Collectors.toUnmodifiableList()));

        if (definition instanceof SignedContractAcceptanceDto) {
            builder.acceptance((SignedContractAcceptanceDto) definition);
        }
        else if (definition instanceof SignedContractOfferDto) {
            builder.offer((SignedContractOfferDto) definition);
        }
        else if (definition instanceof SignedContractRejectionDto) {
            builder.rejection((SignedContractRejectionDto) definition);
        }
        else {
            throw new InternalException("Cannot create DefinitionMessageDto from " + definition);
        }

        return builder.build();
    }
}
