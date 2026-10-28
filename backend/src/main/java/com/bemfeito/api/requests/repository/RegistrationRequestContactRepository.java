package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface RegistrationRequestContactRepository extends JpaRepository<RegistrationRequestContact,Long> {
    java.util.List<RegistrationRequestContact> findByRequestIdOrderById(Long id);
}
