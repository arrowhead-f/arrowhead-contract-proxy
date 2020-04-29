package se.arkalix.core.coprox.model;

public class BadSignatureSumException extends BadRequestException {
    private final String sumAsBase64;

    public BadSignatureSumException(final String sumAsBase64) {
        super("Signature invalid");
        this.sumAsBase64 = sumAsBase64;
    }

    public String sumAsBase64() {
        return sumAsBase64;
    }
}
