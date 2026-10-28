package com.bemfeito.api.institution.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.common.dto.PageResponse;
import com.bemfeito.api.institution.contact.repository.InstitutionContactRepository;
import com.bemfeito.api.institution.dto.InstitutionManagementResponse;
import com.bemfeito.api.institution.dto.InstitutionManagementSummary;
import com.bemfeito.api.institution.dto.InstitutionPublicResponse;
import com.bemfeito.api.institution.dto.InstitutionSummary;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.institution.history.dto.HistoryImageResponse;
import com.bemfeito.api.institution.history.repository.InstitutionHistoryImageRepository;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.unit.dto.UnitPublicResponse;
import com.bemfeito.api.unit.service.UnitQueryService;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@PreAuthorize("denyAll()")
@Transactional(readOnly=true)
public class InstitutionQueryService {
    private final InstitutionRepository institutions;
    private final InstitutionContactRepository contacts;
    private final InstitutionHistoryImageRepository images;
    private final UnitQueryService units;
    public InstitutionQueryService(InstitutionRepository institutions, InstitutionContactRepository contacts,
            InstitutionHistoryImageRepository images, UnitQueryService units) {
        this.institutions=institutions;this.contacts=contacts;this.images=images;this.units=units;
    }
    @PreAuthorize("permitAll()")
    public PageResponse<InstitutionSummary> list(Long cityId,int page,int size) {
        require(cityId==null || cityId>0,"cityId deve ser positivo");
        return PageResponse.from(institutions.findPublic(cityId,pagination(page,size)));
    }
    @PreAuthorize("@institutionAuthorization.isAdmin()")
    public PageResponse<InstitutionManagementSummary> managementList(int page,int size) {
        return PageResponse.from(institutions.findManagement(pagination(page,size)));
    }
    private PageRequest pagination(int page,int size) {
        require(page>=0 && size>=1 && size<=100,"page deve ser >= 0 e size deve estar entre 1 e 100");
        return PageRequest.of(page,size);
    }
    private Institution required(Long id) { return institutions.findById(id).orElseThrow(()->notFound("Instituição")); }
    private Institution visible(Long id) {
        Institution entity=required(id);
        if(entity.getStatus()!=InstitutionStatus.ACTIVE) throw notFound("Instituição");
        return entity;
    }
    @PreAuthorize("permitAll()")
    public InstitutionPublicResponse publicDetail(Long id) {
        Institution i=visible(id);
        return new InstitutionPublicResponse(i.getId(),i.getDisplayName(),i.getCategory().getId(),i.getCategory().getName(),
            i.getObjective(),i.getDescription(),i.getHistory(),i.getWebsite(),
            contacts.findForOwners(List.of(id),true).stream().map(ContactResponse::from).toList(),
            images.findByInstitutionIdOrderByDisplayOrderAscIdAsc(id).stream().map(HistoryImageResponse::from).toList(),
            units.publicForInstitution(id));
    }
    @PreAuthorize("permitAll()")
    public List<UnitPublicResponse> publicUnits(Long id) { visible(id); return units.publicForInstitution(id); }
    @PreAuthorize("@institutionAuthorization.canViewInstitution(#id)")
    public InstitutionManagementResponse management(Long id) {
        Institution i=required(id);
        return new InstitutionManagementResponse(i.getId(),i.getDisplayName(),i.getCategory().getId(),i.getCategory().getName(),
            i.getObjective(),i.getDescription(),i.getHistory(),i.getWebsite(),
            contacts.findForOwners(List.of(id),false).stream().map(ContactResponse::from).toList(),
            images.findByInstitutionIdOrderByDisplayOrderAscIdAsc(id).stream().map(HistoryImageResponse::from).toList(),
            units.managementForInstitution(id),i.getLegalName(),i.getCnpj(),i.getStatus(),i.getCreatedAt(),i.getUpdatedAt());
    }
}
