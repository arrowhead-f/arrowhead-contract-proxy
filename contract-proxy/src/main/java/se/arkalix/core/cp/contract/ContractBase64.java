package se.arkalix.core.cp.contract;

import se.arkalix.core.cp.security.Hash;
import se.arkalix.core.cp.security.HashAlgorithm;
import se.arkalix.core.cp.security.HashBase64;
import se.arkalix.dto.DtoReadableAs;
import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
@DtoWritableAs(JSON)
@DtoToString
public interface ContractBase64 {
    HashBase64 templateHash();

    Map<String, String> arguments();

    default Stream<Hash> hashReferencesInArguments() {
        return arguments()
            .entrySet()
            .stream()
            .filter(entry -> entry.getKey().endsWith(":hash"))
            .map(entry -> Hash.valueOf(entry.getValue()));
    }

    default Contract toContract() {
        return new Contract(templateHash().toHash(), arguments());
    }

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
