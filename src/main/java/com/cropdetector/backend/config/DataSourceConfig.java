package com.cropdetector.backend.config;

import java.net.URI;
import java.net.URISyntaxException;
import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class DataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:}")
    private String rawUrl;

    @Value("${spring.datasource.username:}")
    private String username;

    @Value("${spring.datasource.password:}")
    private String password;

    @Value("${RENDER:}")
    private String renderEnv;

    @Bean
    @Primary
    public DataSource dataSource() {
        String dbUrl = rawUrl != null ? rawUrl.trim() : "";
        String dbUser = username != null ? username.trim() : "";
        String dbPass = password != null ? password : "";

        boolean isCloud = (renderEnv != null && !renderEnv.isBlank())
            || System.getenv("RENDER") != null
            || System.getenv("RENDER_SERVICE_ID") != null
            || System.getenv("PORT") != null;

        // If no URL is provided or if running in cloud (Render) with unconfigured localhost, fall back to in-memory H2
        if (dbUrl.isEmpty() || (isCloud && (dbUrl.contains("localhost") || dbUrl.contains("127.0.0.1")))) {
            log.warn("================================================================================");
            log.warn("No external PostgreSQL database configured. Using in-memory H2 database.");
            log.warn("To persist data in production (e.g. on Render):");
            log.warn("1. Create a PostgreSQL Database in your Render Dashboard");
            log.warn("2. Copy the Internal Database URL and set it as DATABASE_URL in Environment Variables");
            log.warn("================================================================================");

            HikariConfig h2Config = new HikariConfig();
            h2Config.setJdbcUrl("jdbc:h2:mem:crop_disease_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL");
            h2Config.setDriverClassName("org.h2.Driver");
            h2Config.setUsername("sa");
            h2Config.setPassword("");
            return new HikariDataSource(h2Config);
        }

        // Handle Render / Heroku / cloud URI formats:
        // postgres://user:pass@host:port/db or postgresql://user:pass@host:port/db
        if (dbUrl.startsWith("postgres://") || (dbUrl.startsWith("postgresql://") && !dbUrl.startsWith("jdbc:"))) {
            try {
                String uriString = dbUrl.startsWith("postgres://")
                    ? "postgresql://" + dbUrl.substring("postgres://".length())
                    : dbUrl;

                URI uri = new URI(uriString);
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath() != null ? uri.getPath() : "/crop_disease_db";

                dbUrl = "jdbc:postgresql://" + host + ":" + port + path;
                if (uri.getQuery() != null && !uri.getQuery().isBlank()) {
                    dbUrl += "?" + uri.getQuery();
                }

                if (uri.getUserInfo() != null && !uri.getUserInfo().isBlank()) {
                    String[] userInfo = uri.getUserInfo().split(":", 2);
                    dbUser = userInfo[0];
                    if (userInfo.length > 1) {
                        dbPass = userInfo[1];
                    }
                }
                log.info("Successfully converted cloud PostgreSQL URI to JDBC format: host={}, port={}", host, port);
            } catch (URISyntaxException e) {
                log.warn("Failed to parse cloud database URI '{}', using as-is: {}", dbUrl, e.getMessage());
            }
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbUrl);
        config.setUsername(dbUser);
        config.setPassword(dbPass);

        if (dbUrl.startsWith("jdbc:h2:")) {
            config.setDriverClassName("org.h2.Driver");
        } else {
            config.setDriverClassName("org.postgresql.Driver");
            config.setMaximumPoolSize(5);
            config.setMinimumIdle(1);
            config.setIdleTimeout(30000);
            config.setConnectionTimeout(20000);
        }

        return new HikariDataSource(config);
    }
}
