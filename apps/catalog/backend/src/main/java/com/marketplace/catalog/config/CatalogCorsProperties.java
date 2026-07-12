package com.marketplace.catalog.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "catalog.cors")
public record CatalogCorsProperties(List<String> allowedOrigins) {

    public CatalogCorsProperties {
        allowedOrigins = allowedOrigins == null || allowedOrigins.isEmpty()
                ? List.of("http://localhost:5173")
                : List.copyOf(allowedOrigins);
    }
}
