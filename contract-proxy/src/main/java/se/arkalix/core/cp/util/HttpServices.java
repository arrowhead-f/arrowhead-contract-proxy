package se.arkalix.core.cp.util;

import se.arkalix.core.cp.util.UnsatisfiableRequestException;
import se.arkalix.core.plugin.ErrorResponseBuilder;
import se.arkalix.net.http.service.HttpService;

import static se.arkalix.net.http.HttpStatus.BAD_REQUEST;
import static se.arkalix.util.concurrent.Future.done;

public class HttpServices {
    private HttpServices() {}

    public static HttpService newWithUnsatisfiableRequestCatcher() {
        return new HttpService()
            .catcher(UnsatisfiableRequestException.class, (exception, request, response) -> {
                response
                    .status(BAD_REQUEST)
                    .body(new ErrorResponseBuilder()
                        .code(BAD_REQUEST.code())
                        .message(exception.getMessage())
                        .type(exception.type())
                        .build());
                return done();
            });
    }
}
