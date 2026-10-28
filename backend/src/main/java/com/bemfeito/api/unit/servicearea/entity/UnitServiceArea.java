package com.bemfeito.api.unit.servicearea.entity;

import com.bemfeito.api.servicearea.entity.ServiceArea;
import com.bemfeito.api.unit.entity.Unit;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity @Table(name="unit_service_areas")
public class UnitServiceArea {
    @EmbeddedId private UnitServiceAreaId id;
    @MapsId("unitId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="unit_id") private Unit unit;
    @MapsId("serviceAreaId") @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="service_area_id") private ServiceArea serviceArea;
    protected UnitServiceArea() {}
    public UnitServiceArea(Unit unit, ServiceArea area) { this.unit=unit; this.serviceArea=area; this.id=new UnitServiceAreaId(unit.getId(),area.getId()); }
    public Unit getUnit() { return unit; }
    public ServiceArea getServiceArea() { return serviceArea; }
}
