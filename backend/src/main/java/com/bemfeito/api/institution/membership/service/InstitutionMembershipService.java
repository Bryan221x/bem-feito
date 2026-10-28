package com.bemfeito.api.institution.membership.service;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.bemfeito.api.security.InstitutionAuthorizationService;
import com.bemfeito.api.user.entity.UserStatus;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.institution.membership.repository.InstitutionMembershipRepository;
import com.bemfeito.api.institution.membership.entity.*;
import com.bemfeito.api.institution.membership.dto.*;
import static com.bemfeito.api.common.validation.RequestValidation.*;
@Service @Transactional(readOnly=true)
public class InstitutionMembershipService {
    private final InstitutionMembershipRepository memberships;
    private final InstitutionRepository institutions;
    private final InstitutionAuthorizationService authorization;
    public InstitutionMembershipService(InstitutionMembershipRepository memberships,InstitutionRepository institutions,InstitutionAuthorizationService authorization) {
        this.memberships=memberships;this.institutions=institutions;this.authorization=authorization;
    }
    @PreAuthorize("isAuthenticated()")
    public List<MyInstitutionResponse> mine() {
        Long userId=authorization.currentUserId();
        if(userId==null) return List.of();
        return memberships.findByUserIdAndStatusOrderByInstitutionDisplayNameAsc(userId,MembershipStatus.ACTIVE).stream().map(m->
            new MyInstitutionResponse(m.getInstitution().getId(),m.getInstitution().getDisplayName(),m.getInstitution().getStatus(),
                m.getRole(),m.getStatus(),authorization.permissions(m.getRole()))).toList();
    }
    @PreAuthorize("@institutionAuthorization.canManageMemberships(#institutionId)")
    public List<MembershipResponse> members(Long institutionId) {
        if(!institutions.existsById(institutionId)) throw notFound("Instituição");
        return memberships.findByInstitutionIdOrderByUserNameAscIdAsc(institutionId).stream().map(MembershipResponse::from).toList();
    }
    @Transactional
    @PreAuthorize("@institutionAuthorization.canManageMemberships(#institutionId)")
    public MembershipResponse patch(Long institutionId,Long id,MembershipPatch patch) {
        // Serializa mudanças de responsáveis para que duas remoções não eliminem o último OWNER.
        institutions.findForMembershipUpdate(institutionId).orElseThrow(()->notFound("Instituição"));
        if(!authorization.canManageMemberships(institutionId))
            throw new org.springframework.security.access.AccessDeniedException("Acesso negado");
        var membership=memberships.findByIdAndInstitutionId(id,institutionId).orElseThrow(()->notFound("Membership"));
        var role=patch.has("role") ? patch.getRole() : membership.getRole();
        var status=patch.has("status") ? patch.getStatus() : membership.getStatus();
        require(role!=null && status!=null,"role e status não podem ser null");
        require(status!=MembershipStatus.ACTIVE || membership.getUser().getStatus()==UserStatus.ACTIVE,"Conta precisa estar ativa para ativar o vínculo");
        if(!authorization.isAdmin() && membership.getRole()==MembershipRole.OWNER && membership.getStatus()==MembershipStatus.ACTIVE
            && membership.getUser().getStatus()==UserStatus.ACTIVE && membership.getUser().hasUsablePasswordHash()
            && (role!=MembershipRole.OWNER || status!=MembershipStatus.ACTIVE)
            && memberships.findActiveOwnersWithActiveAccounts(institutionId).stream()
                .filter(owner->owner.getUser().hasUsablePasswordHash()).count()<=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Não é possível remover o último OWNER ativo");
        membership.setRole(role);membership.setStatus(status);memberships.flush();return MembershipResponse.from(membership);
    }
}
