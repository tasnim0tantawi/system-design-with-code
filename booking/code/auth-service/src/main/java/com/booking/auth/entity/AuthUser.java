package com.booking.auth.entity;

import com.booking.common.security.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("auth_user")
public class AuthUser {
    @Id
    private Integer id;

    private String email;

    @Column("password_hash")
    private String passwordHash;

    private String name;

    /**
     * Stored as a varchar in the DB; mapped to/from {@link UserRole}
     * via the R2DBC converters registered in {@code R2dbcConfig}.
     */
    private UserRole role;

    @Column("created_at")
    private Instant createdAt;
}
