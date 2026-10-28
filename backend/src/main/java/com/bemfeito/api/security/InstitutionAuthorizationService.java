package com.bemfeito.api.security;

import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.institution.dto.*;
import com.bemfeito.api.institution.membership.entity.*;
import com.bemfeito.api.institution.membership.repository.InstitutionMembershipRepository;
import com.bemfeito.api.institution.contact.repository.InstitutionContactRepository;
import com.bemfeito.api.institution.history.repository.InstitutionHistoryImageRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import com.bemfeito.api.unit.dto.*;
import com.bemfeito.api.unit.contact.repository.UnitContactRepository;
import com.bemfeito.api.need.repository.NeedRepository;

/** Política única. IDs de recursos filhos são conferidos com seus pais reais. */
@Service("institutionAuthorization")
@Transactional(readOnly=true)
public class InstitutionAuthorizationService {
    private final InstitutionMembershipRepository memberships;
    private final InstitutionRepository institutions;
    private final UnitRepository units;
    private final NeedRepository needs;
    private final UnitContactRepository unitContacts;
    private final InstitutionContactRepository institutionContacts;
    private final InstitutionHistoryImageRepository images;

    public InstitutionAuthorizationService(InstitutionMembershipRepository memberships, InstitutionRepository institutions,
            UnitRepository units, NeedRepository needs, UnitContactRepository unitContacts,
            InstitutionContactRepository institutionContacts, InstitutionHistoryImageRepository images) {
        this.memberships=memberships;this.institutions=institutions;this.units=units;this.needs=needs;
        this.unitContacts=unitContacts;this.institutionContacts=institutionContacts;this.images=images;
    }

    public boolean isAdmin() {
        Authentication auth=SecurityContextHolder.getContext().getAuthentication();
        return auth!=null && auth.isAuthenticated() && auth.getAuthorities().stream()
            .anyMatch(a->a.getAuthority().equals("ROLE_PLATFORM_ADMIN"));
    }

    public Long currentUserId() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        return auth!=null && auth.getPrincipal() instanceof UserPrincipal user ? user.getId() : null;
    }

    private MembershipRole role(Long institutionId) {
        Long userId=currentUserId();
        if(userId==null) return null;
        return memberships.findByUserIdAndInstitutionIdAndStatus(userId,institutionId,MembershipStatus.ACTIVE)
            .map(m->m.getRole()).orElse(null);
    }

    public boolean canViewInstitution(Long institutionId) { return isAdmin() || role(institutionId)!=null; }
    public boolean canManageInstitution(Long institutionId) { return canViewInstitution(institutionId); }
    public boolean canManageLegalData(Long institutionId) { return isAdmin() || role(institutionId)==MembershipRole.OWNER; }
    public boolean canManageMemberships(Long institutionId) { return canManageLegalData(institutionId); }
    public boolean canCreateUnit(Long institutionId) {
        var role=role(institutionId);
        return isAdmin() || role==MembershipRole.OWNER || role==MembershipRole.MANAGER;
    }
    public boolean canEditUnit(Long unitId) {
        if(isAdmin()) return true;
        return units.findById(unitId).map(u->canManageInstitution(u.getInstitution().getId())).orElse(false);
    }

    public boolean institutionPatch(Long id, InstitutionPatch patch) {
        if(isAdmin()) return true;
        if(!canManageInstitution(id) || patch.has("status")) return false;
        if((patch.has("cnpj") || patch.has("legalName")) && !canManageLegalData(id)) return false;
        return role(id)!=MembershipRole.EDITOR || (!patch.has("displayName") && !patch.has("categoryId"));
    }

    public boolean institutionReplace(Long id, InstitutionRequest request) {
        if(isAdmin()) return true;
        if(!canManageInstitution(id)) return false;
        var existing=institutions.findById(id).orElse(null);
        if(existing==null || existing.getStatus()!=request.status()) return false;
        if(!canManageLegalData(id) && (!Objects.equals(existing.getCnpj(),request.cnpj())
                || !Objects.equals(existing.getLegalName(),request.legalName()))) return false;
        return role(id)!=MembershipRole.EDITOR || (Objects.equals(existing.getDisplayName(),request.displayName())
            && Objects.equals(existing.getCategory().getId(),request.categoryId()));
    }

    public boolean createUnit(Long institutionId, UnitRequest request) {
        return canCreateUnit(institutionId) && (isAdmin() || role(institutionId)==MembershipRole.OWNER
            || (request.status()==com.bemfeito.api.common.domain.UnitStatus.ACTIVE && request.document()==null));
    }

    public boolean unitPatch(Long id, UnitPatch patch) {
        if(isAdmin()) return true;
        var unit=units.findById(id).orElse(null);
        if(unit==null || !canManageInstitution(unit.getInstitution().getId())) return false;
        var role=role(unit.getInstitution().getId());
        if(role!=MembershipRole.OWNER && (patch.has("document") || patch.has("status"))) return false;
        if(role!=MembershipRole.EDITOR) return true;
        return Set.of("cityId","name","document","postalCode","street","number","complement","neighborhood",
            "latitude","longitude","status").stream().noneMatch(patch::has);
    }

    public boolean unitReplace(Long id, UnitRequest request) {
        if(isAdmin()) return true;
        var unit=units.findById(id).orElse(null);
        if(unit==null || !canManageInstitution(unit.getInstitution().getId())) return false;
        var role=role(unit.getInstitution().getId());
        if(role!=MembershipRole.OWNER && (!Objects.equals(unit.getDocument(),request.document()) || unit.getStatus()!=request.status())) return false;
        if(role!=MembershipRole.EDITOR) return true;
        return Objects.equals(unit.getCity().getId(),request.cityId()) && Objects.equals(unit.getName(),request.name())
            && Objects.equals(unit.getPostalCode(),request.postalCode()) && Objects.equals(unit.getStreet(),request.street())
            && Objects.equals(unit.getNumber(),request.number()) && Objects.equals(unit.getComplement(),request.complement())
            && Objects.equals(unit.getNeighborhood(),request.neighborhood())
            && sameDecimal(unit.getLatitude(),request.latitude()) && sameDecimal(unit.getLongitude(),request.longitude());
    }
    private boolean sameDecimal(java.math.BigDecimal left,java.math.BigDecimal right) {
        return left==null ? right==null : right!=null && left.compareTo(right)==0;
    }

    public boolean child(String kind,Long parentId,Long childId) {
        if(isAdmin()) return true;
        boolean institutionChild=kind.equals("institutionContact") || kind.equals("image");
        if(!(institutionChild ? canManageInstitution(parentId) : canEditUnit(parentId))) return false;
        return switch(kind) {
            case "institutionContact" -> institutionContacts.findByIdAndInstitutionId(childId,parentId).isPresent();
            case "image" -> images.findByIdAndInstitutionId(childId,parentId).isPresent();
            case "unitContact" -> unitContacts.findByIdAndUnitId(childId,parentId).isPresent();
            case "need" -> needs.findByIdAndUnitId(childId,parentId).isPresent();
            default -> false;
        };
    }

    public java.util.List<String> permissions(MembershipRole role) {
        var permissions=new java.util.ArrayList<>(java.util.List.of("VIEW","EDIT_PUBLIC_CONTENT","MANAGE_CONTACTS",
            "MANAGE_HISTORY_IMAGES","MANAGE_HOURS","MANAGE_SERVICE_AREAS","MANAGE_NEEDS","EDIT_UNIT_DESCRIPTION"));
        if(role==MembershipRole.OWNER || role==MembershipRole.MANAGER) permissions.addAll(java.util.List.of("EDIT_PROFILE","CREATE_UNIT","EDIT_UNIT"));
        if(role==MembershipRole.OWNER) permissions.addAll(java.util.List.of("EDIT_LEGAL_DATA","CHANGE_UNIT_STATUS","MANAGE_MEMBERSHIPS"));
        return java.util.List.copyOf(permissions);
    }
}
