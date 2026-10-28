package com.bemfeito.api.user.entity;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Locale;
@Entity @Table(name="users")
public class User extends TimestampedEntity {
    @Column(nullable=false,length=150) private String name;
    @Column(nullable=false,unique=true,length=255) private String email;
    @JsonIgnore @Column(name="password_hash",length=255) private String passwordHash;
    @Enumerated(EnumType.STRING) @Column(name="platform_role",nullable=false,length=30) private PlatformRole platformRole;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private UserStatus status;
    @Column(name="email_verified_at") private LocalDateTime emailVerifiedAt;
    public User() {}
    public static String normalizeEmail(String email) { return email==null ? "" : email.trim().toLowerCase(Locale.ROOT); }
    @PrePersist @PreUpdate void normalize() { email=normalizeEmail(email); }
    public String getName() { return name; }
    public void setName(String value) { name=value; }
    public String getEmail() { return email; }
    public void setEmail(String value) { email=normalizeEmail(value); }
    @JsonIgnore public boolean hasUsablePasswordHash() {
        return passwordHash!=null && passwordHash.matches("\\$2[aby]\\$(0[4-9]|[12][0-9]|3[01])\\$[./A-Za-z0-9]{53}");
    }
    @JsonIgnore public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String value) { passwordHash=value; }
    public PlatformRole getPlatformRole() { return platformRole; }
    public void setPlatformRole(PlatformRole value) { platformRole=value; }
    public UserStatus getStatus() { return status; }
    public void setStatus(UserStatus value) { status=value; }
    public LocalDateTime getEmailVerifiedAt() { return emailVerifiedAt; }
    public void setEmailVerifiedAt(LocalDateTime value) { emailVerifiedAt=value; }
}
