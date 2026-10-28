package com.bemfeito.api.institution.contact.entity;

import com.bemfeito.api.common.domain.ContactType;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.bemfeito.api.institution.entity.Institution;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="institution_contacts")
public class InstitutionContact extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="institution_id", nullable=false)
    private Institution institution;

    @Enumerated(EnumType.STRING) @Column(name="type", length=30, nullable=false)
    private ContactType type;

    @Column(name="value", length=255, nullable=false)
    private String value;

    @Column(name="label", length=100, nullable=true)
    private String label;

    @Column(name="is_primary",nullable=false)
    private Boolean isPrimary;

    @Column(name="is_public",nullable=false)
    private Boolean isPublic;

    @Column(name="display_order",nullable=false)
    private Integer displayOrder;

    public InstitutionContact() {}
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution value) { this.institution = value; }
    public ContactType getType() { return type; }
    public void setType(ContactType value) { this.type = value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label = value; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean value) { this.isPrimary = value; }
    public Boolean getIsPublic() { return isPublic; }
    public void setIsPublic(Boolean value) { this.isPublic = value; }
    public Integer getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Integer value) { this.displayOrder = value; }
}
