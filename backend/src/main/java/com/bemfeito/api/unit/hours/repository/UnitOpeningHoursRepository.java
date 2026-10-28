package com.bemfeito.api.unit.hours.repository;

import com.bemfeito.api.unit.hours.entity.UnitOpeningHours;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnitOpeningHoursRepository extends JpaRepository<UnitOpeningHours,Long> {
    List<UnitOpeningHours> findByUnitIdInOrderByDayOfWeekAsc(Collection<Long> unitIds);
    Optional<UnitOpeningHours> findByUnitIdAndDayOfWeek(Long unitId, Byte dayOfWeek);
}
