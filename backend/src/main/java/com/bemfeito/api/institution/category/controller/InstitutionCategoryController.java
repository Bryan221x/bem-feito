package com.bemfeito.api.institution.category.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bemfeito.api.institution.category.dto.InstitutionCategoryResponse;
import com.bemfeito.api.institution.category.service.InstitutionCategoryService;

@RestController
@RequestMapping("/api/institution-categories")
public class InstitutionCategoryController {
    private final InstitutionCategoryService service;

    public InstitutionCategoryController(InstitutionCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<InstitutionCategoryResponse> findAll() {
        return service.findAll();
    }
}
