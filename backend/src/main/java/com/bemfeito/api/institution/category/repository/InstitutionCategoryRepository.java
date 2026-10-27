package com.bemfeito.api.institution.category.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.bemfeito.api.institution.category.entity.InstitutionCategory;

public interface InstitutionCategoryRepository extends JpaRepository<InstitutionCategory, Long> {
    List<InstitutionCategory> findByActiveTrueOrderByNameAsc();
}
