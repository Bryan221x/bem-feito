package com.bemfeito.api.unit.servicearea.repository;

import com.bemfeito.api.unit.servicearea.entity.UnitServiceArea;
import com.bemfeito.api.unit.servicearea.entity.UnitServiceAreaId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UnitServiceAreaRepository extends JpaRepository<UnitServiceArea,UnitServiceAreaId> {
    @EntityGraph(attributePaths="serviceArea")
    @Query("select a from UnitServiceArea a where a.unit.id in :unitIds order by a.serviceArea.name,a.serviceArea.id")
    List<UnitServiceArea> findForUnits(Collection<Long> unitIds);
}
