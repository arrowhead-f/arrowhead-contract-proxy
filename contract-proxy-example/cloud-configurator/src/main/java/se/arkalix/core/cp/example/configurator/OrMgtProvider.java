package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
public interface OrMgtProvider {
    String address();
    String authenticationInfo();
    int port();
    String systemName();
}
