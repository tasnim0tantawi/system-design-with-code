package com.booking.search.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public class SearchDtos {

    public record HotelSummary(
            @JsonProperty("id") Integer id,
            @JsonProperty("managerId") Integer managerId,
            @JsonProperty("name") String name,
            @JsonProperty("description") String description,
            @JsonProperty("location") String location,
            @JsonProperty("stars") Integer stars,
            @JsonProperty("type") String type,
            @JsonProperty("status") String status) {}

    public record SearchResultItem(Integer hotelId, String name, String location, Integer stars,
                                   String type, BigDecimal minPrice) {}

    public record SearchResponse(List<SearchResultItem> items, int total) {}

    private SearchDtos() {}
}
