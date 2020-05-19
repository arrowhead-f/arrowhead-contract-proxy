package se.arkalix.core.cp.contract;

import se.arkalix.core.plugin.cp.ContractNegotiationStatus;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
@DtoToString
public interface TrustedRenderedContract {
    String name();

    String text();

    ContractNegotiationStatus status();
}
