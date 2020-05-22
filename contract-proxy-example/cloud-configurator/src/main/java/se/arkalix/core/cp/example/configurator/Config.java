package se.arkalix.core.cp.example.configurator;

import java.net.InetSocketAddress;

public final class Config {
    private Config() {}

    public static final InetSocketAddress AU = new InetSocketAddress("172.23.1.10", 8445);
    public static final InetSocketAddress OR = new InetSocketAddress("172.23.1.11", 8441);
    public static final InetSocketAddress SR = new InetSocketAddress("172.23.1.12", 8443);
}
