package se.arkalix.core.coprox.dto;

import se.arkalix.dto.json.JsonName;

public interface Error {
    @JsonName("Error")
    String name();

    static ErrorBadHashFunctionDto badHashFunction(final String function) {
        return new ErrorBadHashFunctionBuilder()
            .name("BadHashFunction")
            .hashFunction(function)
            .build();
    }

    static ErrorBadRequestDto badRequest(final String cause) {
        return new ErrorBadRequestBuilder()
            .name("BadRequest")
            .cause(cause)
            .build();
    }

    static ErrorBadSessionDto badSession(final long sessionId) {
        return new ErrorBadSessionBuilder()
            .name("BadSession")
            .sessionId(sessionId)
            .build();
    }

    static ErrorBadSignatureSchemeDto badSignatureScheme(final String scheme) {
        return new ErrorBadSignatureSchemeBuilder()
            .name("BadSignatureScheme")
            .signatureScheme(scheme)
            .build();
    }

    static ErrorBadSignatureSumDto badSignatureSum(final String signer, final String sum) {
        return new ErrorBadSignatureSumBuilder()
            .name("BadSignatureSum")
            .signer(signer)
            .signatureSum(sum)
            .build();
    }
}
