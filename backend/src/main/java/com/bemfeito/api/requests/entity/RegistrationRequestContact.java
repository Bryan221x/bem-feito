package com.bemfeito.api.requests.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.bemfeito.api.common.entity.*;
import com.bemfeito.api.common.domain.ContactType;
import com.bemfeito.api.institution.category.entity.InstitutionCategory;
import com.bemfeito.api.location.city.entity.City;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.user.entity.User;

@Entity
@Table(name="registration_request_contacts")
public class RegistrationRequestContact extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registration_request_id",nullable=false)
    private RegistrationRequest request;
    @Enumerated(EnumType.STRING) @Column(name="type",nullable=false,length=30)
    private ContactType type;
    @Column(name="value",nullable=false,length=255)
    private String value;
    @Column(name="label",nullable=true,length=100)
    private String label;
    @Column(name="is_primary",nullable=false)
    private Boolean isPrimary;

    public RegistrationRequestContact() {}
    public RegistrationRequest getRequest() { return request; }
    public void setRequest(RegistrationRequest value) { this.request=value; }
    public ContactType getType() { return type; }
    public void setType(ContactType value) { this.type=value; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value=value; }
    public String getLabel() { return label; }
    public void setLabel(String value) { this.label=value; }
    public Boolean getIsPrimary() { return isPrimary; }
    public void setIsPrimary(Boolean value) { this.isPrimary=value; }
}
