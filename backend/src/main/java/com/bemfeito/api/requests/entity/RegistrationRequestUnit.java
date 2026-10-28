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
@Table(name="registration_request_units")
public class RegistrationRequestUnit extends IdentifiedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="registration_request_id",nullable=false)
    private RegistrationRequest request;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="city_id",nullable=false)
    private City city;
    @Column(name="name",nullable=false,length=150)
    private String name;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="document",nullable=true,length=14)
    private String document;
    @Column(columnDefinition="TEXT",name="description",nullable=true)
    private String description;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="postal_code",nullable=false,length=8)
    private String postalCode;
    @Column(name="street",nullable=false,length=180)
    private String street;
    @Column(name="number",nullable=false,length=30)
    private String number;
    @Column(name="complement",nullable=true,length=120)
    private String complement;
    @Column(name="neighborhood",nullable=false,length=120)
    private String neighborhood;

    public RegistrationRequestUnit() {}
    public RegistrationRequest getRequest() { return request; }
    public void setRequest(RegistrationRequest value) { this.request=value; }
    public City getCity() { return city; }
    public void setCity(City value) { this.city=value; }
    public String getName() { return name; }
    public void setName(String value) { this.name=value; }
    public String getDocument() { return document; }
    public void setDocument(String value) { this.document=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description=value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { this.postalCode=value; }
    public String getStreet() { return street; }
    public void setStreet(String value) { this.street=value; }
    public String getNumber() { return number; }
    public void setNumber(String value) { this.number=value; }
    public String getComplement() { return complement; }
    public void setComplement(String value) { this.complement=value; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String value) { this.neighborhood=value; }
}
