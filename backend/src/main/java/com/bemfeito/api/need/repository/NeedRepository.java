package com.bemfeito.api.need.repository;

import com.bemfeito.api.need.entity.Need;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NeedRepository extends JpaRepository<Need,Long> {
    @EntityGraph(attributePaths="category")
    @Query("""
        select n from Need n where n.unit.id in :unitIds
        and (:publicOnly=false or n.status=com.bemfeito.api.common.domain.NeedStatus.ACTIVE)
        order by case n.priority when com.bemfeito.api.common.domain.NeedPriority.HIGH then 0
            when com.bemfeito.api.common.domain.NeedPriority.MEDIUM then 1 else 2 end, n.title,n.id
        """)
    List<Need> findForUnits(Collection<Long> unitIds, boolean publicOnly);
    @EntityGraph(attributePaths="category") Optional<Need> findByIdAndUnitId(Long id, Long unitId);
}
