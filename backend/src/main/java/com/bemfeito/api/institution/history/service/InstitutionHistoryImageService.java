package com.bemfeito.api.institution.history.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.institution.history.dto.HistoryImagePatch;
import com.bemfeito.api.institution.history.dto.HistoryImageRequest;
import com.bemfeito.api.institution.history.dto.HistoryImageResponse;
import com.bemfeito.api.institution.history.entity.InstitutionHistoryImage;
import com.bemfeito.api.institution.history.repository.InstitutionHistoryImageRepository;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;

@Service
@PreAuthorize("denyAll()")
@Transactional
public class InstitutionHistoryImageService {
    private final InstitutionHistoryImageRepository repository;
    private final RequestValidation validation;
    private final InstitutionRepository owners;
    public InstitutionHistoryImageService(InstitutionHistoryImageRepository repository, RequestValidation validation, InstitutionRepository owners) {
        this.repository=repository;
        this.validation=validation;
        this.owners=owners;
    }
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public HistoryImageResponse create(Long institutionId, HistoryImageRequest request) {
        InstitutionHistoryImage entity=new InstitutionHistoryImage();
        entity.setInstitution(owners.findById(institutionId).orElseThrow(()->notFound("Institution")));
        apply(entity,request);
        repository.saveAndFlush(entity);
        return HistoryImageResponse.from(entity);
    }
    private InstitutionHistoryImage required(Long institutionId, Long id) {
        if (!owners.existsById(institutionId)) throw notFound("Institution");
        return repository.findByIdAndInstitutionId(id,institutionId).orElseThrow(()->notFound("InstitutionHistoryImage"));
    }
    @PreAuthorize("@institutionAuthorization.child('image',#institutionId,#id)")
    public HistoryImageResponse replace(Long institutionId, Long id, HistoryImageRequest request) {
        InstitutionHistoryImage entity=required(institutionId,id);
        apply(entity,request);
        repository.flush();
        return HistoryImageResponse.from(entity);
    }
    @PreAuthorize("@institutionAuthorization.child('image',#institutionId,#id)")
    public HistoryImageResponse patch(Long institutionId, Long id, HistoryImagePatch patch) {
        InstitutionHistoryImage entity=required(institutionId,id);
        var merged=new HistoryImageRequest(
            patch.has("imageUrl") ? patch.getImageUrl() : entity.getImageUrl(),
            patch.has("altText") ? patch.getAltText() : entity.getAltText(),
            patch.has("caption") ? patch.getCaption() : entity.getCaption(),
            patch.has("displayOrder") ? patch.getDisplayOrder() : entity.getDisplayOrder());
        return replace(institutionId,id,merged);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.child('image',#institutionId,#id)")
    public HistoryImageResponse find(Long institutionId, Long id) { return HistoryImageResponse.from(required(institutionId,id)); }
    @PreAuthorize("@institutionAuthorization.child('image',#institutionId,#id)")
    public void delete(Long institutionId, Long id) {
        InstitutionHistoryImage entity=required(institutionId,id);
        repository.delete(entity);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public java.util.List<HistoryImageResponse> list(Long institutionId) {
        if(!owners.existsById(institutionId)) throw notFound("institution");
        return repository.findByInstitutionIdOrderByDisplayOrderAscIdAsc(institutionId).stream().map(HistoryImageResponse::from).toList();
    }
    private void apply(InstitutionHistoryImage entity, HistoryImageRequest request) {
        validation.check(request);
        entity.setImageUrl(request.imageUrl());
        entity.setAltText(request.altText());
        entity.setCaption(request.caption());
        entity.setDisplayOrder(request.displayOrder());
    }
}
