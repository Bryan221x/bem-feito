package com.bemfeito.api.institution.entity;

import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.bemfeito.api.institution.category.entity.InstitutionCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name="institutions")
public class Institution extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="category_id", nullable=false)
    private InstitutionCategory category;

    @Column(name="display_name", length=150, nullable=false)
    private String displayName;

    @Column(name="legal_name", length=200, nullable=false)
    private String legalName;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="cnpj", length=14, nullable=false)
    private String cnpj;

    @Column(name="objective", columnDefinition="TEXT")
    private String objective;

    @Column(name="description", columnDefinition="TEXT")
    private String description;

    @Column(name="history", columnDefinition="TEXT")
    private String history;

    @Column(name="website", length=255, nullable=true)
    private String website;

    @Enumerated(EnumType.STRING) @Column(name="status", length=30, nullable=false)
    private InstitutionStatus status;

    public Institution() {}
    public InstitutionCategory getCategory() { return category; }
    public void setCategory(InstitutionCategory value) { this.category = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName = value; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String value) { this.legalName = value; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj = value; }
    public String getObjective() { return objective; }
    public void setObjective(String value) { this.objective = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description = value; }
    public String getHistory() { return history; }
    public void setHistory(String value) { this.history = value; }
    public String getWebsite() { return website; }
    public void setWebsite(String value) { this.website = value; }
    public InstitutionStatus getStatus() { return status; }
    public void setStatus(InstitutionStatus value) { this.status = value; }
}
