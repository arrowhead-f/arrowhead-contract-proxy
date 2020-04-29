package se.arkalix.core.coprox.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.time.Instant;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface TrustedRejection {
    @JsonName("SessionId")
    long sessionId();

    @JsonName("RejectedAt")
    Instant rejectedAt();
}
