package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest,Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegistrationRequest r where r.id=:id") Optional<RegistrationRequest> findForReview(Long id);
    boolean existsByCnpjAndStatusIn(String cnpj, java.util.Collection<RequestStatus> statuses);
    org.springframework.data.domain.Page<RegistrationRequest> findByStatus(RequestStatus status, org.springframework.data.domain.Pageable pageable);
    long countByStatus(RequestStatus status);
}
