package com.booking.urlshortener.controller;

import com.booking.urlshortener.service.ClickEventProducer;
import com.booking.urlshortener.service.ShortenerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Public 302 redirect endpoint. Mounted at the root so {@code /abc123} works.
 */
@Tag(name = "Redirect", description = "Public redirect from short code to original URL")
@RestController
public class RedirectController {

    private final ShortenerService service;
    private final ClickEventProducer producer;

    public RedirectController(ShortenerService service, ClickEventProducer producer) {
        this.service = service;
        this.producer = producer;
    }

    @Operation(summary = "Redirect to the original URL and emit a click event")
    @GetMapping("/{shortCode:[A-Za-z0-9]{1,10}}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortCode, ServerHttpRequest req) {
        return service.resolve(shortCode)
                .map(longUrl -> {
                    String userAgent = req.getHeaders().getFirst(HttpHeaders.USER_AGENT);
                    String referrer = req.getHeaders().getFirst(HttpHeaders.REFERER);
                    String ip = req.getRemoteAddress() == null ? null
                            : req.getRemoteAddress().getAddress().getHostAddress();
                    // Fire-and-forget the click event; the redirect must not depend on Kafka health.
                    producer.publish(shortCode, userAgent, ip, referrer).subscribe();
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(longUrl))
                            .<Void>build();
                })
                .defaultIfEmpty(ResponseEntity.<Void>notFound().build());
    }
}
