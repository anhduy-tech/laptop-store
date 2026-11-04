package com.lapstore.config;

import java.util.List;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class PortConfig {

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> morePorts(
            @Value("${server.port}") int mainPort,
            @Value("${app.ports}") List<Integer> ports) {
        return factory -> ports.stream()
                .filter(p -> p != mainPort)
                .map(p -> {
                    Connector c = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
                    c.setPort(p);
                    return c;
                })
                .forEach(factory::addAdditionalConnectors);
    }
}
