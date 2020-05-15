package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.HashBase64;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ContractBase64 {
    HashBase64 templateHash();

    Map<String, String> arguments();

    default void writeCanonicalJson(final StringBuilder builder) {
        builder.append("{\"templateHash\":");
        templateHash().writeCanonicalJson(builder);
        builder.append(",\"arguments\":{")
            .append(arguments()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(Collectors.joining(",")))
            .append("}}");
    }
}
