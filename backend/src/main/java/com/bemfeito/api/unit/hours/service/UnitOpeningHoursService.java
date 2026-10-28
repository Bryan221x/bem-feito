package com.bemfeito.api.unit.hours.service;

import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.unit.hours.dto.OpeningHoursRequest;
import com.bemfeito.api.unit.hours.dto.OpeningHoursResponse;
import com.bemfeito.api.unit.hours.entity.UnitOpeningHours;
import com.bemfeito.api.unit.hours.repository.UnitOpeningHoursRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.bemfeito.api.common.validation.RequestValidation.notFound;
import static com.bemfeito.api.common.validation.RequestValidation.require;

@Service
@Transactional
public class UnitOpeningHoursService {
    private final UnitRepository units;
    private final UnitOpeningHoursRepository hours;
    private final RequestValidation validation;

    public UnitOpeningHoursService(UnitRepository units, UnitOpeningHoursRepository hours, RequestValidation validation) {
        this.units=units; this.hours=hours; this.validation=validation;
    }

    public OpeningHoursResponse put(Long unitId, int day, OpeningHoursRequest request) {
        validation.check(request);
        require(day>=1 && day<=7,"dayOfWeek deve estar entre 1 (segunda) e 7 (domingo)");
        var unit=units.findById(unitId).orElseThrow(()->notFound("Unidade"));
        if (request.closed()) {
            require(request.opensAt()==null && request.closesAt()==null,"Dia fechado não possui horários");
        } else {
            require(request.opensAt()!=null && request.closesAt()!=null,"Informe abertura e fechamento");
            require(request.opensAt().isBefore(request.closesAt()),"Abertura deve ser anterior ao fechamento no mesmo dia");
            require(request.opensAt().getNano()==0 && request.closesAt().getNano()==0,"Horários não aceitam frações de segundo");
        }
        var entity=hours.findByUnitIdAndDayOfWeek(unitId,(byte)day).orElseGet(UnitOpeningHours::new);
        entity.setUnit(unit); entity.setDayOfWeek((byte)day); entity.setClosed(request.closed());
        entity.setOpensAt(request.opensAt()); entity.setClosesAt(request.closesAt()); entity.setNote(request.note());
        return OpeningHoursResponse.from(hours.saveAndFlush(entity));
    }

    @Transactional(readOnly=true)
    public List<OpeningHoursResponse> list(Long unitId) {
        if(!units.existsById(unitId)) throw notFound("Unidade");
        return hours.findByUnitIdInOrderByDayOfWeekAsc(List.of(unitId)).stream().map(OpeningHoursResponse::from).toList();
    }
}
