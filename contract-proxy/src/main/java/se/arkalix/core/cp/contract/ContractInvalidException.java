package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.util.UnsatisfiableRequestException;

public class ContractInvalidException extends UnsatisfiableRequestException {
    public ContractInvalidException(final String message) {
        super("INVALID_CONTRACT", message);
    }
}
