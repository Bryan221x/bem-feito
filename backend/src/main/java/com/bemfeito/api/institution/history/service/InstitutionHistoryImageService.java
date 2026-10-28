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
    private final com.bemfeito.api.storage.ImageStorage storage;
    private final com.bemfeito.api.security.InstitutionAuthorizationService authorization;
    public InstitutionHistoryImageService(InstitutionHistoryImageRepository repository, RequestValidation validation, InstitutionRepository owners, com.bemfeito.api.storage.ImageStorage storage, com.bemfeito.api.security.InstitutionAuthorizationService authorization) {
        this.storage=storage;this.authorization=authorization;
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
        String oldUrl=entity.getImageUrl();
        if(oldUrl.startsWith("/api/history-media/") && oldUrl.equals(request.imageUrl())) {
            validation.check(new HistoryImageRequest("https://example.invalid/image.png",request.altText(),request.caption(),request.displayOrder()));
            entity.setAltText(request.altText());entity.setCaption(request.caption());entity.setDisplayOrder(request.displayOrder());
        } else { apply(entity,request); }
        repository.flush();
        if(!oldUrl.equals(entity.getImageUrl())) afterCommitDelete(institutionId,oldUrl);
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
        afterCommitDelete(institutionId,entity.getImageUrl());
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public java.util.List<HistoryImageResponse> list(Long institutionId) {
        if(!owners.existsById(institutionId)) throw notFound("institution");
        return repository.findByInstitutionIdOrderByDisplayOrderAscIdAsc(institutionId).stream().map(HistoryImageResponse::from).toList();
    }
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public HistoryImageResponse upload(Long institutionId,org.springframework.web.multipart.MultipartFile file,String altText,String caption,int displayOrder) {
        validation.check(new HistoryImageRequest("https://example.invalid/image.png",altText,caption,displayOrder));
        var owner=owners.findById(institutionId).orElseThrow(()->notFound("Instituição"));
        String url=storage.store(institutionId,file);
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCompletion(int status) { if(status!=STATUS_COMMITTED) storage.delete(institutionId,url); }
        });
        var entity=new InstitutionHistoryImage();entity.setInstitution(owner);entity.setImageUrl(url);entity.setAltText(altText.trim());entity.setCaption(caption.isBlank()?null:caption.trim());entity.setDisplayOrder(displayOrder);
        repository.saveAndFlush(entity);return HistoryImageResponse.from(entity);
    }
    @PreAuthorize("permitAll()")
    @Transactional(readOnly=true)
    public org.springframework.core.io.Resource media(Long institutionId,String key) {
        var owner=owners.findById(institutionId).orElseThrow(()->notFound("Imagem"));
        if(owner.getStatus()!=com.bemfeito.api.common.domain.InstitutionStatus.ACTIVE && !authorization.canViewInstitution(institutionId)) throw notFound("Imagem");
        if(!repository.existsByInstitutionIdAndImageUrl(institutionId,"/api/history-media/"+institutionId+"/"+key)) throw notFound("Imagem");
        return storage.read(institutionId,key);
    }
    private void afterCommitDelete(Long owner,String url) {
        org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
            @Override public void afterCommit() { storage.delete(owner,url); }
        });
    }
    private void apply(InstitutionHistoryImage entity, HistoryImageRequest request) {
        validation.check(request);
        entity.setImageUrl(request.imageUrl());
        entity.setAltText(request.altText());
        entity.setCaption(request.caption());
        entity.setDisplayOrder(request.displayOrder());
    }
}
