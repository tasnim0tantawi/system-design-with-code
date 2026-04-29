package com.booking.urlshortener.cassandra;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.cassandra.core.cql.ReactiveCqlOperations;
import org.springframework.stereotype.Component;

/**
 * Creates the {@code click_counts} counter table on startup. Spring Data
 * Cassandra's SchemaAction.CREATE_IF_NOT_EXISTS only handles entity-mapped
 * tables; it can't infer COUNTER columns from a @Table annotation, so we run
 * the DDL ourselves.
 */
@Component
public class ClickCountsSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClickCountsSchemaInitializer.class);

    private static final String DDL =
            "CREATE TABLE IF NOT EXISTS click_counts (" +
            "  short_code text," +
            "  bucket_day text," +
            "  count counter," +
            "  PRIMARY KEY ((short_code), bucket_day)" +
            ")";

    private final ReactiveCqlOperations cql;

    public ClickCountsSchemaInitializer(ReactiveCqlOperations cql) {
        this.cql = cql;
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean ok = cql.execute(DDL).block();
        log.info("click_counts schema ensured (ok={})", ok);
    }
}
