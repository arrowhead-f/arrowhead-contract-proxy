package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoWritableAs;

import java.util.List;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface AuMgtIntracloudRule {
    int consumerId();
    List<Integer> interfaceIds();
    List<Integer> providerIds();
    List<Integer> serviceDefinitionIds();
}
