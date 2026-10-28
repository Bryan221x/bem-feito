package com.bemfeito.api.requests.service;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.validation.constraints.Email;
import com.bemfeito.api.common.validation.RequestValidation;
import com.bemfeito.api.common.domain.*;
import com.bemfeito.api.requests.dto.RequestInput.*;
import com.bemfeito.api.requests.dto.RequestOutput.Submitted;
import com.bemfeito.api.requests.entity.*;
import com.bemfeito.api.requests.repository.*;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.institution.category.repository.InstitutionCategoryRepository;
import com.bemfeito.api.institution.membership.repository.InstitutionMembershipRepository;
import com.bemfeito.api.institution.membership.entity.MembershipStatus;
import com.bemfeito.api.location.city.repository.CityRepository;
import com.bemfeito.api.security.InstitutionAuthorizationService;
import com.bemfeito.api.user.repository.UserRepository;
import static com.bemfeito.api.common.validation.RequestValidation.*;

@Service
@Transactional
@PreAuthorize("permitAll()")
public class RequestSubmissionService {
    private final RegistrationRequestRepository registrations;
    private final RegistrationRequestContactRepository contacts;
    private final RegistrationRequestUnitRepository units;
    private final RegistrationRequestUnitContactRepository unitContacts;
    private final AccessRequestRepository accesses;
    private final InstitutionRepository institutions;
    private final InstitutionCategoryRepository categories;
    private final CityRepository cities;
    private final UserRepository users;
    private final InstitutionMembershipRepository memberships;
    private final InstitutionAuthorizationService authorization;
    private final RequestValidation validation;
    public RequestSubmissionService(RegistrationRequestRepository registrations,RegistrationRequestContactRepository contacts,
            RegistrationRequestUnitRepository units,RegistrationRequestUnitContactRepository unitContacts,AccessRequestRepository accesses,
            InstitutionRepository institutions,InstitutionCategoryRepository categories,CityRepository cities,UserRepository users,
            InstitutionMembershipRepository memberships,InstitutionAuthorizationService authorization,RequestValidation validation) {
        this.registrations=registrations;this.contacts=contacts;this.units=units;this.unitContacts=unitContacts;this.accesses=accesses;
        this.institutions=institutions;this.categories=categories;this.cities=cities;this.users=users;
        this.memberships=memberships;this.authorization=authorization;this.validation=validation;
    }
    public Submitted register(Registration input) {
        validation.check(input);validateContacts(input.publicContacts());validateContacts(input.mainUnit().contacts());
        if(institutions.existsByCnpj(input.cnpj())) throw conflict("Instituição já cadastrada; utilize Solicitar acesso");
        if(registrations.existsByCnpjAndStatusIn(input.cnpj(),List.of(RequestStatus.PENDING,RequestStatus.APPROVED)))
            throw conflict("Já existe solicitação pendente ou aprovada para este CNPJ");
        var category=categories.findById(input.categoryId()).orElseThrow(()->notFound("Categoria"));
        require(category.isActive(),"Categoria inativa");
        var city=cities.findById(input.mainUnit().cityId()).orElseThrow(()->notFound("Cidade"));
        var request=new RegistrationRequest();request.setCategory(category);request.setDisplayName(input.displayName());
        request.setLegalName(input.legalName());request.setCnpj(input.cnpj());request.setObjective(input.objective());
        request.setDescription(input.description());request.setHistory(input.history());request.setWebsite(input.website());
        request.setResponsibleName(input.responsible().name());request.setResponsibleEmail(input.responsible().email());
        request.setResponsiblePhone(input.responsible().phone());request.setResponsibleRole(input.responsible().role());request.setStatus(RequestStatus.PENDING);
        registrations.saveAndFlush(request);
        for(var data:input.publicContacts()) {
            var contact=new RegistrationRequestContact();contact.setRequest(request);contact.setType(data.type());
            contact.setValue(data.value());contact.setLabel(data.label());contact.setIsPrimary(data.isPrimary());contacts.save(contact);
        }
        var data=input.mainUnit();var unit=new RegistrationRequestUnit();unit.setRequest(request);unit.setCity(city);
        unit.setName(data.name());unit.setDocument(data.document());unit.setDescription(data.description());unit.setPostalCode(data.postalCode());
        unit.setStreet(data.street());unit.setNumber(data.number());unit.setComplement(data.complement());unit.setNeighborhood(data.neighborhood());units.save(unit);
        for(var c:data.contacts()) {
            var contact=new RegistrationRequestUnitContact();contact.setUnit(unit);contact.setType(c.type());contact.setValue(c.value());
            contact.setLabel(c.label());contact.setIsPrimary(c.isPrimary());unitContacts.save(contact);
        }
        registrations.flush();return new Submitted(request.getId(),request.getStatus(),request.getCreatedAt());
    }
    public Submitted access(Access input) {
        validation.check(input);
        var institution=institutions.findForMembershipUpdate(input.institutionId()).orElseThrow(()->notFound("Instituição"));
        if(institution.getStatus()!=InstitutionStatus.ACTIVE) throw conflict("Instituição não está ativa");
        Long currentId=authorization.currentUserId();
        var current=currentId==null?null:users.findById(currentId).orElseThrow(()->notFound("User"));
        if(current!=null && !current.getEmail().equals(input.requesterEmail())) throw conflict("E-mail deve corresponder à conta autenticada");
        var resolved=current!=null?current:users.findByEmail(input.requesterEmail()).orElse(null);
        if(resolved!=null && memberships.findByUserIdAndInstitutionIdAndStatus(resolved.getId(),institution.getId(),MembershipStatus.ACTIVE).isPresent())
            throw conflict("Esta conta já possui vínculo ativo com a instituição");
        if(accesses.existsByInstitutionIdAndRequesterEmailAndStatus(institution.getId(),input.requesterEmail(),RequestStatus.PENDING))
            throw conflict("Já existe solicitação de acesso pendente para esta instituição e e-mail");
        var request=new AccessRequest();request.setInstitution(institution);request.setRequesterUser(current);
        request.setRequesterName(current==null?input.requesterName():current.getName());request.setRequesterEmail(current==null?input.requesterEmail():current.getEmail());
        request.setRequesterPhone(input.requesterPhone());request.setRequesterRole(input.requesterRole());request.setJustification(input.justification());
        request.setStatus(RequestStatus.PENDING);accesses.saveAndFlush(request);
        return new Submitted(request.getId(),request.getStatus(),request.getCreatedAt());
    }
    private void validateContacts(List<Contact> contacts) {
        require(contacts.stream().filter(c->Boolean.TRUE.equals(c.isPrimary())).count()<=1,"Só um contato principal é permitido");
        var seen=new HashSet<String>();
        for(var contact:contacts) {
            if(contact.type()==ContactType.EMAIL) validation.check(new EmailValue(contact.value()));
            else require(contact.value().matches("\\+?[0-9]{8,15}"),"Telefone deve ter de 8 a 15 dígitos, com + opcional");
            require(seen.add(contact.type()+":"+contact.value()),"Contato duplicado");
        }
    }
    private record EmailValue(@Email String value) {}
    public static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
