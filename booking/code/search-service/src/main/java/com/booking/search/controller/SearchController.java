package com.booking.search.controller;

import com.booking.search.dto.SearchDtos.SearchResponse;
import com.booking.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDate;

@Tag(name = "Search", description = "Hotel search with filters")
@RestController
@RequestMapping("/api/hotels/search")
public class SearchController {

    private final SearchService service;

    public SearchController(SearchService service) {
        this.service = service;
    }

    @Operation(summary = "Search hotels by location, dates, price range, and room type")
    @GetMapping
    public Mono<SearchResponse> search(@RequestParam(required = false) String location,
                                        @RequestParam(name = "check_in", required = false) String checkIn,
                                        @RequestParam(name = "check_out", required = false) String checkOut,
                                        @RequestParam(name = "min_price", required = false) BigDecimal minPrice,
                                        @RequestParam(name = "max_price", required = false) BigDecimal maxPrice,
                                        @RequestParam(name = "room_type", required = false) String roomType) {
        LocalDate ci = checkIn == null ? null : LocalDate.parse(checkIn);
        LocalDate co = checkOut == null ? null : LocalDate.parse(checkOut);
        return service.search(location, ci, co, minPrice, maxPrice, roomType);
    }
}
