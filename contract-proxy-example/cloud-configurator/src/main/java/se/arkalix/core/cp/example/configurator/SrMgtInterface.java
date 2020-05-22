package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoReadableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface SrMgtInterface {
    String createdAt();
    int id();
    String interfaceName();
    String updatedAt();
}
