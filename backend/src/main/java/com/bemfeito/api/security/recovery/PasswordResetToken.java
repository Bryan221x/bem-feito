package com.bemfeito.api.security.recovery;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.bemfeito.api.common.entity.IdentifiedEntity;
import com.bemfeito.api.user.entity.User;
@Entity @Table(name="password_reset_tokens")
public class PasswordResetToken extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) User user;
    @Column(name="token_hash",nullable=false,unique=true,length=64) String tokenHash;
    @Column(name="expires_at",nullable=false) LocalDateTime expiresAt;
    @Column(name="used_at") LocalDateTime usedAt;
    @Column(name="created_at",nullable=false) LocalDateTime createdAt;
}
