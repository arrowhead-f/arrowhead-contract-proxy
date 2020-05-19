package se.arkalix.core.cp.contract;

import se.arkalix.dto.DtoToString;
import se.arkalix.dto.DtoWritableAs;

import static se.arkalix.dto.DtoEncoding.JSON;

@DtoWritableAs(JSON)
@DtoToString
public interface TrustedTemplate {
    String name();

    String text();

    static TrustedTemplateDto from(final Template template) {
        return new TrustedTemplateBuilder()
            .name(template.name())
            .text(template.text())
            .build();
    }
}
