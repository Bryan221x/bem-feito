package com.bemfeito.api.institution.membership.entity;
import com.bemfeito.api.common.entity.TimestampedEntity;
import com.bemfeito.api.user.entity.User;
import com.bemfeito.api.institution.entity.Institution;
import jakarta.persistence.*;
@Entity @Table(name="institution_memberships",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","institution_id"}))
public class InstitutionMembership extends TimestampedEntity {
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="user_id",nullable=false) private User user;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="institution_id",nullable=false) private Institution institution;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private MembershipRole role;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private MembershipStatus status;
    public InstitutionMembership() {}
    public User getUser() { return user; }
    public void setUser(User value) { user=value; }
    public Institution getInstitution() { return institution; }
    public void setInstitution(Institution value) { institution=value; }
    public MembershipRole getRole() { return role; }
    public void setRole(MembershipRole value) { role=value; }
    public MembershipStatus getStatus() { return status; }
    public void setStatus(MembershipStatus value) { status=value; }
}
