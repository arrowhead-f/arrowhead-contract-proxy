package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoReadableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface SrMgtProvider {
    String address();
    String authenticationInfo();
    String createdAt();
    int id();
    int port();
    String systemName();
    String updatedAt();
}
