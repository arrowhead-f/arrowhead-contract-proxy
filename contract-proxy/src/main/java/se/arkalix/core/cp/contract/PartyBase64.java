package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.dto.DtoWritableAs;

import java.util.List;
import java.util.stream.Collectors;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface PartyBase64 {
    String name();

    List<HashBase64> fingerprints();

    static PartyBase64Dto from(final Party party) {
        return new PartyBase64Builder()
            .name(party.commonName())
            .fingerprints(party.acceptedFingerprints()
                .stream()
                .map(HashBase64::from)
                .collect(Collectors.toUnmodifiableList()))
            .build();
    }
}
