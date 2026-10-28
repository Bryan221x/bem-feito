package com.bemfeito.api.unit.contact.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.contact.dto.ContactPatch;
import com.bemfeito.api.common.contact.dto.ContactRequest;
import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.unit.contact.entity.UnitContact;
import com.bemfeito.api.unit.contact.repository.UnitContactRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@PreAuthorize("denyAll()")
@Transactional
public class UnitContactService {
    private final UnitContactRepository repository;
    private final RequestValidation validation;
    private final UnitRepository owners;
    public UnitContactService(UnitContactRepository repository, RequestValidation validation, UnitRepository owners) {
        this.repository=repository;
        this.validation=validation;
        this.owners=owners;
    }
    @PreAuthorize("@institutionAuthorization.canEditUnit(#unitId)")
    public ContactResponse create(Long unitId, ContactRequest request) {
        UnitContact entity=new UnitContact();
        entity.setUnit(owners.findById(unitId).orElseThrow(()->notFound("Unit")));
        apply(entity,request);
        repository.saveAndFlush(entity);
        return ContactResponse.from(entity);
    }
    private UnitContact required(Long unitId, Long id) {
        if (!owners.existsById(unitId)) throw notFound("Unit");
        return repository.findByIdAndUnitId(id,unitId).orElseThrow(()->notFound("UnitContact"));
    }
    @PreAuthorize("@institutionAuthorization.child('unitContact',#unitId,#id)")
    public ContactResponse replace(Long unitId, Long id, ContactRequest request) {
        UnitContact entity=required(unitId,id);
        apply(entity,request);
        repository.flush();
        return ContactResponse.from(entity);
    }
    @PreAuthorize("@institutionAuthorization.child('unitContact',#unitId,#id)")
    public ContactResponse patch(Long unitId, Long id, ContactPatch patch) {
        UnitContact entity=required(unitId,id);
        var merged=new ContactRequest(
            patch.has("type") ? patch.getType() : entity.getType(),
            patch.has("value") ? patch.getValue() : entity.getValue(),
            patch.has("label") ? patch.getLabel() : entity.getLabel(),
            patch.has("isPrimary") ? patch.getIsPrimary() : entity.getIsPrimary(),
            patch.has("isPublic") ? patch.getIsPublic() : entity.getIsPublic(),
            patch.has("displayOrder") ? patch.getDisplayOrder() : entity.getDisplayOrder());
        return replace(unitId,id,merged);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.child('unitContact',#unitId,#id)")
    public ContactResponse find(Long unitId, Long id) { return ContactResponse.from(required(unitId,id)); }
    @PreAuthorize("@institutionAuthorization.child('unitContact',#unitId,#id)")
    public void delete(Long unitId, Long id) {
        UnitContact entity=required(unitId,id);
        repository.delete(entity);
    }
    @Transactional(readOnly=true)
    @PreAuthorize("@institutionAuthorization.canEditUnit(#unitId)")
    public java.util.List<ContactResponse> list(Long unitId) {
        if(!owners.existsById(unitId)) throw notFound("unit");
        return repository.findForOwners(java.util.List.of(unitId),false).stream().map(ContactResponse::from).toList();
    }
    private void apply(UnitContact entity, ContactRequest request) {
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
