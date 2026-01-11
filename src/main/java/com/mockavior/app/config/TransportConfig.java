package com.mockavior.app.config;

import com.mockavior.transport.http.HttpTransportAdapter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class TransportConfig {

    @Bean
    public HttpTransportAdapter httpTransportAdapter() {
        log.info("Initializing HttpTransportAdapter");
        return new HttpTransportAdapter();
    }
}
