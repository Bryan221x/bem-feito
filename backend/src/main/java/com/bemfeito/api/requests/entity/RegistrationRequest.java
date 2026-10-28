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
@Table(name="registration_requests")
public class RegistrationRequest extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="category_id",nullable=false)
    private InstitutionCategory category;
    @Column(name="display_name",nullable=false,length=150)
    private String displayName;
    @Column(name="legal_name",nullable=false,length=200)
    private String legalName;
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR) @Column(name="cnpj",nullable=false,length=14)
    private String cnpj;
    @Column(columnDefinition="TEXT",name="objective",nullable=true)
    private String objective;
    @Column(columnDefinition="TEXT",name="description",nullable=true)
    private String description;
    @Column(columnDefinition="TEXT",name="history",nullable=true)
    private String history;
    @Column(name="website",nullable=true,length=255)
    private String website;
    @Column(name="responsible_name",nullable=false,length=150)
    private String responsibleName;
    @Column(name="responsible_email",nullable=false,length=255)
    private String responsibleEmail;
    @Column(name="responsible_phone",nullable=false,length=30)
    private String responsiblePhone;
    @Column(name="responsible_role",nullable=false,length=100)
    private String responsibleRole;
    @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=30)
    private RequestStatus status;
    @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(name="reviewed_by_user_id",nullable=true)
    private User reviewedByUser;
    @Column(name="reviewed_at",nullable=true)
    private LocalDateTime reviewedAt;
    @Column(columnDefinition="TEXT",name="review_notes",nullable=true)
    private String reviewNotes;

    public RegistrationRequest() {}
    public InstitutionCategory getCategory() { return category; }
    public void setCategory(InstitutionCategory value) { this.category=value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { this.displayName=value; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String value) { this.legalName=value; }
    public String getCnpj() { return cnpj; }
    public void setCnpj(String value) { this.cnpj=value; }
    public String getObjective() { return objective; }
    public void setObjective(String value) { this.objective=value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { this.description=value; }
    public String getHistory() { return history; }
    public void setHistory(String value) { this.history=value; }
    public String getWebsite() { return website; }
    public void setWebsite(String value) { this.website=value; }
    public String getResponsibleName() { return responsibleName; }
    public void setResponsibleName(String value) { this.responsibleName=value; }
    public String getResponsibleEmail() { return responsibleEmail; }
    public void setResponsibleEmail(String value) { this.responsibleEmail=value; }
    public String getResponsiblePhone() { return responsiblePhone; }
    public void setResponsiblePhone(String value) { this.responsiblePhone=value; }
    public String getResponsibleRole() { return responsibleRole; }
    public void setResponsibleRole(String value) { this.responsibleRole=value; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus value) { this.status=value; }
    public User getReviewedByUser() { return reviewedByUser; }
    public void setReviewedByUser(User value) { this.reviewedByUser=value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { this.reviewedAt=value; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String value) { this.reviewNotes=value; }
}
