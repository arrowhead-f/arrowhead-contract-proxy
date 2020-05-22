package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoReadableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface SrMgtServiceDefinition {
    String createdAt();
    int id();
    String serviceDefinition();
    String updatedAt();
}
