package com.bemfeito.api.unit.entity;

import com.bemfeito.api.common.domain.UnitStatus;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.location.city.entity.City;
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
@Table(name="units")
public class Unit extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="institution_id", nullable=false)
    private Institution institution;

    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="city_id", nullable=false)
    private City city;

    @Column(name="name", length=150, nullable=false)
    private String name;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="document", length=14, nullable=true)
    private String document;

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="postal_code", length=8, nullable=false)
    private String postalCode;

    @Column(name="street", length=180, nullable=false)
    private String street;

    @Column(name="number", length=30, nullable=false)
    private String number;

    @Column(name="complement", length=120, nullable=true)
    private String complement;

    @Column(name="neighborhood", length=120, nullable=false)
    private String neighborhood;

    @Column(precision=10, scale=8)
    private BigDecimal latitude;

    @Column(precision=11, scale=8)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING) @Column(name="status", length=30, nullable=false)
    private UnitStatus status;

    public Unit() {}
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution value) { this.institution = value; }
    public City getCity() { return city; }
    public void setCity(City value) { this.city = value; }
    public String getName() { return name; }
    public void setName(String value) { this.name = value; }
    public String getDocument() { return document; }
    public void setDocument(String value) { this.document = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { this.postalCode = value; }
    public String getStreet() { return street; }
    public void setStreet(String value) { this.street = value; }
    public String getNumber() { return number; }
    public void setNumber(String value) { this.number = value; }
    public String getComplement() { return complement; }
    public void setComplement(String value) { this.complement = value; }
    public String getNeighborhood() { return neighborhood; }
    public void setNeighborhood(String value) { this.neighborhood = value; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal value) { this.latitude = value; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal value) { this.longitude = value; }
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus value) { this.status = value; }
}
