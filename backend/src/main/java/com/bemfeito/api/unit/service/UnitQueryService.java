package com.bemfeito.api.unit.service;
import org.springframework.security.access.prepost.PreAuthorize;

import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.common.domain.UnitStatus;
import com.bemfeito.api.location.city.dto.CityResponse;
import com.bemfeito.api.need.dto.NeedResponse;
import com.bemfeito.api.need.repository.NeedRepository;
import com.bemfeito.api.servicearea.dto.ServiceAreaResponse;
import com.bemfeito.api.unit.contact.repository.UnitContactRepository;
import com.bemfeito.api.unit.dto.AddressResponse;
import com.bemfeito.api.unit.dto.UnitManagementResponse;
import com.bemfeito.api.unit.dto.UnitPublicResponse;
import com.bemfeito.api.unit.entity.Unit;
import com.bemfeito.api.unit.hours.dto.OpeningHoursResponse;
import com.bemfeito.api.unit.hours.repository.UnitOpeningHoursRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import com.bemfeito.api.unit.servicearea.repository.UnitServiceAreaRepository;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;

@Service
@PreAuthorize("denyAll()")
@Transactional(readOnly=true)
public class UnitQueryService {
    private final UnitRepository units;
    private final UnitContactRepository contacts;
    private final UnitOpeningHoursRepository hours;
    private final UnitServiceAreaRepository areas;
    private final NeedRepository needs;
    public UnitQueryService(UnitRepository units, UnitContactRepository contacts, UnitOpeningHoursRepository hours,
            UnitServiceAreaRepository areas, NeedRepository needs) {
        this.units=units; this.contacts=contacts; this.hours=hours; this.areas=areas; this.needs=needs;
    }
    @PreAuthorize("@institutionAuthorization.canEditUnit(#id)")
    public UnitManagementResponse management(Long id) { return load(List.of(required(id)),false).getFirst(); }
    @PreAuthorize("permitAll()")
    public UnitPublicResponse publicDetail(Long id) {
        Unit unit=required(id);
        if (unit.getStatus()!=UnitStatus.ACTIVE || unit.getInstitution().getStatus()!=InstitutionStatus.ACTIVE) throw notFound("Unidade");
        return toPublic(load(List.of(unit),true).getFirst());
    }
    @PreAuthorize("permitAll()")
    public List<UnitPublicResponse> publicForInstitution(Long id) {
        return load(units.findForInstitution(id,true),true).stream().map(this::toPublic).toList();
    }
    @PreAuthorize("@institutionAuthorization.canViewInstitution(#id)")
    public List<UnitManagementResponse> managementForInstitution(Long id) { return load(units.findForInstitution(id,false),false); }
    @PreAuthorize("permitAll()")
    public List<CityResponse> availableCities() { return units.findAvailableCities().stream().map(CityResponse::from).toList(); }
    private Unit required(Long id) { return units.findById(id).orElseThrow(()->notFound("Unidade")); }

    // Quatro consultas em lote para filhos, independentemente do número de unidades.
    private List<UnitManagementResponse> load(List<Unit> selected, boolean publicOnly) {
        if (selected.isEmpty()) return List.of();
        var ids=selected.stream().map(Unit::getId).toList();
        var contactGroups=contacts.findForOwners(ids,publicOnly).stream().collect(Collectors.groupingBy(c->c.getUnit().getId()));
        var hourGroups=hours.findByUnitIdInOrderByDayOfWeekAsc(ids).stream().collect(Collectors.groupingBy(h->h.getUnit().getId()));
        var areaGroups=areas.findForUnits(ids).stream().collect(Collectors.groupingBy(a->a.getUnit().getId()));
        var needGroups=needs.findForUnits(ids,publicOnly).stream().collect(Collectors.groupingBy(n->n.getUnit().getId()));
        return selected.stream().map(unit->{
            var city=unit.getCity();
            var address=new AddressResponse(city.getId(),city.getName(),city.getState().getUf(),unit.getPostalCode(),
                unit.getStreet(),unit.getNumber(),unit.getComplement(),unit.getNeighborhood());
            return new UnitManagementResponse(unit.getId(),unit.getInstitution().getId(),unit.getName(),unit.getDescription(),address,
                unit.getLatitude(),unit.getLongitude(),
                contactGroups.getOrDefault(unit.getId(),List.of()).stream().map(ContactResponse::from).toList(),
                hourGroups.getOrDefault(unit.getId(),List.of()).stream().map(OpeningHoursResponse::from).toList(),
                areaGroups.getOrDefault(unit.getId(),List.of()).stream().map(a->ServiceAreaResponse.from(a.getServiceArea())).toList(),
                needGroups.getOrDefault(unit.getId(),List.of()).stream().map(NeedResponse::from).toList(),
                unit.getDocument(),unit.getStatus(),unit.getCreatedAt(),unit.getUpdatedAt());
        }).toList();
    }
    private UnitPublicResponse toPublic(UnitManagementResponse unit) {
        return new UnitPublicResponse(unit.id(),unit.institutionId(),unit.name(),unit.description(),unit.address(),unit.latitude(),
            unit.longitude(),unit.contacts(),unit.openingHours(),unit.serviceAreas(),unit.needs());
    }
}
