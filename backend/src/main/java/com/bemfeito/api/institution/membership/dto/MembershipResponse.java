package com.bemfeito.api.institution.membership.dto;
import com.bemfeito.api.institution.membership.entity.*;
import com.bemfeito.api.user.dto.UserResponse;
public record MembershipResponse(Long id,Long institutionId,UserResponse user,MembershipRole role,MembershipStatus status) {
    public static MembershipResponse from(InstitutionMembership membership) {
        return new MembershipResponse(membership.getId(),membership.getInstitution().getId(),UserResponse.from(membership.getUser()),membership.getRole(),membership.getStatus());
    }
}
