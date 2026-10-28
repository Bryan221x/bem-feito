package com.bemfeito.api.unit.contact.repository;

import com.bemfeito.api.unit.contact.entity.UnitContact;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnitContactRepository extends JpaRepository<UnitContact,Long> {
    @Query("select c from UnitContact c where c.unit.id in :ownerIds and (:publicOnly=false or c.isPublic=true) order by c.displayOrder,c.id") List<UnitContact> findForOwners(Collection<Long> ownerIds, boolean publicOnly);
    Optional<UnitContact> findByIdAndUnitId(Long id, Long ownerId);
}
