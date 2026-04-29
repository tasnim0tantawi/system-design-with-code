package com.booking.urlshortener.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.domain.Persistable;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;

/**
 * Implements {@link Persistable} so Spring Data R2DBC knows new rows must be
 * INSERTed instead of UPDATEd. With a manually-assigned String PK there's no
 * way for the framework to infer "new vs existing"; without this, save() does
 * a no-op UPDATE that affects 0 rows.
 */
@Table("url_mapping")
public class UrlMapping implements Persistable<String> {

    @Id
    @Column("short_code")
    private String shortCode;

    @Column("long_url")
    private String longUrl;

    @Column("created_at")
    private Instant createdAt;

    @Column("expires_at")
    private Instant expiresAt;

    @Column("creator_ip")
    private String creatorIp;

    @Transient
    private boolean isNew = true;

    public UrlMapping() {}

    @Override
    public String getId() { return shortCode; }

    @Override
    public boolean isNew() { return isNew; }

    /** Mark as loaded-from-DB so a subsequent save() is treated as UPDATE. */
    public UrlMapping markNotNew() { this.isNew = false; return this; }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }
    public String getLongUrl() { return longUrl; }
    public void setLongUrl(String longUrl) { this.longUrl = longUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public String getCreatorIp() { return creatorIp; }
    public void setCreatorIp(String creatorIp) { this.creatorIp = creatorIp; }
}
