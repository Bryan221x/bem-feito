package com.bemfeito.api.user.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.user.entity.User;
public interface UserRepository extends JpaRepository<User,Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.email=:email")
    Optional<User> findForInvitationByEmail(String email);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where u.id=:id")
    Optional<User> findForInvitation(Long id);
    Optional<User> findByEmail(String email);
    boolean existsByPlatformRole(com.bemfeito.api.user.entity.PlatformRole role);
}
