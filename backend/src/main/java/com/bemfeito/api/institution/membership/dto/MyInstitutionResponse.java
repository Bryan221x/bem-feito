package com.bemfeito.api.institution.membership.dto;
import java.util.List;
import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.institution.membership.entity.*;
public record MyInstitutionResponse(Long institutionId,String displayName,InstitutionStatus status,
    MembershipRole membershipRole,MembershipStatus membershipStatus,List<String> permissions) {}
