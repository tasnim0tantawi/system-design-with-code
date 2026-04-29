package com.booking.auth.config;

import com.booking.common.security.UserRole;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.PostgresDialect;

import java.util.List;

/**
 * Registers explicit String &lt;-&gt; {@link UserRole} converters so Spring Data
 * R2DBC can map the {@code role} varchar column on {@code auth_user} into the
 * enum on the entity. Without these the framework wouldn't know how to read a
 * varchar back into a {@link UserRole} field.
 */
@Configuration
public class R2dbcConfig {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions() {
        return R2dbcCustomConversions.of(
                PostgresDialect.INSTANCE,
                List.of(new StringToUserRole(), new UserRoleToString()));
    }

    @ReadingConverter
    static class StringToUserRole implements Converter<String, UserRole> {
        @Override
        public UserRole convert(String source) {
            return UserRole.valueOf(source);
        }
    }

    @WritingConverter
    static class UserRoleToString implements Converter<UserRole, String> {
        @Override
        public String convert(UserRole source) {
            return source.name();
        }
    }
}
