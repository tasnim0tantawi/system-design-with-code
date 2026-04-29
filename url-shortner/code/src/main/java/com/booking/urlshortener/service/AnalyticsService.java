package com.booking.urlshortener.service;

import com.booking.urlshortener.dto.Dtos.DailyCount;
import com.booking.urlshortener.dto.Dtos.StatsResponse;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnalyticsService {

    private final ReactiveCassandraTemplate cassandra;

    public AnalyticsService(ReactiveCassandraTemplate cassandra) {
        this.cassandra = cassandra;
    }

    public Mono<StatsResponse> stats(String shortCode) {
        return cassandra.getReactiveCqlOperations()
                .query("SELECT bucket_day, count FROM click_counts WHERE short_code = ?",
                       (row, n) -> new DailyCount(row.getString("bucket_day"), row.getLong("count")),
                       shortCode)
                .collectList()
                .map(daily -> {
                    List<DailyCount> sorted = new ArrayList<>(daily);
                    sorted.sort((a, b) -> a.day().compareTo(b.day()));
                    long total = sorted.stream().mapToLong(DailyCount::count).sum();
                    return new StatsResponse(shortCode, total, sorted);
                });
    }
}
