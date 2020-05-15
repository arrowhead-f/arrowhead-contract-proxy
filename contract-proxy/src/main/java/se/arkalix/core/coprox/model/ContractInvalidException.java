package se.arkalix.core.coprox.model;

public class ContractInvalidException extends RuntimeException {
    public ContractInvalidException(final String message) {
        super(message);
    }
}
