package se.arkalix.core.coprox.model;

@FunctionalInterface
public interface ContractSessionObserver {
    void onEvent(ContractSessionEvent event);
}
