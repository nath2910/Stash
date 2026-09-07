package backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Enables scheduled jobs whose individual feature switch is active.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
