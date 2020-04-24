package se.arkalix.core.coprox.dto;

import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface ErrorBadHashPointer extends Error {
    @JsonName("HashPointer")
    Hash_ hashPointer();
}
