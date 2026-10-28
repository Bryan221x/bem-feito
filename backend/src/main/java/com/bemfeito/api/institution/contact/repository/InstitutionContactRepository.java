package com.bemfeito.api.institution.contact.repository;

import com.bemfeito.api.institution.contact.entity.InstitutionContact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstitutionContactRepository extends JpaRepository<InstitutionContact,Long> {
    @Query("select c from InstitutionContact c where c.institution.id in :ownerIds and (:publicOnly=false or c.isPublic=true) order by c.displayOrder,c.id") List<InstitutionContact> findForOwners(Collection<Long> ownerIds, boolean publicOnly);
    Optional<InstitutionContact> findByIdAndInstitutionId(Long id, Long ownerId);
}
