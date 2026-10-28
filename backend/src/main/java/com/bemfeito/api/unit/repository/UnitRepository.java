package com.bemfeito.api.unit.repository;

import com.bemfeito.api.location.city.entity.City;
import com.bemfeito.api.unit.entity.Unit;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnitRepository extends JpaRepository<Unit,Long> {
    @Override @EntityGraph(attributePaths={"institution","city.state"}) Optional<Unit> findById(Long id);
    @EntityGraph(attributePaths={"city.state"})
    @Query("""
        select u from Unit u where u.institution.id=:institutionId
        and (:publicOnly=false or (u.status=com.bemfeito.api.common.domain.UnitStatus.ACTIVE
            and u.institution.status=com.bemfeito.api.common.domain.InstitutionStatus.ACTIVE))
        order by u.name,u.id
        """)
    List<Unit> findForInstitution(Long institutionId, boolean publicOnly);
    @Query("""
        select c from City c join fetch c.state where exists (select u.id from Unit u where u.city=c
            and u.status=com.bemfeito.api.common.domain.UnitStatus.ACTIVE
            and u.institution.status=com.bemfeito.api.common.domain.InstitutionStatus.ACTIVE)
        order by c.name,c.state.uf
        """)
    List<City> findAvailableCities();
}
