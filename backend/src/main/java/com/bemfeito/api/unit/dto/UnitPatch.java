package com.bemfeito.api.unit.dto;

import com.bemfeito.api.common.domain.UnitStatus;
import com.bemfeito.api.common.dto.PatchRequest;
import java.math.BigDecimal;

public class UnitPatch extends PatchRequest {
    private Long cityId;
    private String name;
    private String document;
    private String description;
    private String postalCode;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private UnitStatus status;
    @com.fasterxml.jackson.annotation.JsonSetter("cityId")
    public void setCityId(Long value) { mark("cityId"); this.cityId=value; }
    public Long getCityId() { return cityId; }
    @com.fasterxml.jackson.annotation.JsonSetter("name")
    public void setName(String value) { mark("name"); this.name=value; }
    public String getName() { return name; }
    @com.fasterxml.jackson.annotation.JsonSetter("document")
    public void setDocument(String value) { mark("document"); this.document=value; }
    public String getDocument() { return document; }
    @com.fasterxml.jackson.annotation.JsonSetter("description")
    public void setDescription(String value) { mark("description"); this.description=value; }
    public String getDescription() { return description; }
    @com.fasterxml.jackson.annotation.JsonSetter("postalCode")
    public void setPostalCode(String value) { mark("postalCode"); this.postalCode=value; }
    public String getPostalCode() { return postalCode; }
    @com.fasterxml.jackson.annotation.JsonSetter("street")
    public void setStreet(String value) { mark("street"); this.street=value; }
    public String getStreet() { return street; }
    @com.fasterxml.jackson.annotation.JsonSetter("number")
    public void setNumber(String value) { mark("number"); this.number=value; }
    public String getNumber() { return number; }
    @com.fasterxml.jackson.annotation.JsonSetter("complement")
    public void setComplement(String value) { mark("complement"); this.complement=value; }
    public String getComplement() { return complement; }
    @com.fasterxml.jackson.annotation.JsonSetter("neighborhood")
    public void setNeighborhood(String value) { mark("neighborhood"); this.neighborhood=value; }
    public String getNeighborhood() { return neighborhood; }
    @com.fasterxml.jackson.annotation.JsonSetter("latitude")
    public void setLatitude(BigDecimal value) { mark("latitude"); this.latitude=value; }
    public BigDecimal getLatitude() { return latitude; }
    @com.fasterxml.jackson.annotation.JsonSetter("longitude")
    public void setLongitude(BigDecimal value) { mark("longitude"); this.longitude=value; }
    public BigDecimal getLongitude() { return longitude; }
    @com.fasterxml.jackson.annotation.JsonSetter("status")
    public void setStatus(UnitStatus value) { mark("status"); this.status=value; }
    public UnitStatus getStatus() { return status; }
}
