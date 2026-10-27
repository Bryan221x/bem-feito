package com.bemfeito.api.servicearea.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.servicearea.entity.ServiceArea;

public interface ServiceAreaRepository extends JpaRepository<ServiceArea, Long> {
    List<ServiceArea> findByActiveTrueOrderByNameAsc();
}
