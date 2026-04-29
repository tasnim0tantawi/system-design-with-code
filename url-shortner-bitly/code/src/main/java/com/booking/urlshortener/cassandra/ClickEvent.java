package com.booking.urlshortener.cassandra;

import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

@Table("click_events")
public class ClickEvent {

    @PrimaryKey
    private ClickEventKey key;

    @Column("user_agent")
    private String userAgent;

    @Column("ip")
    private String ip;

    @Column("referrer")
    private String referrer;

    @Column("country")
    private String country;

    public ClickEvent() {}

    public ClickEventKey getKey() { return key; }
    public void setKey(ClickEventKey key) { this.key = key; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }
    public String getReferrer() { return referrer; }
    public void setReferrer(String referrer) { this.referrer = referrer; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
}
