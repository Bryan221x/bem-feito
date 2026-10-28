package com.bemfeito.api.institution.dto;

import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.common.dto.PatchRequest;

public class InstitutionPatch extends PatchRequest {
    private Long categoryId;
    private String displayName;
    private String legalName;
    private String cnpj;
    private String objective;
    private String description;
    private String history;
    private String website;
    private InstitutionStatus status;
    @com.fasterxml.jackson.annotation.JsonSetter("categoryId")
    public void setCategoryId(Long value) { mark("categoryId"); this.categoryId=value; }
    public Long getCategoryId() { return categoryId; }
    @com.fasterxml.jackson.annotation.JsonSetter("displayName")
    public void setDisplayName(String value) { mark("displayName"); this.displayName=value; }
    public String getDisplayName() { return displayName; }
    @com.fasterxml.jackson.annotation.JsonSetter("legalName")
    public void setLegalName(String value) { mark("legalName"); this.legalName=value; }
    public String getLegalName() { return legalName; }
    @com.fasterxml.jackson.annotation.JsonSetter("cnpj")
    public void setCnpj(String value) { mark("cnpj"); this.cnpj=value; }
    public String getCnpj() { return cnpj; }
    @com.fasterxml.jackson.annotation.JsonSetter("objective")
    public void setObjective(String value) { mark("objective"); this.objective=value; }
    public String getObjective() { return objective; }
    @com.fasterxml.jackson.annotation.JsonSetter("description")
    public void setDescription(String value) { mark("description"); this.description=value; }
    public String getDescription() { return description; }
    @com.fasterxml.jackson.annotation.JsonSetter("history")
    public void setHistory(String value) { mark("history"); this.history=value; }
    public String getHistory() { return history; }
    @com.fasterxml.jackson.annotation.JsonSetter("website")
    public void setWebsite(String value) { mark("website"); this.website=value; }
    public String getWebsite() { return website; }
    @com.fasterxml.jackson.annotation.JsonSetter("status")
    public void setStatus(InstitutionStatus value) { mark("status"); this.status=value; }
    public InstitutionStatus getStatus() { return status; }
}
