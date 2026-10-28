package com.bemfeito.api.unit.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.location.city.repository.CityRepository;
import com.bemfeito.api.unit.dto.UnitManagementResponse;
import com.bemfeito.api.unit.dto.UnitPatch;
import com.bemfeito.api.unit.dto.UnitRequest;
import com.bemfeito.api.unit.entity.Unit;
import com.bemfeito.api.unit.repository.UnitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@PreAuthorize("denyAll()")
@Transactional
public class UnitService {
    private final UnitRepository repository;
    private final RequestValidation validation;
    private final InstitutionRepository owners;
    private final CityRepository references;
    private final UnitQueryService queries;
    public UnitService(UnitRepository repository, RequestValidation validation, InstitutionRepository owners, CityRepository references, UnitQueryService queries) {
        this.repository=repository;
        this.validation=validation;
        this.owners=owners;
        this.references=references;
        this.queries=queries;
    }
    @PreAuthorize("@institutionAuthorization.createUnit(#institutionId,#request)")
    public UnitManagementResponse create(Long institutionId, UnitRequest request) {
        Unit entity=new Unit();
        entity.setInstitution(owners.findById(institutionId).orElseThrow(()->notFound("Institution")));
        apply(entity,request);
        repository.saveAndFlush(entity);
        return queries.management(entity.getId());
    }
    private Unit required(Long id) {
        return repository.findById(id).orElseThrow(()->notFound("Unit"));
    }
    @PreAuthorize("@institutionAuthorization.unitReplace(#id,#request)")
    public UnitManagementResponse replace(Long id, UnitRequest request) {
        Unit entity=required(id);
        apply(entity,request);
        repository.flush();
        return queries.management(entity.getId());
    }
    @PreAuthorize("@institutionAuthorization.unitPatch(#id,#patch)")
    public UnitManagementResponse patch(Long id, UnitPatch patch) {
        Unit entity=required(id);
        var merged=new UnitRequest(
            patch.has("cityId") ? patch.getCityId() : entity.getCity().getId(),
            patch.has("name") ? patch.getName() : entity.getName(),
            patch.has("document") ? patch.getDocument() : entity.getDocument(),
            patch.has("description") ? patch.getDescription() : entity.getDescription(),
            patch.has("postalCode") ? patch.getPostalCode() : entity.getPostalCode(),
            patch.has("street") ? patch.getStreet() : entity.getStreet(),
            patch.has("number") ? patch.getNumber() : entity.getNumber(),
            patch.has("complement") ? patch.getComplement() : entity.getComplement(),
            patch.has("neighborhood") ? patch.getNeighborhood() : entity.getNeighborhood(),
            patch.has("latitude") ? patch.getLatitude() : entity.getLatitude(),
            patch.has("longitude") ? patch.getLongitude() : entity.getLongitude(),
            patch.has("status") ? patch.getStatus() : entity.getStatus());
        return replace(id,merged);
    }
    private void apply(Unit entity, UnitRequest request) {
        validation.check(request);
        var reference=references.findById(request.cityId()).orElseThrow(()->notFound("City"));
        entity.setCity(reference);
        require((request.latitude()==null)==(request.longitude()==null),"Latitude e longitude devem ser informadas juntas");
        entity.setName(request.name());
        entity.setDocument(request.document());
        entity.setDescription(request.description());
        entity.setPostalCode(request.postalCode());
        entity.setStreet(request.street());
        entity.setNumber(request.number());
        entity.setComplement(request.complement());
        entity.setNeighborhood(request.neighborhood());
        entity.setLatitude(request.latitude());
        entity.setLongitude(request.longitude());
        entity.setStatus(request.status());
    }
}
