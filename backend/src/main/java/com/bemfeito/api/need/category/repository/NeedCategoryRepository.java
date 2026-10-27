package com.bemfeito.api.need.category.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.need.category.entity.NeedCategory;

public interface NeedCategoryRepository extends JpaRepository<NeedCategory, Long> {
    List<NeedCategory> findByActiveTrueOrderByNameAsc();
}
