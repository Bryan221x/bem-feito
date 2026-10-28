package com.bemfeito.api.unit.servicearea.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class UnitServiceAreaId implements Serializable {
    @Column(name="unit_id") private Long unitId;
    @Column(name="service_area_id") private Long serviceAreaId;
    protected UnitServiceAreaId() {}
    public UnitServiceAreaId(Long unitId, Long serviceAreaId) { this.unitId=unitId; this.serviceAreaId=serviceAreaId; }
    @Override public boolean equals(Object other) {
        return other instanceof UnitServiceAreaId id && Objects.equals(unitId,id.unitId) && Objects.equals(serviceAreaId,id.serviceAreaId);
    }
    @Override public int hashCode() { return Objects.hash(unitId,serviceAreaId); }
}
