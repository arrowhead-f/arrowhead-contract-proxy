package se.arkalix.core.coprox.dto;

import se.arkalix.core.coprox.model.BadHashFunctionExeption;
import se.arkalix.core.coprox.model.BadRequestException;
import se.arkalix.core.coprox.model.BadSessionException;
import se.arkalix.core.coprox.model.BadSignatureSumException;
import se.arkalix.dto.DtoReadable;
import se.arkalix.dto.DtoWritable;
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

    static ErrorBadSignatureSumDto badSignatureSum(final String sum) {
        return new ErrorBadSignatureSumBuilder()
            .name("BadSignatureSum")
            .signatureSum(sum)
            .build();
    }

    static DtoWritable from(final BadRequestException exception) {
        if (exception instanceof BadHashFunctionExeption) {
            return badHashFunction(((BadHashFunctionExeption) exception).function().toString());
        }
        if (exception instanceof BadSessionException) {
            return badSession(((BadSessionException) exception).sessionId());
        }
        if (exception instanceof BadSignatureSumException) {
            return badSignatureSum(((BadSignatureSumException) exception).sumAsBase64());
        }
        return badRequest(exception.getMessage());
    }
}
