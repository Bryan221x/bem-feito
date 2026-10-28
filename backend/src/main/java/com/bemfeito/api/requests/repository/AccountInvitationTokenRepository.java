package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface AccountInvitationTokenRepository extends JpaRepository<AccountInvitationToken,Long> {
    Optional<AccountInvitationToken> findByTokenHash(String hash);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from AccountInvitationToken t where t.tokenHash=:hash") Optional<AccountInvitationToken> findForUse(String hash);
    @Modifying @Query("update AccountInvitationToken t set t.usedAt=:now where t.user.id=:userId and t.usedAt is null")
    int invalidateUnused(Long userId,java.time.LocalDateTime now);
}
