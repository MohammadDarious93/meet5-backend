package com.meet5.socialnetwork.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(AppConfig.FraudProperties.class)
public class AppConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @ConfigurationProperties(prefix = "app.fraud")
    public record FraudProperties(
            int threshold,
            int windowMinutes,
            boolean enableSelfVisit
    ) {
    }
}
