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
@Table(name="access_requests")
public class AccessRequest extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="institution_id",nullable=false)
    private Institution institution;
    @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(name="requester_user_id",nullable=true)
    private User requesterUser;
    @Column(name="requester_name",nullable=false,length=150)
    private String requesterName;
    @Column(name="requester_email",nullable=false,length=255)
    private String requesterEmail;
    @Column(name="requester_phone",nullable=false,length=30)
    private String requesterPhone;
    @Column(name="requester_role",nullable=false,length=100)
    private String requesterRole;
    @Column(columnDefinition="TEXT",name="justification",nullable=true)
    private String justification;
    @Enumerated(EnumType.STRING) @Column(name="status",nullable=false,length=30)
    private RequestStatus status;
    @ManyToOne(fetch=FetchType.LAZY,optional=true) @JoinColumn(name="reviewed_by_user_id",nullable=true)
    private User reviewedByUser;
    @Column(name="reviewed_at",nullable=true)
    private LocalDateTime reviewedAt;
    @Column(columnDefinition="TEXT",name="review_notes",nullable=true)
    private String reviewNotes;

    public AccessRequest() {}
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution value) { this.institution=value; }
    public User getRequesterUser() { return requesterUser; }
    public void setRequesterUser(User value) { this.requesterUser=value; }
    public String getRequesterName() { return requesterName; }
    public void setRequesterName(String value) { this.requesterName=value; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String value) { this.requesterEmail=value; }
    public String getRequesterPhone() { return requesterPhone; }
    public void setRequesterPhone(String value) { this.requesterPhone=value; }
    public String getRequesterRole() { return requesterRole; }
    public void setRequesterRole(String value) { this.requesterRole=value; }
    public String getJustification() { return justification; }
    public void setJustification(String value) { this.justification=value; }
    public RequestStatus getStatus() { return status; }
    public void setStatus(RequestStatus value) { this.status=value; }
    public User getReviewedByUser() { return reviewedByUser; }
    public void setReviewedByUser(User value) { this.reviewedByUser=value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime value) { this.reviewedAt=value; }
    public String getReviewNotes() { return reviewNotes; }
    public void setReviewNotes(String value) { this.reviewNotes=value; }
}
