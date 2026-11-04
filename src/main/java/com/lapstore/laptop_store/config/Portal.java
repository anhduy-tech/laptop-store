package com.lapstore.laptop_store.config;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum Portal {
    SHOP(8080, "/shop"),
    SALES(8081, "/staff/sales"),
    CONSULT(8082, "/staff/consult"),
    WAREHOUSE(8083, "/staff/warehouse"),
    ADMIN(8084, "/admin");

    private static final List<String> SHARED =
            List.of("/css/", "/js/", "/img/", "/webjars/", "/error", "/ws/",
                    "/login", "/logout", "/actuator");

    private final int port;
    private final String prefix;

    Portal(int port, String prefix) {
        this.port = port;
        this.prefix = prefix;
    }

    public int port() { return port; }

    public String prefix() { return prefix; }

    public boolean owns(String path) {
        if (path.equals("/") || path.equals(prefix) || path.startsWith(prefix + "/")) return true;
        return SHARED.stream().anyMatch(path::startsWith);
    }

    public static Optional<Portal> byPort(int port) {
        return Arrays.stream(values()).filter(p -> p.port == port).findFirst();
    }
}
