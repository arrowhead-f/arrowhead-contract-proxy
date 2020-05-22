package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoReadableAs;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface SrMgtEntry {
    Optional<String> createdAt();
    Optional<String> endOfValidity();
    int id();
    List<SrMgtInterface> interfaces();
    Map<String, String> metadata();
    SrMgtProvider provider();
    String secure();
    SrMgtServiceDefinition serviceDefinition();
    String serviceUri();
    Optional<String> updatedAt();
    int version();
}
