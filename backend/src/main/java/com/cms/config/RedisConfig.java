package com.cms.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Secondary PostgreSQL datasource used for:
 * - File content storage (bytea)
 * - Job queue (file processing, embedding, AI, audit)
 * - Upload sessions (chunked uploads)
 * - JWT tokens, blocklist, lockout, 2FA pending
 */
@Configuration
public class RedisConfig {

    @Value("${pg.host:localhost}")
    private String pgHost;

    @Value("${pg.port:5433}")
    private int pgPort;

    @Value("${pg.db:cms_app}")
    private String pgDb;

    @Value("${pg.user:cmsuser}")
    private String pgUser;

    @Value("${pg.password:cmspassword}")
    private String pgPassword;

    @Bean(name = "pgDataSource")
    public DataSource pgDataSource() {
        String url = "jdbc:postgresql://" + pgHost + ":" + pgPort + "/" + pgDb;
        return DataSourceBuilder.create()
                .url(url)
                .username(pgUser)
                .password(pgPassword)
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean(name = "pgJdbcTemplate")
    public JdbcTemplate pgJdbcTemplate() {
        return new JdbcTemplate(pgDataSource());
    }
}
