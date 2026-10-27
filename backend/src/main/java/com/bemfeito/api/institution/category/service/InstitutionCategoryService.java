package com.bemfeito.api.institution.category.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.institution.category.dto.InstitutionCategoryResponse;
import com.bemfeito.api.institution.category.repository.InstitutionCategoryRepository;

@Service
public class InstitutionCategoryService {
    private final InstitutionCategoryRepository repository;

    public InstitutionCategoryService(InstitutionCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InstitutionCategoryResponse> findAll() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
            .map(InstitutionCategoryResponse::from).toList();
    }
}
