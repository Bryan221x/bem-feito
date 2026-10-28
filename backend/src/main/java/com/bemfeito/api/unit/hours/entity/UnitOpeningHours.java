package com.bemfeito.api.unit.hours.entity;

import com.bemfeito.api.common.entity.IdentifiedEntity;
import com.bemfeito.api.unit.entity.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;

@Entity
@Table(name="unit_opening_hours")
public class UnitOpeningHours extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="unit_id", nullable=false)
    private Unit unit;

    @Column(name="day_of_week",nullable=false)
    private Byte dayOfWeek;

    @Column(name="opens_at")
    private LocalTime opensAt;

    @Column(name="closes_at")
    private LocalTime closesAt;

    @Column(nullable=false)
    private Boolean closed;

    @Column(name="note", length=150, nullable=true)
    private String note;

    public UnitOpeningHours() {}
    public Unit getUnit() { return unit; }
    public void setUnit(Unit value) { this.unit = value; }
    public Byte getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(Byte value) { this.dayOfWeek = value; }
    public LocalTime getOpensAt() { return opensAt; }
    public void setOpensAt(LocalTime value) { this.opensAt = value; }
    public LocalTime getClosesAt() { return closesAt; }
    public void setClosesAt(LocalTime value) { this.closesAt = value; }
    public Boolean getClosed() { return closed; }
    public void setClosed(Boolean value) { this.closed = value; }
    public String getNote() { return note; }
    public void setNote(String value) { this.note = value; }
}
