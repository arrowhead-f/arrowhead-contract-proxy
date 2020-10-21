package se.arkalix.core.cp.security;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public interface Hashable {
    byte[] canonicalize();

    default Hash hashUsing(final HashAlgorithm hashAlgorithm) {
        return hashAlgorithm.hash(canonicalize());
    }

    default List<Hash> hashUsing(final Collection<HashAlgorithm> hashAlgorithms) {
        final var data = canonicalize();
        return hashAlgorithms.stream()
            .map(hashAlgorithm -> hashAlgorithm.hash(data))
            .collect(Collectors.toUnmodifiableList());
    }
}
