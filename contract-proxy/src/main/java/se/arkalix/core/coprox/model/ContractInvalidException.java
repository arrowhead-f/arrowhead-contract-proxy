package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.util.UnsatisfiableRequestException;

public class ContractInvalidException extends UnsatisfiableRequestException {
    public ContractInvalidException(final String message) {
        super("INVALID_CONTRACT", message);
    }
}
