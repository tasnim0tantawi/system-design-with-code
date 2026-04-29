package com.booking.urlshortener.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * Spring Boot 4 with pure R2DBC has no JDBC DataSource bean, so the default
 * LiquibaseAutoConfiguration never fires. We build a SpringLiquibase manually
 * from spring.liquibase.url + user + password.
 */
@Configuration
@ConditionalOnProperty(prefix = "spring.liquibase", name = "url")
public class LiquibaseConfig {

    @Value("${spring.liquibase.url}")
    private String url;

    @Value("${spring.liquibase.user}")
    private String user;

    @Value("${spring.liquibase.password}")
    private String password;

    @Value("${spring.liquibase.change-log:classpath:db/changelog/db.changelog-master.yaml}")
    private String changeLog;

    @Bean
    public SpringLiquibase liquibase() {
        DriverManagerDataSource ds = new DriverManagerDataSource();
        ds.setUrl(url);
        ds.setUsername(user);
        ds.setPassword(password);

        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(ds);
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }
}
