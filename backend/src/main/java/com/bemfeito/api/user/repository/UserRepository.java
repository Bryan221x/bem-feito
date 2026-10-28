package com.bemfeito.api.user.repository;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.user.entity.User;
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    boolean existsByPlatformRole(com.bemfeito.api.user.entity.PlatformRole role);
}
