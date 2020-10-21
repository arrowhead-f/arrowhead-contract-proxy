package se.arkalix.core.cp.bank;

import se.arkalix.core.cp.security.Hashable;
import se.arkalix.dto.DtoReadable;
import se.arkalix.dto.DtoWritable;
import se.arkalix.util.InternalException;

import java.util.Optional;

public interface Definition extends Hashable {
    long negotiationId();

    static Optional<Definition> from(final DefinitionMessage message) {
        Definition definition;

        definition = message.acceptance().orElse(null);
        if (definition != null) {
            return Optional.of(definition);
        }

        definition = message.offer().orElse(null);
        if (definition != null) {
            return Optional.of(definition);
        }

        definition = message.rejection().orElse(null);
        if (definition != null) {
            return Optional.of(definition);
        }

        return Optional.empty();
    }
}
