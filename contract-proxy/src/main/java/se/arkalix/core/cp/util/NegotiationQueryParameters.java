package se.arkalix.core.cp.util;

import se.arkalix.net.http.HttpIncomingRequest;

import java.util.Objects;

public class NegotiationQueryParameters {
    private final String name1;
    private final String name2;
    private final long id;

    public static NegotiationQueryParameters readOrThrow(final HttpIncomingRequest<?> request) {
        final var name1 = request.queryParameter("name1")
            .orElseThrow(() -> new UnsatisfiableRequestException(
                "NO_NAME1", "Expected query parameter \"name1\""));

        final var name2 = request.queryParameter("name2")
            .orElseThrow(() -> new UnsatisfiableRequestException(
                "NO_NAME2", "Expected query parameter \"name2\""));

        final long id;
        try {
            id = request.queryParameter("id")
                .map(Long::parseLong)
                .orElseThrow(() -> new UnsatisfiableRequestException(
                    "NO_ID", "Expected query parameter \"id\""));
        }
        catch (final NumberFormatException exception) {
            throw new UnsatisfiableRequestException(
                "BAD_ID", "Expected query parameter \"id\" to be unsigned 63-bit integer", exception);
        }
        if (id < 0) {
            throw new UnsatisfiableRequestException(
                "BAD_ID", "Expected query parameter \"id\" to be a unsigned 63-bit integer");
        }
        return new NegotiationQueryParameters(name1, name2, id);
    }

    private NegotiationQueryParameters(final String name1, final String name2, final long id) {
        this.name1 = Objects.requireNonNull(name1, "Expected name1");
        this.name2 = Objects.requireNonNull(name2, "Expected name2");
        this.id = id;
    }

    public String name1() {
        return name1;
    }

    public String name2() {
        return name2;
    }

    public long id() {
        return id;
    }
}
