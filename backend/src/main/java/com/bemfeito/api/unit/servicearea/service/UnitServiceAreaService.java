package com.bemfeito.api.unit.servicearea.service;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.servicearea.dto.ServiceAreaResponse;
import com.bemfeito.api.servicearea.repository.ServiceAreaRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import com.bemfeito.api.unit.servicearea.dto.ServiceAreasRequest;
import com.bemfeito.api.unit.servicearea.entity.UnitServiceArea;
import com.bemfeito.api.unit.servicearea.repository.UnitServiceAreaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@Transactional
public class UnitServiceAreaService {
    private final UnitRepository units;
    private final ServiceAreaRepository areas;
    private final UnitServiceAreaRepository links;
    private final RequestValidation validation;

    public UnitServiceAreaService(UnitRepository units, ServiceAreaRepository areas,
            UnitServiceAreaRepository links, RequestValidation validation) {
        this.units=units; this.areas=areas; this.links=links; this.validation=validation;
    }

    public List<ServiceAreaResponse> replace(Long unitId, ServiceAreasRequest request) {
        validation.check(request);
        var unit=units.findById(unitId).orElseThrow(()->notFound("Unidade"));
        var selected=areas.findAllById(request.serviceAreaIds());
        if(selected.size()!=request.serviceAreaIds().size()) throw notFound("Área de atuação");
        var current=links.findForUnits(List.of(unitId));
        var existing=current.stream().map(a->a.getServiceArea().getId()).toList();
        require(selected.stream().allMatch(a->a.isActive() || existing.contains(a.getId())),"Não é possível associar nova área inativa");
        links.deleteAll(current.stream().filter(a->!request.serviceAreaIds().contains(a.getServiceArea().getId())).toList());
        links.saveAllAndFlush(selected.stream().filter(a->!existing.contains(a.getId())).map(a->new UnitServiceArea(unit,a)).toList());
        return list(unitId);
    }

    @Transactional(readOnly=true)
    public List<ServiceAreaResponse> list(Long unitId) {
        if(!units.existsById(unitId)) throw notFound("Unidade");
        return links.findForUnits(List.of(unitId)).stream().map(a->ServiceAreaResponse.from(a.getServiceArea())).toList();
    }
}
