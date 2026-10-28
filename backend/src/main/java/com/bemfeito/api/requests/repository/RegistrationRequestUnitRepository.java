package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface RegistrationRequestUnitRepository extends JpaRepository<RegistrationRequestUnit,Long> {
    java.util.List<RegistrationRequestUnit> findByRequestIdOrderById(Long id);
}
