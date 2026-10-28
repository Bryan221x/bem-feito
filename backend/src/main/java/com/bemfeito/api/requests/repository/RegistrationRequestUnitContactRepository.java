package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface RegistrationRequestUnitContactRepository extends JpaRepository<RegistrationRequestUnitContact,Long> {
    java.util.List<RegistrationRequestUnitContact> findByUnitIdOrderById(Long id);
}
