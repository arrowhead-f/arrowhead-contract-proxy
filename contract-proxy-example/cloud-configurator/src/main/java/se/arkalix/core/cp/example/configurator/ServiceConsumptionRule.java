package se.arkalix.core.cp.example.configurator;

import java.util.List;

public class ServiceConsumptionRule {
    private String consumer;
    private List<String> services;
    private List<String> providers;

    public String consumer() {
        if (consumer == null) {
            throw new IllegalStateException("consumer not set");
        }
        if (consumer.isBlank()) {
            throw new IllegalStateException("consumer is blank");
        }
        return consumer;
    }

    public ServiceConsumptionRule consumer(final String name) {
        consumer = name;
        return this;
    }

    public List<String> services() {
        if (services == null) {
            throw new IllegalStateException("services not set");
        }
        if (services.isEmpty()) {
            throw new IllegalStateException("services is empty");
        }
        if (services.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException("services contains blank service name");
        }
        return services;
    }

    public ServiceConsumptionRule services(final String... names) {
        services = List.of(names);
        return this;
    }

    public List<String> providers() {
        if (providers == null) {
            throw new IllegalStateException("providers not set");
        }
        if (providers.isEmpty()) {
            throw new IllegalStateException("providers is empty");
        }
        if (providers.stream().anyMatch(String::isBlank)) {
            throw new IllegalStateException("providers contains blank system name");
        }
        return providers;
    }

    public ServiceConsumptionRule providers(final String... names) {
        providers = List.of(names);
        return this;
    }
}
