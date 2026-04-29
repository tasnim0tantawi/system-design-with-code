package com.booking.review.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("review_aggregate")
public class ReviewAggregate {
    @Id
    @Column("hotel_id")
    private Integer hotelId;

    @Column("total_reviews")
    private Integer totalReviews;

    @Column("sum_ratings")
    private Long sumRatings;

    @Column("rating_1")
    private Integer rating1;

    @Column("rating_2")
    private Integer rating2;

    @Column("rating_3")
    private Integer rating3;

    @Column("rating_4")
    private Integer rating4;

    @Column("rating_5")
    private Integer rating5;
}
