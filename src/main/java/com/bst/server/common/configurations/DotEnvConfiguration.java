package com.bst.server.common.configurations;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * Configuration class that loads environment variables from .env file
 * using dotenv-java library.
 *
 * <p>The .env file should be placed at the project root directory.
 * All variables defined in .env are set as system properties so that
 * Spring's ${...} placeholder resolution can pick them up.</p>
 *
 * <p>Note: The primary .env loading happens in ServerApplication.main()
 * before the Spring context starts. This class provides a secondary
 * validation and logging of the loaded environment.</p>
 */
@Slf4j
@Configuration
public class DotEnvConfiguration {

    private final Environment environment;

    public DotEnvConfiguration(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void logActiveProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length > 0) {
            log.info("╔══════════════════════════════════════════════════╗");
            log.info("║  Active Profile(s): {}", String.join(", ", activeProfiles));
            log.info("╚══════════════════════════════════════════════════╝");
        } else {
            log.warn("No active profile set. Falling back to default profile.");
        }
    }

    /**
     * Loads .env file and sets entries as system properties.
     * Called statically from ServerApplication.main() before Spring context boots.
     */
    public static void loadDotEnv() {
        try {
            Dotenv dotenv = Dotenv.configure()
                    .directory("./")
                    .ignoreIfMissing()
                    .load();

            dotenv.entries().forEach(entry ->
                    System.setProperty(entry.getKey(), entry.getValue())
            );

            log.info(".env file loaded successfully. {} entries set as system properties.",
                    dotenv.entries().size());
        } catch (Exception e) {
            log.warn("Could not load .env file: {}. Using system environment variables.", e.getMessage());
        }
    }
}
