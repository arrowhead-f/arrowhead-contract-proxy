package se.arkalix.core.coprox.util;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public class Properties {
    private static final Pattern PATTERN_INET_SOCKET_ADDRESS = Pattern
        .compile("^(?:(?:\\[([^]\\s]*)])|([^:\\s]*))(?::([0-9]+))?$");

    private final java.util.Properties inner;
    private final String name;

    public Properties(final java.util.Properties properties, final String name) {
        inner = Objects.requireNonNullElseGet(properties, java.util.Properties::new);
        this.name = Objects.requireNonNull(name, "Expected name");
    }

    public static Properties read(final Path path) throws IOException {
        final var properties = new java.util.Properties();
        try (final var input = Files.newInputStream(path, StandardOpenOption.READ)) {
            properties.load(input);
        }
        return new Properties(properties, path.toString());
    }

    public Optional<Boolean> getBoolean(final String key) {
        var property = inner.getProperty(key);
        if (property == null) {
            return Optional.empty();
        }
        property = property.trim();
        if ("true".equalsIgnoreCase(property)) {
            return Optional.of(true);
        }
        if ("false".equalsIgnoreCase(property)) {
            return Optional.of(false);
        }
        throw invalidProperty(key, "must be true or false");
    }

    public Optional<Duration> getDuration(final String key) {
        return getString(key).map(Duration::parse);
    }

    public Optional<InetSocketAddress> getInetSocketAddress(final String key) {
        final var property = inner.getProperty(key);
        if (property == null) {
            return Optional.empty();
        }
        final var matcher = PATTERN_INET_SOCKET_ADDRESS.matcher(property.trim());
        if (!matcher.matches()) {
            throw invalidProperty(key, "must match " + PATTERN_INET_SOCKET_ADDRESS);
        }
        final var ipv6Hostname = matcher.group(1);
        final var ipv4Hostname = matcher.group(2);
        final var hostname = ipv6Hostname != null
            ? ipv6Hostname
            : ipv4Hostname;
        final var port = matcher.group(3);
        return Optional.of(InetSocketAddress.createUnresolved(hostname, port != null
            ? Integer.parseInt(port)
            : 0));
    }

    public Optional<Integer> getInteger(final String key) {
        return getString(key).map(Integer::parseInt);
    }

    public Optional<Path> getPath(final String key) {
        return getString(key).map(Path::of);
    }

    public Path getPathOrThrow(final String key) {
        return getPath(key).orElseThrow(() -> missingProperty(key));
    }

    public Optional<String> getString(final String key) {
        return Optional.ofNullable(inner.getProperty(key));
    }

    public String getStringOrThrow(final String key) {
        return getString(key).orElseThrow(() -> missingProperty(key));
    }

    private IllegalArgumentException invalidProperty(final String key, final String issue) {
        return new IllegalArgumentException("Field " + key + " in " + name +
            " has an invalid value; " + issue);
    }

    private IllegalArgumentException missingProperty(final String key) {
        return new IllegalArgumentException("Expected the field " + key +
            " to be defined in " + key);
    }

    public boolean isDefined(final String key) {
        return inner.containsKey(key);
    }
}
