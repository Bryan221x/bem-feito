package com.bemfeito.api.need.category.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bemfeito.api.need.category.dto.NeedCategoryResponse;
import com.bemfeito.api.need.category.service.NeedCategoryService;

@RestController
@RequestMapping("/api/need-categories")
public class NeedCategoryController {
    private final NeedCategoryService service;

    public NeedCategoryController(NeedCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<NeedCategoryResponse> findAll() {
        return service.findAll();
    }
}
