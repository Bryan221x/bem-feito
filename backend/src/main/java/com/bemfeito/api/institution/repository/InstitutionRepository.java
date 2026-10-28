package com.bemfeito.api.institution.repository;

import com.bemfeito.api.institution.dto.InstitutionManagementSummary;
import com.bemfeito.api.institution.dto.InstitutionSummary;
import com.bemfeito.api.institution.entity.Institution;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InstitutionRepository extends JpaRepository<Institution,Long> {
    @Override @EntityGraph(attributePaths="category") Optional<Institution> findById(Long id);
    @Query("""
        select new com.bemfeito.api.institution.dto.InstitutionSummary(i.id,i.displayName,i.category.id,i.category.name,substring(i.description,1,500))
        from Institution i where i.status = com.bemfeito.api.common.domain.InstitutionStatus.ACTIVE
        and (:cityId is null or exists (select u.id from Unit u where u.institution=i and u.city.id=:cityId
            and u.status = com.bemfeito.api.common.domain.UnitStatus.ACTIVE))
        order by i.displayName, i.id
        """)
    Page<InstitutionSummary> findPublic(Long cityId, Pageable pageable);
    @Query("""
        select new com.bemfeito.api.institution.dto.InstitutionManagementSummary(i.id,i.displayName,i.status)
        from Institution i order by i.displayName, i.id
        """)
    Page<InstitutionManagementSummary> findManagement(Pageable pageable);
}
