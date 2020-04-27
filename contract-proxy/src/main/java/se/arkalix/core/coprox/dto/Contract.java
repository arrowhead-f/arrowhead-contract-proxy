package se.arkalix.core.coprox.dto;

import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoWritableAs;
import se.arkalix.dto.json.JsonName;

import java.util.Map;
import java.util.stream.Collectors;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
public interface Contract {
    @JsonName("TemplateHash")
    Hash templateHash();

    @JsonName("Arguments")
    Map<String, String> arguments();

    default se.arkalix.core.coprox.model.Contract toContract() {
        return new se.arkalix.core.coprox.model.Contract(templateHash().toHash(), arguments());
    }

    default void writeCanonicalJson(final StringBuilder builder) {
        builder.append("{\"TemplateHash\":");
        templateHash().writeCanonicalJson(builder);
        builder.append(",\"Arguments\":{")
            .append(arguments()
                .entrySet()
                .stream()
                .sorted()
                .map(entry -> "\"" + entry.getKey() + "\":" + entry.getValue())
                .collect(Collectors.joining(",")))
            .append("}}");
    }
}
