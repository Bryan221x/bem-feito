package com.bemfeito.api.requests.repository;
import com.bemfeito.api.requests.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;
public interface AccessRequestRepository extends JpaRepository<AccessRequest,Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from AccessRequest r where r.id=:id") Optional<AccessRequest> findForReview(Long id);
    boolean existsByInstitutionIdAndRequesterEmailAndStatus(Long institutionId,String email,RequestStatus status);
    org.springframework.data.domain.Page<AccessRequest> findByStatus(RequestStatus status,org.springframework.data.domain.Pageable pageable);
    long countByStatus(RequestStatus status);
}
