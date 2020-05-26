package se.arkalix.core.cp.example.configurator;

import se.arkalix.core.plugin.SystemDetailsBuilder;
import se.arkalix.core.plugin.SystemDetailsDto;
import se.arkalix.core.plugin.sr.ServiceRegistrationBuilder;
import se.arkalix.core.plugin.sr.ServiceRegistrationDto;
import se.arkalix.descriptor.InterfaceDescriptor;
import se.arkalix.descriptor.SecurityDescriptor;

import java.net.InetSocketAddress;
import java.util.List;

public final class Config {
    private Config() {}

    public static final InetSocketAddress AU = new InetSocketAddress("172.23.1.10", 8445);
    public static final InetSocketAddress OR = new InetSocketAddress("172.23.1.11", 8441);
    public static final InetSocketAddress SR = new InetSocketAddress("172.23.1.12", 8443);

    public static final SystemDetailsDto SYSTEM_CONTRACT_INITIATOR = new SystemDetailsBuilder()
        .name("contract-initiator")
        .hostname("172.23.2.10")
        .port(9001)
        .publicKeyBase64("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvO" +
            "SjiR62Tkk9yQ6eraVqyOeAeDx8n1B0xkcmWtdMXla3CnauVg54r5RY9oj9S" +
            "ufbbA2rInIQO2fjmrEEgn4D+V95RGwDpx8ULea+JTtfrJMPiqXe50IrSKmm" +
            "B9mCrWKdObWchGEk82hbLnBMxnGxT05fnhEJzIReAf8PWONAhFxgI5izRr0" +
            "9WOrs7qBpQwgT+9jvO+yzUAPg+kxQj9yhmSoUS+R9wIApxXSlAuieZtBJ1Z" +
            "D9GRP+6zzrcTNM26RS7wdbbd7yeZkaK9szkxeBfOyc9eXOv5jl4Z9jq8Lif" +
            "nMWun6cxePdA9Z9CuQITibHRSdOjclt8JsXm31tnTnGYQIDAQAB")
        .build();

    public static final SystemDetailsDto SYSTEM_CONTRACT_PROXY_INITIATOR = new SystemDetailsBuilder()
        .name("contract-proxy-initiator")
        .hostname("127.23.2.11")
        .port(8901)
        .publicKeyBase64("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAie" +
            "+10W7i0nIkuaYFlAa/+y/EcEshTXEBJcrRe07GxEnhwPGt3gDyOE8H+ZF62" +
            "oFwgN8RGJs46HnA7N7XbMM8UUmDJLvT5uRm/76PEDZ1jieaeE0V/NhLdj6n" +
            "zrwpuw1pvzFIRafRXyoRpTQrsdcfvydaA+Qp0IzMfW1UheVAIa89axsQtoc" +
            "Rp5GYZZ4nsGgYZDQeyg8ND2KSS6kvELspgTrMdPP/B5diOCmm9EBIQyoTve" +
            "PMYiaL7ewGTk/fKyKkp0qwNDK1/kg+lCQpQK9fOLybX0HDupB9H2wtW+vyL" +
            "gEvOzqwui8thqwIGoce+JcbCseeMh2H302FtXcFTxew0wIDAQAB")
        .build();

    public static final SystemDetailsDto SYSTEM_CONTRACT_REACTOR = new SystemDetailsBuilder()
        .name("contract-reactor")
        .hostname("172.23.3.10")
        .port(9002)
        .publicKeyBase64("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxL" +
            "T+fFvyVhQ3e8SaZv8H2r+T92NOpkPOIIy2gTelz1oh055pVh8QQook9xPUU" +
            "YPtIWd//1MHrbMt9pG803tVCkM7O0jLlCFWBqSHfFDX0+URwt3FwSVLIFU8" +
            "inyiR7wGLdu9Mo7jbDPd3RcVO/Eh/bRmVjs54w98L5bgEP1s0nKZu/1MLmN" +
            "6S8Fm5G05IvXE6Kn/TwztIrbuhFMPwk8Zx0svdAlVNflPKNGvhPi7DCH1lc" +
            "0KJASREUib3uJL2mChSQhhB6wxrXbENcQ8+S9ahCwTnGu2dkh/jvBdGt51u" +
            "bcZ5UVArcJ+R3KdS2ncbQk5JwRHFPBIRYRd+971i/2oDQIDAQAB")
        .build();

    public static final SystemDetailsDto SYSTEM_CONTRACT_PROXY_REACTOR = new SystemDetailsBuilder()
        .name("contract-proxy-reactor")
        .hostname("127.23.3.11")
        .port(8902)
        .publicKeyBase64("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkh" +
            "KqM2BwnF8JFLFNPLGC+I3IB6/9yfUXEf72xHoll5Uc90wyXyHD/xi49wrYz" +
            "UT7WyKavXv7DOZ4O1I6zlxuXrUbIf40NhGCi8dI3wXy59TFWyq+CeQUls/l" +
            "k69yHUHXC8MbrKI0Gw0jGOzWdpEeRdUjCKcV8splOsW4lg1GILnqtkCTQ77" +
            "LcAW2veIL+A2WFK6UcuxU6hXloRa7fpooxXFe6W60Fdldn9iZfx4tsWS/pm" +
            "GqovyKMUaDmHDiEVQoBbEbnfCn/wTMx/MZ/S7/VRvWmkDj6OnYCQ1PDhl/M" +
            "bO2pNb4xNUTmR2RnbzTOHQyhz3M6/hjM+vpm9beBG1VfwIDAQAB")
        .build();

    public static final List<ServiceRegistrationDto> SERVICES = List.of(
        new ServiceRegistrationBuilder()
            .name("contract-initiation")
            .uri("/contract-initiation")
            .security(SecurityDescriptor.CERTIFICATE)
            .provider(SYSTEM_CONTRACT_INITIATOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),

        new ServiceRegistrationBuilder()
            .name("contract-negotiation")
            .uri("/contract-negotiation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_INITIATOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),
        new ServiceRegistrationBuilder()
            .name("trusted-contract-negotiation")
            .uri("/trusted-contract-negotiation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_INITIATOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),
        new ServiceRegistrationBuilder()
            .name("trusted-contract-observation")
            .uri("/trusted-contract-observation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_INITIATOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),

        new ServiceRegistrationBuilder()
            .name("contract-reaction")
            .uri("/contract-reaction")
            .security(SecurityDescriptor.CERTIFICATE)
            .provider(SYSTEM_CONTRACT_REACTOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),

        new ServiceRegistrationBuilder()
            .name("contract-negotiation")
            .uri("/contract-negotiation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_REACTOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),
        new ServiceRegistrationBuilder()
            .name("trusted-contract-negotiation")
            .uri("/trusted-contract-negotiation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_REACTOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build(),
        new ServiceRegistrationBuilder()
            .name("trusted-contract-observation")
            .uri("/trusted-contract-observation")
            .security(SecurityDescriptor.TOKEN)
            .provider(SYSTEM_CONTRACT_PROXY_REACTOR)
            .interfaces(InterfaceDescriptor.HTTP_SECURE_JSON)
            .build()
    );

    public static final ServiceConsumptionRule[] RULES = new ServiceConsumptionRule[]{
        new ServiceConsumptionRule()
            .consumer("contract-initiator")
            .services("event-subscribe", "event-unsubscribe")
            .providers("event_handler"),
        new ServiceConsumptionRule()
            .consumer("contract-initiator")
            .services("trusted-contract-negotiation", "trusted-contract-observation")
            .providers("contract-proxy-initiator"),

        new ServiceConsumptionRule()
            .consumer("contract-proxy-initiator")
            .services("contract-negotiation")
            .providers("contract-proxy-reactor"),

        new ServiceConsumptionRule()
            .consumer("contract-proxy-reactor")
            .services("contract-negotiation")
            .providers("contract-proxy-initiator"),

        new ServiceConsumptionRule()
            .consumer("contract-reactor")
            .services("event-subscribe", "event-unsubscribe")
            .providers("event_handler"),
        new ServiceConsumptionRule()
            .consumer("contract-reactor")
            .services("trusted-contract-negotiation", "trusted-contract-observation")
            .providers("contract-proxy-reactor"),
    };
}
