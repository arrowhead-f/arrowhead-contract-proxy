package se.arkalix.core.coprox.model;

import se.arkalix.core.plugin.cp.ContractSessionStatus;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ContractSessionEvent {
    private final long sessionId;
    private final String offerorName;
    private final String receiverName;
    private final ContractSessionStatus status;
    private final Set<String> templateNames;

    public ContractSessionEvent(final Builder builder) {
        sessionId = Objects.requireNonNull(builder.sessionId, "Expected sessionId");
        offerorName = Objects.requireNonNull(builder.offerorName, "Expected offerorName");
        receiverName = Objects.requireNonNull(builder.receiverName, "Expected receiverName");
        status = Objects.requireNonNull(builder.status, "Expected status");
        templateNames = Collections.unmodifiableSet(
            Objects.requireNonNull(builder.templateNames, "Expected templateNames"));
    }

    public long sessionId() {
        return sessionId;
    }

    public String offerorName() {
        return offerorName;
    }

    public String receiverName() {
        return receiverName;
    }

    public ContractSessionStatus status() {
        return status;
    }

    public Set<String> templateNames() {
        return templateNames;
    }

    public static class Builder {
        private Long sessionId;
        private String offerorName;
        private String receiverName;
        private ContractSessionStatus status;
        private Set<String> templateNames;

        public Builder sessionId(final Long sessionId) {
            this.sessionId = sessionId;
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

        public Builder status(final ContractSessionStatus status) {
            this.status = status;
            return this;
        }

        public Builder templateNames(final Set<String> templateNames) {
            this.templateNames = templateNames;
            return this;
        }

        public ContractSessionEvent build() {
            return new ContractSessionEvent(this);
        }
    }
}
