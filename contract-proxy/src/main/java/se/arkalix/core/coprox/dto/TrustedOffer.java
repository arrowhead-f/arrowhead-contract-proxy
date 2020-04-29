package se.arkalix.core.coprox.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.time.Instant;
import java.util.List;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface TrustedOffer {
    @JsonName("SessionId")
    long sessionId();

    @JsonName("OfferorName")
    String offerorName();

    @JsonName("ReceiverName")
    String receiverName();

    @JsonName("ValidAfter")
    Instant validAfter();

    @JsonName("ValidUntil")
    Instant validUntil();

    @JsonName("Contracts")
    List<Contract> contracts();

    @JsonName("OfferedAt")
    Instant offeredAt();
}
