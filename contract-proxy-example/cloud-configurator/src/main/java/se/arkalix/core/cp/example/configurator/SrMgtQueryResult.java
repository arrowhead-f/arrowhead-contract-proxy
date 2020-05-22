package se.arkalix.core.cp.example.configurator;

import se.arkalix.dto.DtoReadableAs;

import java.util.List;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoReadableAs(JSON)
public interface SrMgtQueryResult {
    int count();
    List<SrMgtEntry> data();
}
