package com.bemfeito.api.institution.membership.dto;
import com.bemfeito.api.common.dto.PatchRequest;
import com.bemfeito.api.institution.membership.entity.*;
import com.fasterxml.jackson.annotation.JsonSetter;
public class MembershipPatch extends PatchRequest {
    private MembershipRole role;
    private MembershipStatus status;
    @JsonSetter("role") public void setRole(MembershipRole value) { mark("role");role=value; }
    @JsonSetter("status") public void setStatus(MembershipStatus value) { mark("status");status=value; }
    public MembershipRole getRole() { return role; }
    public MembershipStatus getStatus() { return status; }
}
