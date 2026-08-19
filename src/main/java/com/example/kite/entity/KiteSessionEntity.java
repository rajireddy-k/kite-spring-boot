package com.example.kite.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "kite_session")
public class KiteSessionEntity {
    @Id
    private Long id = 1L;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, length = 2048)
    private String accessToken;

    @Column(length = 2048)
    private String publicToken;

    private OffsetDateTime loginTime;
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getPublicToken() { return publicToken; }
    public void setPublicToken(String publicToken) { this.publicToken = publicToken; }
    public OffsetDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(OffsetDateTime loginTime) { this.loginTime = loginTime; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
