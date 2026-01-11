package com.mockavior.app;

import com.mockavior.app.config.AdminProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@Slf4j
@SpringBootApplication
@ComponentScan("com.mockavior")
@EnableConfigurationProperties(AdminProperties.class)
public class MockaviorApplication {

    public static void main(String[] args) {
        log.info("Starting Mockavior application");
        SpringApplication.run(MockaviorApplication.class, args);
    }
}
