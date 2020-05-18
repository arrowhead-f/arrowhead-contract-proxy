package se.arkalix.core.coprox.model;

import se.arkalix.core.plugin.cp.ContractNegotiationStatus;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

public class ContractNegotiationEvent {
    private final long negotiationId;
    private final String offerorName;
    private final String receiverName;
    private final ContractNegotiationStatus status;
    private final Set<String> templateNames;

    private ContractNegotiationEvent(final Builder builder) {
        negotiationId = Objects.requireNonNull(builder.negotiationId, "Expected negotiationId");
        offerorName = Objects.requireNonNull(builder.offerorName, "Expected offerorName");
        receiverName = Objects.requireNonNull(builder.receiverName, "Expected receiverName");
        status = Objects.requireNonNull(builder.status, "Expected status");
        templateNames = Collections.unmodifiableSet(
            Objects.requireNonNull(builder.templateNames, "Expected templateNames"));
    }

    public long negotiationId() {
        return negotiationId;
    }

    public String offerorName() {
        return offerorName;
    }

    public String receiverName() {
        return receiverName;
    }

    public ContractNegotiationStatus status() {
        return status;
    }

    public Set<String> templateNames() {
        return templateNames;
    }

    public static class Builder {
        private Long negotiationId;
        private String offerorName;
        private String receiverName;
        private ContractNegotiationStatus status;
        private Set<String> templateNames;

        public Builder negotiationId(final Long negotiationId) {
            this.negotiationId = negotiationId;
            return this;
        }

        public Builder offerorName(final String offerorName) {
            this.offerorName = offerorName;
            return this;
        }

        public Builder receiverName(final String receiverName) {
            this.receiverName = receiverName;
            return this;
        }

        public Builder status(final ContractNegotiationStatus status) {
            this.status = status;
            return this;
        }

        public Builder templateNames(final Set<String> templateNames) {
            this.templateNames = templateNames;
            return this;
        }

        public ContractNegotiationEvent build() {
            return new ContractNegotiationEvent(this);
        }
    }
}
