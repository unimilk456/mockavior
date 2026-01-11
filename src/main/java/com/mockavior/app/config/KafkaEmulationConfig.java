package com.mockavior.app.config;

import com.mockavior.kafka.compiler.KafkaScenarioCompiler;
import com.mockavior.kafka.runtime.ScenarioExecutionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans for Kafka emulation runtime.
 */
@Slf4j
@Configuration
public class KafkaEmulationConfig {

    @Bean
    public ScenarioExecutionRegistry scenarioExecutionRegistry() {
        log.info("Initializing ScenarioExecutionRegistry");
        return new ScenarioExecutionRegistry();
    }

    @Bean
    public KafkaScenarioCompiler kafkaScenarioCompiler() {
        log.info("Initializing KafkaScenarioCompiler");
        return new KafkaScenarioCompiler();
    }

}
