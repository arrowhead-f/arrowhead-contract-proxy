package se.arkalix.core.coprox.model;

import se.arkalix.core.coprox.security.Hash;
import se.arkalix.core.coprox.security.HashAlgorithm;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Template {
    private static final Pattern PATTERN = Pattern.compile("\\{([\\w]+)}");

    private final String name;
    private final String text;
    private final Map<String, Span> parameters;
    private final List<Hash> acceptedHashes;
    private final Hash preferredHash;

    public Template(final String name, final String text, final Set<HashAlgorithm> supportedHashAlgorithms) {
        this.name = Objects.requireNonNull(name, "Expected name");
        this.text = Objects.requireNonNull(text, "Expected text");

        final var parameters = new HashMap<String, Span>();
        final var matcher = PATTERN.matcher(text);
        while (matcher.find()) {
            final var key = matcher.group(1);
            parameters.put(key, new Span(matcher.start(), matcher.end()));
        }
        this.parameters = parameters;

        final var textAsBytes = text.getBytes(StandardCharsets.UTF_8);
        acceptedHashes = supportedHashAlgorithms.stream()
            .map(hashAlgorithm -> hashAlgorithm.hash(textAsBytes))
            .collect(Collectors.toUnmodifiableList());
        preferredHash = acceptedHashes.stream()
            .filter(fingerprint -> fingerprint.algorithm().isCollisionSafe())
            .findFirst()
            .orElseGet(() -> acceptedHashes.get(0));
    }

    public String name() {
        return name;
    }

    public String text() {
        return text;
    }

    public Collection<Hash> acceptedHashes() {
        return acceptedHashes;
    }

    public Hash preferredHash() {
        return preferredHash;
    }

    public String render(final Contract contract) {
        final var builder = new StringBuilder();

        var start = 0;
        for (final var parameterEntry : parameters.entrySet()) {
            final var span = parameterEntry.getValue();
            builder
                .append(text, start, span.start())
                .append('{')
                .append(contract.argument(parameterEntry.getKey())
                    .orElseThrow(() -> new ContractInvalidException("Key \"" +
                        parameterEntry.getKey() + "\" not specified; " +
                        " cannot render contract")))
                .append('}');
            start = span.end();
        }

        return builder
            .append(text, start, text.length())
            .toString();
    }

    public void validate(final Contract contract) {
        final var c0 = new HashSet<>(contract.arguments().keySet());
        final var c1 = new HashSet<>(contract.arguments().keySet());

        final var t0 = new HashSet<>(parameters.keySet());
        final var t1 = new HashSet<>(parameters.keySet());

        c0.removeAll(t0);
        t1.removeAll(c1);

        final var builder = new StringBuilder();
        if (c0.size() != 0) {
            builder
                .append("The following contract arguments are not defined in the template \"")
                .append(name)
                .append("\": [")
                .append(String.join(", ", c0))
                .append(']');
        }
        if (t1.size() != 0) {
            if (builder.length() != 0) {
                builder.append("; the following template parameters of \"");
            }
            else {
                builder.append("The following template parameters of \"");
            }
            builder
                .append(name)
                .append("\" are not specified in the given contract: [")
                .append(String.join(", ", t1))
                .append(']');
        }
        if (builder.length() != 0) {
            builder.append("; contract not valid");
            throw new ContractInvalidException(builder.toString());
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        final Template template = (Template) o;
        return text.equals(template.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(text);
    }

    private static class Span {
        private final int start;
        private final int end;

        public Span(final int start, final int end) {
            this.start = start;
            this.end = end;
        }

        public int start() {
            return start;
        }

        public int end() {
            return end;
        }
    }
}
