package com.bemfeito.api.institution.membership.repository;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import com.bemfeito.api.institution.membership.entity.*;
public interface InstitutionMembershipRepository extends JpaRepository<InstitutionMembership,Long> {
    Optional<InstitutionMembership> findByUserIdAndInstitutionIdAndStatus(Long userId,Long institutionId,MembershipStatus status);
    @EntityGraph(attributePaths="institution") List<InstitutionMembership> findByUserIdAndStatusOrderByInstitutionDisplayNameAsc(Long userId,MembershipStatus status);
    @EntityGraph(attributePaths="user") List<InstitutionMembership> findByInstitutionIdOrderByUserNameAscIdAsc(Long institutionId);
    @EntityGraph(attributePaths="user") Optional<InstitutionMembership> findByIdAndInstitutionId(Long id,Long institutionId);
    long countByInstitutionIdAndRoleAndStatus(Long institutionId,MembershipRole role,MembershipStatus status);
    @Query("""
        select m from InstitutionMembership m join fetch m.user where m.institution.id=:institutionId
        and m.role=com.bemfeito.api.institution.membership.entity.MembershipRole.OWNER
        and m.status=com.bemfeito.api.institution.membership.entity.MembershipStatus.ACTIVE
        and m.user.status=com.bemfeito.api.user.entity.UserStatus.ACTIVE
        """)
    List<InstitutionMembership> findActiveOwnersWithActiveAccounts(Long institutionId);
}
