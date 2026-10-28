package com.bemfeito.api.requests.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.bemfeito.api.common.entity.*;
import com.bemfeito.api.common.domain.ContactType;
import com.bemfeito.api.institution.category.entity.InstitutionCategory;
import com.bemfeito.api.location.city.entity.City;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.user.entity.User;

@Entity
@Table(name="account_invitation_tokens")
public class AccountInvitationToken extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false)
    private User user;
    @Column(name="token_hash",nullable=false,length=255)
    private String tokenHash;
    @Column(name="expires_at",nullable=false)
    private LocalDateTime expiresAt;
    @Column(name="used_at",nullable=true)
    private LocalDateTime usedAt;
    @org.hibernate.annotations.CreationTimestamp @Column(name="created_at",nullable=false)
    private LocalDateTime createdAt;

    public AccountInvitationToken() {}
    public User getUser() { return user; }
    public void setUser(User value) { this.user=value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { this.tokenHash=value; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime value) { this.expiresAt=value; }
    public LocalDateTime getUsedAt() { return usedAt; }
    public void setUsedAt(LocalDateTime value) { this.usedAt=value; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime value) { this.createdAt=value; }
}
