package com.todoapp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Provides "now" as an injectable bean instead of calling LocalDate.now() directly.
 * Why: the recommendation rules depend on the current date/time. With a Clock bean, a unit test
 * can pass Clock.fixed(...) and check "what happens at 18:00" without waiting until 18:00.
 * The zone comes from app.timezone so "today" means the user's today, not the server's.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:UTC}") String timezone) {
        return Clock.system(ZoneId.of(timezone));
    }
}
