package com.mockavior.app.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "mockavior.admin")
@Setter
public class AdminProperties {

    /**
     * Base path for all administrative endpoints.
     */
    private String prefix = "/__mockavior__";
}
