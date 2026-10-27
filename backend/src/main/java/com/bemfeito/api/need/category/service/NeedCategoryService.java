package com.bemfeito.api.need.category.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.need.category.dto.NeedCategoryResponse;
import com.bemfeito.api.need.category.repository.NeedCategoryRepository;

@Service
public class NeedCategoryService {
    private final NeedCategoryRepository repository;

    public NeedCategoryService(NeedCategoryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<NeedCategoryResponse> findAll() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
            .map(NeedCategoryResponse::from).toList();
    }
}
