package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface OrMgtRule {
    int consumerSystemId();
    int priority();
    OrMgtProvider providerSystem();
    String serviceDefinitionName();
    String serviceInterfaceName();
}
