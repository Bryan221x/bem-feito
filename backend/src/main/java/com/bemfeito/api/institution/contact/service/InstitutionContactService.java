package com.bemfeito.api.institution.contact.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.contact.dto.ContactPatch;
import com.bemfeito.api.common.contact.dto.ContactRequest;
import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.institution.contact.entity.InstitutionContact;
import com.bemfeito.api.institution.contact.repository.InstitutionContactRepository;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@PreAuthorize("denyAll()")
@Transactional
public class InstitutionContactService {
    private final InstitutionContactRepository repository;
    private final RequestValidation validation;
    private final InstitutionRepository owners;
    public InstitutionContactService(InstitutionContactRepository repository, RequestValidation validation, InstitutionRepository owners) {
        this.repository=repository;
        this.validation=validation;
        this.owners=owners;
    }
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public ContactResponse create(Long institutionId, ContactRequest request) {
        InstitutionContact entity=new InstitutionContact();
        entity.setInstitution(owners.findById(institutionId).orElseThrow(()->notFound("Institution")));
        apply(entity,request);
        repository.saveAndFlush(entity);
        return ContactResponse.from(entity);
    }
    private InstitutionContact required(Long institutionId, Long id) {
        if (!owners.existsById(institutionId)) throw notFound("Institution");
        return repository.findByIdAndInstitutionId(id,institutionId).orElseThrow(()->notFound("InstitutionContact"));
    }
    @PreAuthorize("@institutionAuthorization.child('institutionContact',#institutionId,#id)")
    public ContactResponse replace(Long institutionId, Long id, ContactRequest request) {
        InstitutionContact entity=required(institutionId,id);
        apply(entity,request);
        repository.flush();
        return ContactResponse.from(entity);
    }
    @PreAuthorize("@institutionAuthorization.child('institutionContact',#institutionId,#id)")
    public ContactResponse patch(Long institutionId, Long id, ContactPatch patch) {
        InstitutionContact entity=required(institutionId,id);
        var merged=new ContactRequest(
            patch.has("type") ? patch.getType() : entity.getType(),
            patch.has("value") ? patch.getValue() : entity.getValue(),
            patch.has("label") ? patch.getLabel() : entity.getLabel(),
            patch.has("isPrimary") ? patch.getIsPrimary() : entity.getIsPrimary(),
            patch.has("isPublic") ? patch.getIsPublic() : entity.getIsPublic(),
            patch.has("displayOrder") ? patch.getDisplayOrder() : entity.getDisplayOrder());
        return replace(institutionId,id,merged);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.child('institutionContact',#institutionId,#id)")
    public ContactResponse find(Long institutionId, Long id) { return ContactResponse.from(required(institutionId,id)); }
    @PreAuthorize("@institutionAuthorization.child('institutionContact',#institutionId,#id)")
    public void delete(Long institutionId, Long id) {
        InstitutionContact entity=required(institutionId,id);
        repository.delete(entity);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.canManageInstitution(#institutionId)")
    public java.util.List<ContactResponse> list(Long institutionId) {
        if(!owners.existsById(institutionId)) throw notFound("institution");
        return repository.findForOwners(java.util.List.of(institutionId),false).stream().map(ContactResponse::from).toList();
    }
    private void apply(InstitutionContact entity, ContactRequest request) {
        validation.check(request);
        if (request.type()==com.bemfeito.api.common.domain.ContactType.EMAIL) {
            validation.check(new EmailValue(request.value()));
        } else {
            require(request.value().matches("\\+?[0-9]{8,15}"),"Telefone deve conter de 8 a 15 dígitos, com + opcional");
        }
        entity.setType(request.type());
        entity.setValue(request.value());
        entity.setLabel(request.label());
        entity.setIsPrimary(request.isPrimary());
        entity.setIsPublic(request.isPublic());
        entity.setDisplayOrder(request.displayOrder());
    }
    private record EmailValue(@jakarta.validation.constraints.Email String value) {}
}
