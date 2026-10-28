package com.bemfeito.api.institution.service;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.institution.category.repository.InstitutionCategoryRepository;
import com.bemfeito.api.institution.dto.InstitutionManagementResponse;
import com.bemfeito.api.institution.dto.InstitutionPatch;
import com.bemfeito.api.institution.dto.InstitutionRequest;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@Transactional
public class InstitutionService {
    private final InstitutionRepository repository;
    private final RequestValidation validation;
    private final InstitutionCategoryRepository references;
    private final InstitutionQueryService queries;
    public InstitutionService(InstitutionRepository repository, RequestValidation validation, InstitutionCategoryRepository references, InstitutionQueryService queries) {
        this.repository=repository;
        this.validation=validation;
        this.references=references;
        this.queries=queries;
    }
    public InstitutionManagementResponse create(InstitutionRequest request) {
        Institution entity=new Institution();
        apply(entity,request);
        repository.saveAndFlush(entity);
        return queries.management(entity.getId());
    }
    private Institution required(Long id) {
        return repository.findById(id).orElseThrow(()->notFound("Institution"));
    }
    public InstitutionManagementResponse replace(Long id, InstitutionRequest request) {
        Institution entity=required(id);
        apply(entity,request);
        repository.flush();
        return queries.management(entity.getId());
    }
    public InstitutionManagementResponse patch(Long id, InstitutionPatch patch) {
        Institution entity=required(id);
        var merged=new InstitutionRequest(
            patch.has("categoryId") ? patch.getCategoryId() : entity.getCategory().getId(),
            patch.has("displayName") ? patch.getDisplayName() : entity.getDisplayName(),
            patch.has("legalName") ? patch.getLegalName() : entity.getLegalName(),
            patch.has("cnpj") ? patch.getCnpj() : entity.getCnpj(),
            patch.has("objective") ? patch.getObjective() : entity.getObjective(),
            patch.has("description") ? patch.getDescription() : entity.getDescription(),
            patch.has("history") ? patch.getHistory() : entity.getHistory(),
            patch.has("website") ? patch.getWebsite() : entity.getWebsite(),
            patch.has("status") ? patch.getStatus() : entity.getStatus());
        return replace(id,merged);
    }
    private void apply(Institution entity, InstitutionRequest request) {
        validation.check(request);
        var reference=references.findById(request.categoryId()).orElseThrow(()->notFound("InstitutionCategory"));
        require(reference.isActive() || (entity.getCategory()!=null && entity.getCategory().getId().equals(reference.getId())), "Categoria inativa");
        entity.setCategory(reference);
        entity.setDisplayName(request.displayName());
        entity.setLegalName(request.legalName());
        entity.setCnpj(request.cnpj());
        entity.setObjective(request.objective());
        entity.setDescription(request.description());
        entity.setHistory(request.history());
        entity.setWebsite(request.website());
        entity.setStatus(request.status());
    }
}
