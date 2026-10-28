package com.bemfeito.api.need.entity;

import com.bemfeito.api.common.domain.NeedPriority;
import com.bemfeito.api.common.domain.NeedStatus;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.bemfeito.api.need.category.entity.NeedCategory;
import com.bemfeito.api.unit.entity.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name="needs")
public class Need extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="unit_id", nullable=false)
    private Unit unit;

    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="category_id", nullable=false)
    private NeedCategory category;

    @Column(name="title", length=150, nullable=false)
    private String title;

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @Column(precision=10, scale=2)
    private BigDecimal quantity;

    @Column(name="unit_label", length=50, nullable=true)
    private String unitLabel;

    @Enumerated(EnumType.STRING) @Column(name="priority", length=20, nullable=false)
    private NeedPriority priority;

    @Enumerated(EnumType.STRING) @Column(name="status", length=30, nullable=false)
    private NeedStatus status;

    public Need() {}
    public Unit getUnit() { return unit; }
    public void setUnit(Unit value) { this.unit = value; }
    public NeedCategory getCategory() { return category; }
    public void setCategory(NeedCategory value) { this.category = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { this.title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { this.quantity = value; }
    public String getUnitLabel() { return unitLabel; }
    public void setUnitLabel(String value) { this.unitLabel = value; }
    public NeedPriority getPriority() { return priority; }
    public void setPriority(NeedPriority value) { this.priority = value; }
    public NeedStatus getStatus() { return status; }
    public void setStatus(NeedStatus value) { this.status = value; }
}
