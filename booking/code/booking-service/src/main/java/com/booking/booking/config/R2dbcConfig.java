package com.booking.booking.config;

import com.booking.booking.entity.BookingStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.List;

/**
 * Registers explicit String &lt;-&gt; {@link BookingStatus} converters so Spring
 * Data R2DBC can map the {@code booking.status} varchar column into the enum
 * on the {@link com.booking.booking.entity.Booking} entity. Without these the
 * framework wouldn't know how to read a varchar back into an enum field.
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                List.of(new StringToBookingStatus(), new BookingStatusToString()));
    }

    @ReadingConverter
    static class StringToBookingStatus implements Converter<String, BookingStatus> {
        @Override
        public BookingStatus convert(String source) {
            return BookingStatus.valueOf(source);
        }
    }

    @WritingConverter
    static class BookingStatusToString implements Converter<BookingStatus, String> {
        @Override
        public String convert(BookingStatus source) {
            return source.name();
        }
    }
}
