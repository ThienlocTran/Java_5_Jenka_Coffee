package com.springboot.jenka_coffee.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Database configuration and health check
 */
@Slf4j
@Configuration
public class DatabaseConfig {

    private final DataSource dataSource;

    public DatabaseConfig(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Check database connection on application startup
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkDatabaseConnection() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(5)) {
                log.info("✅ Database connection successful!");
                log.info("Database URL: {}", connection.getMetaData().getURL());
                log.info("Database Product: {}", connection.getMetaData().getDatabaseProductName());
                log.info("Database Version: {}", connection.getMetaData().getDatabaseProductVersion());
            } else {
                log.error("❌ Database connection is not valid!");
            }
        } catch (SQLException e) {
            log.error("❌ Failed to connect to database: {}", e.getMessage());
            log.error("Please check your database configuration in application.properties");
            log.error("Stack trace:", e);
        }
    }
}
