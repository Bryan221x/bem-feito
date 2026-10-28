package com.bemfeito.api.need.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.need.category.repository.NeedCategoryRepository;
import com.bemfeito.api.need.dto.NeedPatch;
import com.bemfeito.api.need.dto.NeedRequest;
import com.bemfeito.api.need.dto.NeedResponse;
import com.bemfeito.api.need.entity.Need;
import com.bemfeito.api.need.repository.NeedRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@PreAuthorize("denyAll()")
@Transactional
public class NeedService {
    private final NeedRepository repository;
    private final RequestValidation validation;
    private final UnitRepository owners;
    private final NeedCategoryRepository references;
    public NeedService(NeedRepository repository, RequestValidation validation, UnitRepository owners, NeedCategoryRepository references) {
        this.repository=repository;
        this.validation=validation;
        this.owners=owners;
        this.references=references;
    }
    @PreAuthorize("@institutionAuthorization.canEditUnit(#unitId)")
    public NeedResponse create(Long unitId, NeedRequest request) {
        Need entity=new Need();
        entity.setUnit(owners.findById(unitId).orElseThrow(()->notFound("Unit")));
        apply(entity,request);
        repository.saveAndFlush(entity);
        return NeedResponse.from(entity);
    }
    private Need required(Long unitId, Long id) {
        if (!owners.existsById(unitId)) throw notFound("Unit");
        return repository.findByIdAndUnitId(id,unitId).orElseThrow(()->notFound("Need"));
    }
    @PreAuthorize("@institutionAuthorization.child('need',#unitId,#id)")
    public NeedResponse replace(Long unitId, Long id, NeedRequest request) {
        Need entity=required(unitId,id);
        apply(entity,request);
        repository.flush();
        return NeedResponse.from(entity);
    }
    @PreAuthorize("@institutionAuthorization.child('need',#unitId,#id)")
    public NeedResponse patch(Long unitId, Long id, NeedPatch patch) {
        Need entity=required(unitId,id);
        var merged=new NeedRequest(
            patch.has("categoryId") ? patch.getCategoryId() : entity.getCategory().getId(),
            patch.has("title") ? patch.getTitle() : entity.getTitle(),
            patch.has("description") ? patch.getDescription() : entity.getDescription(),
            patch.has("quantity") ? patch.getQuantity() : entity.getQuantity(),
            patch.has("unitLabel") ? patch.getUnitLabel() : entity.getUnitLabel(),
            patch.has("priority") ? patch.getPriority() : entity.getPriority(),
            patch.has("status") ? patch.getStatus() : entity.getStatus());
        return replace(unitId,id,merged);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.child('need',#unitId,#id)")
    public NeedResponse find(Long unitId, Long id) { return NeedResponse.from(required(unitId,id)); }
    @PreAuthorize("@institutionAuthorization.child('need',#unitId,#id)")
    public void delete(Long unitId, Long id) {
        Need entity=required(unitId,id);
        if (entity.getStatus()!=com.bemfeito.api.common.domain.NeedStatus.INACTIVE)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Inative a necessidade antes de removê-la");
        repository.delete(entity);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.canEditUnit(#unitId)")
    public java.util.List<NeedResponse> list(Long unitId) {
        if(!owners.existsById(unitId)) throw notFound("unit");
        return repository.findForUnits(java.util.List.of(unitId),false).stream().map(NeedResponse::from).toList();
    }
    private void apply(Need entity, NeedRequest request) {
        validation.check(request);
        var reference=references.findById(request.categoryId()).orElseThrow(()->notFound("NeedCategory"));
        require(reference.isActive() || (entity.getCategory()!=null && entity.getCategory().getId().equals(reference.getId())), "Categoria inativa");
        entity.setCategory(reference);
        entity.setTitle(request.title());
        entity.setDescription(request.description());
        entity.setQuantity(request.quantity());
        entity.setUnitLabel(request.unitLabel());
        entity.setPriority(request.priority());
        entity.setStatus(request.status());
    }
}
