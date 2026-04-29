package com.booking.urlshortener.repository;

import com.booking.urlshortener.entity.UrlMapping;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface UrlMappingRepository extends ReactiveCrudRepository<UrlMapping, String> {
}
