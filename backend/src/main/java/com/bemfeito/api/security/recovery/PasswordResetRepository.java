package com.bemfeito.api.security.recovery;
import java.util.Optional;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.*;
public interface PasswordResetRepository extends JpaRepository<PasswordResetToken,Long> {
    Optional<PasswordResetToken> findByTokenHash(String hash);
    boolean existsByUserIdAndCreatedAtAfter(Long userId,LocalDateTime since);
    @Modifying @Query("update PasswordResetToken t set t.usedAt=:now where t.user.id=:id and t.usedAt is null")
    void invalidate(Long id,LocalDateTime now);
}
