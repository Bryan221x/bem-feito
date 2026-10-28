package com.bemfeito.api.requests.service;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import com.bemfeito.api.requests.dto.RequestInput.Decision;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.entity.*;
import com.bemfeito.api.requests.repository.*;
import com.bemfeito.api.institution.entity.Institution;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.institution.contact.entity.InstitutionContact;
import com.bemfeito.api.institution.contact.repository.InstitutionContactRepository;
import com.bemfeito.api.institution.category.repository.InstitutionCategoryRepository;
import com.bemfeito.api.institution.membership.entity.*;
import com.bemfeito.api.institution.membership.repository.InstitutionMembershipRepository;
import com.bemfeito.api.unit.entity.Unit;
import com.bemfeito.api.unit.repository.UnitRepository;
import com.bemfeito.api.unit.contact.entity.UnitContact;
import com.bemfeito.api.unit.contact.repository.UnitContactRepository;
import com.bemfeito.api.location.city.repository.CityRepository;
import com.bemfeito.api.security.InstitutionAuthorizationService;
import com.bemfeito.api.common.domain.*;
import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import static com.bemfeito.api.common.validation.RequestValidation.*;
import static com.bemfeito.api.requests.service.RequestSubmissionService.conflict;

@Service
@Transactional
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class RequestReviewService {
    private final RegistrationRequestRepository registrations;
    private final RegistrationRequestContactRepository requestedContacts;
    private final RegistrationRequestUnitRepository requestedUnits;
    private final RegistrationRequestUnitContactRepository requestedUnitContacts;
    private final AccessRequestRepository accesses;
    private final InstitutionRepository institutions;
    private final InstitutionContactRepository contacts;
    private final InstitutionCategoryRepository categories;
    private final CityRepository cities;
    private final UnitRepository units;
    private final UnitContactRepository unitContacts;
    private final UserRepository users;
    private final InstitutionMembershipRepository memberships;
    private final InstitutionAuthorizationService authorization;
    private final InvitationService invitations;
    private final RequestValidation validation;
    public RequestReviewService(RegistrationRequestRepository registrations,RegistrationRequestContactRepository requestedContacts,
            RegistrationRequestUnitRepository requestedUnits,RegistrationRequestUnitContactRepository requestedUnitContacts,AccessRequestRepository accesses,
            InstitutionRepository institutions,InstitutionContactRepository contacts,InstitutionCategoryRepository categories,CityRepository cities,
            UnitRepository units,UnitContactRepository unitContacts,UserRepository users,InstitutionMembershipRepository memberships,
            InstitutionAuthorizationService authorization,InvitationService invitations,RequestValidation validation) {
        this.registrations=registrations;this.requestedContacts=requestedContacts;this.requestedUnits=requestedUnits;this.requestedUnitContacts=requestedUnitContacts;
        this.accesses=accesses;this.institutions=institutions;this.contacts=contacts;this.categories=categories;this.cities=cities;this.units=units;
        this.unitContacts=unitContacts;this.users=users;this.memberships=memberships;this.authorization=authorization;this.invitations=invitations;this.validation=validation;
    }
    public Reviewed registration(Long id,String action,Decision decision) {
        validation.check(decision);var status=decisionStatus(action,decision);
        var request=registrations.findForReview(id).orElseThrow(()->notFound("Solicitação"));pending(request.getStatus());
        User reviewer=reviewer();Institution institution=null;User user=null;Invitation invitation=null;
        if(status==RequestStatus.APPROVED) {
            if(institutions.existsByCnpj(request.getCnpj())) throw conflict("CNPJ já cadastrado; aprovação não pode ser repetida");
            var category=categories.findById(request.getCategory().getId()).orElseThrow(()->notFound("Categoria"));require(category.isActive(),"Categoria inativa");
            institution=new Institution();institution.setCategory(category);institution.setDisplayName(request.getDisplayName());
            institution.setLegalName(request.getLegalName());institution.setCnpj(request.getCnpj());institution.setObjective(request.getObjective());
            institution.setDescription(request.getDescription());institution.setHistory(request.getHistory());institution.setWebsite(request.getWebsite());
            institution.setStatus(InstitutionStatus.ACTIVE);institutions.saveAndFlush(institution);
            int order=0;
            for(var c:requestedContacts.findByRequestIdOrderById(id)) {
                var copy=new InstitutionContact();copy.setInstitution(institution);copy.setType(c.getType());copy.setValue(c.getValue());copy.setLabel(c.getLabel());
                copy.setIsPrimary(c.getIsPrimary());copy.setIsPublic(true);copy.setDisplayOrder(order++);contacts.save(copy);
            }
            var mainUnits=requestedUnits.findByRequestIdOrderById(id);require(!mainUnits.isEmpty(),"Solicitação sem unidade principal");
            for(var source:mainUnits) {
                var city=cities.findById(source.getCity().getId()).orElseThrow(()->notFound("Cidade"));
                var copy=new Unit();copy.setInstitution(institution);copy.setCity(city);copy.setName(source.getName());copy.setDocument(source.getDocument());
                copy.setDescription(source.getDescription());copy.setPostalCode(source.getPostalCode());copy.setStreet(source.getStreet());copy.setNumber(source.getNumber());
                copy.setComplement(source.getComplement());copy.setNeighborhood(source.getNeighborhood());copy.setStatus(UnitStatus.ACTIVE);units.save(copy);
                int contactOrder=0;
                for(var c:requestedUnitContacts.findByUnitIdOrderById(source.getId())) {
                    var uc=new UnitContact();uc.setUnit(copy);uc.setType(c.getType());uc.setValue(c.getValue());uc.setLabel(c.getLabel());
                    uc.setIsPrimary(c.getIsPrimary());uc.setIsPublic(true);uc.setDisplayOrder(contactOrder++);unitContacts.save(uc);
                }
            }
            user=resolve(request.getResponsibleEmail(),request.getResponsibleName());link(user,institution,MembershipRole.OWNER);
            if(user.getStatus()==UserStatus.INVITED) invitation=invitations.issue(user,institution.getDisplayName());
        }
        request.setStatus(status);request.setReviewedByUser(reviewer);request.setReviewedAt(LocalDateTime.now());request.setReviewNotes(decision.notes());
        registrations.flush();return new Reviewed(id,status,institution==null?null:institution.getId(),user==null?null:user.getId(),invitation);
    }
    public Reviewed access(Long id,String action,Decision decision) {
        validation.check(decision);var status=decisionStatus(action,decision);
        if(status==RequestStatus.APPROVED) require(decision.membershipRole()!=null,"Selecione explicitamente a permissão do sistema");
        var request=accesses.findForReview(id).orElseThrow(()->notFound("Solicitação"));pending(request.getStatus());
        User reviewer=reviewer();User user=null;Invitation invitation=null;
        var institution=request.getInstitution();
        if(status==RequestStatus.APPROVED) {
            institution=institutions.findForMembershipUpdate(institution.getId()).orElseThrow(()->notFound("Instituição"));
            if(institution.getStatus()!=InstitutionStatus.ACTIVE) throw conflict("Instituição não está ativa");
            user=resolve(request.getRequesterEmail(),request.getRequesterName());
            if(request.getRequesterUser()!=null && !request.getRequesterUser().getId().equals(user.getId())) throw conflict("Identidade do solicitante inconsistente");
            link(user,institution,decision.membershipRole());
            if(user.getStatus()==UserStatus.INVITED) invitation=invitations.issue(user,institution.getDisplayName());
        }
        request.setStatus(status);request.setReviewedByUser(reviewer);request.setReviewedAt(LocalDateTime.now());request.setReviewNotes(decision.notes());
        accesses.flush();return new Reviewed(id,status,institution.getId(),user==null?null:user.getId(),invitation);
    }
    private User reviewer() {
        Long id=authorization.currentUserId();
        if(id==null) throw new org.springframework.security.access.AccessDeniedException("Sessão administrativa real necessária");
        return users.findById(id).orElseThrow(()->notFound("User"));
    }
    private User resolve(String email,String name) {
        var user=users.findForInvitationByEmail(email).orElse(null);
        if(user==null) { user=new User();user.setName(name);user.setEmail(email);user.setStatus(UserStatus.INVITED);user.setPlatformRole(PlatformRole.USER);users.saveAndFlush(user); }
        if(user.getStatus()==UserStatus.INACTIVE || user.getStatus()==UserStatus.SUSPENDED
                || (user.getStatus()==UserStatus.ACTIVE && !user.hasUsablePasswordHash()))
            throw conflict("Conta existente indisponível; administrador deve resolver a situação antes de aprovar");
        return user;
    }
    private void link(User user,Institution institution,MembershipRole role) {
        var membership=memberships.findByUserIdAndInstitutionId(user.getId(),institution.getId()).orElse(null);
        if(membership!=null && membership.getStatus()==MembershipStatus.ACTIVE) throw conflict("Conta já possui vínculo ativo");
        if(membership==null) { membership=new InstitutionMembership();membership.setInstitution(institution);membership.setUser(user); }
        membership.setRole(role);membership.setStatus(MembershipStatus.ACTIVE);memberships.saveAndFlush(membership);
    }
    private static void pending(RequestStatus status) { if(status!=RequestStatus.PENDING) throw conflict("Somente solicitações PENDING podem receber decisão"); }
    private static RequestStatus decisionStatus(String action,Decision decision) {
        RequestStatus status=switch(action) {
            case "approve"->RequestStatus.APPROVED;case "reject"->RequestStatus.REJECTED;
            case "needs-changes"->RequestStatus.NEEDS_CHANGES;case "cancel"->RequestStatus.CANCELLED;
            default->throw notFound("Ação"); };
        if(status!=RequestStatus.APPROVED) require(decision.notes()!=null && !decision.notes().isBlank(),"Informe o motivo da decisão");
        return status;
    }
}
