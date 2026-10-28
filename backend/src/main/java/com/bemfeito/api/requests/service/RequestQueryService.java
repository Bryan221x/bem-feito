package com.bemfeito.api.requests.service;

import java.util.*;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.requests.dto.RequestInput.*;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.entity.*;
import com.bemfeito.api.requests.repository.*;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.unit.repository.UnitRepository;
import com.bemfeito.api.user.repository.UserRepository;
import static com.bemfeito.api.common.validation.RequestValidation.*;

@Service
@Transactional(readOnly=true)
@PreAuthorize("hasRole('PLATFORM_ADMIN')")
public class RequestQueryService {
    private final RegistrationRequestRepository registrations;
    private final RegistrationRequestContactRepository contacts;
    private final RegistrationRequestUnitRepository units;
    private final RegistrationRequestUnitContactRepository unitContacts;
    private final AccessRequestRepository accesses;
    private final InstitutionRepository institutions;
    private final UnitRepository realUnits;
    private final UserRepository users;
    public RequestQueryService(RegistrationRequestRepository registrations,RegistrationRequestContactRepository contacts,
            RegistrationRequestUnitRepository units,RegistrationRequestUnitContactRepository unitContacts,AccessRequestRepository accesses,
            InstitutionRepository institutions,UnitRepository realUnits,UserRepository users) {
        this.registrations=registrations;this.contacts=contacts;this.units=units;this.unitContacts=unitContacts;this.accesses=accesses;
        this.institutions=institutions;this.realUnits=realUnits;this.users=users;
    }
    @PreAuthorize("permitAll()")
    public List<SearchResult> search(String query) {
        String q=query==null?"":query.trim();require(q.length()<=150,"Busca deve ter no máximo 150 caracteres");
        if(q.length()<2) return List.of();
        String digits=q.replaceAll("[^0-9]", "");
        String name=q.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_");
        return institutions.searchForAccess(name+"%",digits.length()==14?digits:"",PageRequest.of(0,15)).stream().map(i->
            new SearchResult(i.getId(),i.getDisplayName(),i.getCategory().getName(),realUnits.findAccessLocations(i.getId(),PageRequest.of(0,5)))).toList();
    }
    private Pageable pageable(int page,int size) {
        require(page>=0 && size>=1 && size<=100,"Página/tamanho inválido (tamanho máximo 100)");
        return PageRequest.of(page,size,Sort.by(Sort.Direction.DESC,"createdAt","id"));
    }
    public Page<Summary> registrations(RequestStatus status,int page,int size) {
        var paging=pageable(page,size);return (status==null?registrations.findAll(paging):registrations.findByStatus(status,paging)).map(RequestQueryService::summary);
    }
    public Page<Summary> accesses(RequestStatus status,int page,int size) {
        var paging=pageable(page,size);return (status==null?accesses.findAll(paging):accesses.findByStatus(status,paging)).map(RequestQueryService::summary);
    }
    public Dashboard dashboard() {
        var recent=Stream.concat(registrations(null,0,5).stream(),accesses(null,0,5).stream())
            .sorted(Comparator.comparing(Summary::createdAt).reversed().thenComparing(Summary::id,Comparator.reverseOrder())).limit(5).toList();
        return new Dashboard(registrations.countByStatus(RequestStatus.PENDING)+accesses.countByStatus(RequestStatus.PENDING),recent);
    }
    public Detail registration(Long id) {
        var r=registrations.findById(id).orElseThrow(()->notFound("Solicitação"));
        var details=units.findByRequestIdOrderById(id).stream().map(u->new UnitDetail(u.getId(),unitData(u),u.getCity().getName(),u.getCity().getState().getUf())).toList();
        var publicContacts=contacts.findByRequestIdOrderById(id).stream().map(c->new Contact(c.getType(),c.getValue(),c.getLabel(),c.getIsPrimary())).toList();
        var input=new Registration(r.getCategory().getId(),r.getDisplayName(),r.getLegalName(),r.getCnpj(),r.getObjective(),r.getDescription(),r.getHistory(),r.getWebsite(),
            new Responsible(r.getResponsibleName(),r.getResponsibleEmail(),r.getResponsiblePhone(),r.getResponsibleRole()),publicContacts,details.isEmpty()?null:details.getFirst().data());
        var user=users.findByEmail(r.getResponsibleEmail()).orElse(null);
        return new Detail(summary(r),r.getCategory().getName(),input,details,null,user==null?null:user.getId(),user==null?null:user.getStatus(),
            r.getReviewedByUser()==null?null:r.getReviewedByUser().getId(),r.getReviewedAt(),r.getReviewNotes());
    }
    public Detail access(Long id) {
        var r=accesses.findById(id).orElseThrow(()->notFound("Solicitação"));
        var user=r.getRequesterUser()!=null?r.getRequesterUser():users.findByEmail(r.getRequesterEmail()).orElse(null);
        return new Detail(summary(r),null,null,List.of(),new Access(r.getInstitution().getId(),r.getRequesterName(),r.getRequesterEmail(),r.getRequesterPhone(),r.getRequesterRole(),r.getJustification()),
            user==null?null:user.getId(),user==null?null:user.getStatus(),r.getReviewedByUser()==null?null:r.getReviewedByUser().getId(),r.getReviewedAt(),r.getReviewNotes());
    }
    private MainUnit unitData(RegistrationRequestUnit u) {
        return new MainUnit(u.getName(),u.getDocument(),u.getDescription(),u.getCity().getId(),u.getPostalCode(),u.getStreet(),u.getNumber(),u.getComplement(),u.getNeighborhood(),
            unitContacts.findByUnitIdOrderById(u.getId()).stream().map(c->new Contact(c.getType(),c.getValue(),c.getLabel(),c.getIsPrimary())).toList());
    }
    static Summary summary(RegistrationRequest r) { return new Summary(r.getId(),"REGISTRATION",r.getDisplayName(),r.getResponsibleName(),r.getCreatedAt(),r.getStatus()); }
    static Summary summary(AccessRequest r) { return new Summary(r.getId(),"ACCESS",r.getInstitution().getDisplayName(),r.getRequesterName(),r.getCreatedAt(),r.getStatus()); }
}
