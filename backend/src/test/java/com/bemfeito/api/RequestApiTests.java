package com.bemfeito.api;

import java.time.LocalDateTime;
import java.util.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.security.UserPrincipal;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import com.bemfeito.api.requests.service.InvitationService;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Every identity/request lives in isolated H2 and is rolled back. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:requests-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc @Transactional @Sql("/institutional-fixtures.sql")
class RequestApiTests {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    private User admin;
    private static final String PASSWORD=UUID.randomUUID().toString();
    private static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    @BeforeEach void setup() { admin=account("admin-requests@example.invalid",UserStatus.ACTIVE,PlatformRole.PLATFORM_ADMIN); }
    private User account(String email,UserStatus status,PlatformRole role) {
        var u=new User();u.setName("Responsável de teste H2");u.setEmail(email);u.setStatus(status);u.setPlatformRole(role);
        u.setPasswordHash(status==UserStatus.INVITED?null:HASH);return users.saveAndFlush(u);
    }
    private Long id(String table,String field,String value) { return jdbc.queryForObject("SELECT id FROM "+table+" WHERE "+field+"=?",Long.class,value); }
    private long count(String table) { return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class); }
    private Map<String,Object> contact(String type,String value,boolean primary) { return new LinkedHashMap<>(Map.of("type",type,"value",value,"isPrimary",primary)); }
    private Map<String,Object> registration() {
        var unit=new LinkedHashMap<String,Object>(Map.of("name","Unidade nova H2","cityId",id("cities","name","Luz"),"postalCode","35595-000","street"," Rua H2 ","number"," 12 ","neighborhood","Centro","contacts",List.of(contact("WHATSAPP","(31) 99999-0000",true))));
        var input=new LinkedHashMap<String,Object>(Map.of("categoryId",id("institution_categories","name","Educação"),"displayName"," Instituição nova H2 ","legalName","Razão nova H2","cnpj","04.252.011/0001-10","objective","Objetivo","description","Descrição","history","História","website","https://example.invalid",
            "responsible",new LinkedHashMap<>(Map.of("name"," Pessoa H2 ","email"," RESPONSIBLE@EXAMPLE.INVALID ","phone","(31) 99999-0000","role"," Diretoria ")),"publicContacts",new ArrayList<>(List.of(contact("EMAIL"," PUBLIC@EXAMPLE.INVALID ",true)))));
        input.put("mainUnit",unit);return input;
    }
    @SuppressWarnings("unchecked") private Map<String,Object> child(Map<String,Object> map,String key) { return (Map<String,Object>)map.get(key); }
    private MockHttpServletRequestBuilder jsonPost(String path,Object body) { return post(path).with(csrf()).contentType("application/json").content(mapper.writeValueAsString(body)); }
    private ResultActions adminPost(String path,Object body) throws Exception { return mvc.perform(jsonPost(path,body).with(user(new UserPrincipal(admin)))); }
    private Long createRegistration(Map<String,Object> input) throws Exception { var json=mvc.perform(jsonPost("/api/registration-requests",input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return ((Number)JsonPath.read(json,"$.id")).longValue(); }
    private String approve(Long id) throws Exception { return adminPost("/api/admin/registration-requests/"+id+"/approve",Map.of("notes","Conferido H2")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString(); }
    private String token(String approval) { String url=JsonPath.read(approval,"$.invitation.activationUrl");return url.substring(url.indexOf("#token=")+7); }
    private Map<String,Object> access() { return new LinkedHashMap<>(Map.of("institutionId",id("institutions","display_name","Alpha Teste"),"requesterName","Solicitante H2","requesterEmail","access@example.invalid","requesterPhone","(31) 99999-0000","requesterRole","Tesouraria","justification","Vínculo informado")); }
    private Long createAccess(Map<String,Object> input) throws Exception { var json=mvc.perform(jsonPost("/api/access-requests",input)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();return ((Number)JsonPath.read(json,"$.id")).longValue(); }

    @Test void publicSubmissionNormalizesAndExposesOnlyReceipt() throws Exception {
        mvc.perform(jsonPost("/api/registration-requests",registration())).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.createdAt").exists()).andExpect(jsonPath("$",aMapWithSize(3)));
        assertThat(jdbc.queryForObject("SELECT cnpj FROM registration_requests",String.class)).isEqualTo("04252011000110");
        assertThat(jdbc.queryForObject("SELECT responsible_email FROM registration_requests",String.class)).isEqualTo("responsible@example.invalid");
        assertThat(jdbc.queryForObject("SELECT postal_code FROM registration_request_units",String.class)).isEqualTo("35595000");
        assertThat(jdbc.queryForObject("SELECT value FROM registration_request_contacts",String.class)).isEqualTo("public@example.invalid");
        assertThat(jdbc.queryForObject("SELECT value FROM registration_request_unit_contacts",String.class)).isEqualTo("31999990000");
    }
    @ParameterizedTest @ValueSource(strings={"registration-requests","access-requests","account-invitations/inspect","account-invitations/accept"})
    void allPublicMutationsRequireCsrf(String path) throws Exception { mvc.perform(post("/api/"+path).contentType("application/json").content("{}")).andExpect(status().isForbidden()); }
    @ParameterizedTest @ValueSource(strings={"category","city","role","email","cnpj","postal","website","contact","two-primary","duplicate-contact","empty-contacts"})
    void invalidSubmissionsDoNotPersist(String failure) throws Exception {
        var input=registration();
        switch(failure) {
            case "category"->input.put("categoryId",999999);case "city"->child(input,"mainUnit").put("cityId",999999);
            case "role"->child(input,"responsible").put("role"," ");case "email"->child(input,"responsible").put("email","invalid");
            case "cnpj"->input.put("cnpj","11.111.111/1111-11");case "postal"->child(input,"mainUnit").put("postalCode","123");
            case "website"->input.put("website","javascript:alert(1)");case "contact"->input.put("publicContacts",List.of(contact("EMAIL","invalid",true)));
            case "two-primary"->input.put("publicContacts",List.of(contact("EMAIL","a@example.invalid",true),contact("PHONE","31999990000",true)));
            case "duplicate-contact"->input.put("publicContacts",List.of(contact("EMAIL"," A@EXAMPLE.INVALID ",false),contact("EMAIL","a@example.invalid",true)));
            case "empty-contacts"->input.put("publicContacts",List.of());
        }
        mvc.perform(jsonPost("/api/registration-requests",input)).andExpect(failure.equals("category")||failure.equals("city")?status().isNotFound():status().isBadRequest());
        assertThat(count("registration_requests")).isZero();
    }
    @Test void pendingAndExistingInstitutionCnpjAreConflicts() throws Exception {
        var input=registration();createRegistration(input);mvc.perform(jsonPost("/api/registration-requests",input)).andExpect(status().isConflict());
        input.put("cnpj","11.222.333/0001-81");mvc.perform(jsonPost("/api/registration-requests",input)).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"reject","needs-changes","cancel"})
    void terminalDecisionRequiresNotesAndAllowsCorrectedSubmission(String action) throws Exception {
        Long id=createRegistration(registration());String endpoint="/api/admin/registration-requests/"+id+"/"+action;
        adminPost(endpoint,Map.of()).andExpect(status().isBadRequest());adminPost(endpoint,Map.of("notes","Motivo H2")).andExpect(status().isOk());
        createRegistration(registration());assertThat(count("institutions")).isEqualTo(3);
    }
    @Test void adminDataAndActionsAreProtectedAndRejectIdentitySpoofing() throws Exception {
        Long id=createRegistration(registration());var ordinary=account("ordinary@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);
        for(String endpoint:List.of("/api/admin/registration-requests","/api/admin/registration-requests/"+id,"/api/admin/access-requests")) {
            mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());mvc.perform(get(endpoint).with(user(new UserPrincipal(ordinary)))).andExpect(status().isForbidden());
        }
        mvc.perform(jsonPost("/api/admin/registration-requests/"+id+"/approve",Map.of()).with(user(new UserPrincipal(ordinary)))).andExpect(status().isForbidden());
        adminPost("/api/admin/registration-requests/"+id+"/approve",Map.of("reviewerId",ordinary.getId())).andExpect(status().isBadRequest());
        mvc.perform(post("/api/admin/registration-requests/"+id+"/approve").with(user(new UserPrincipal(admin))).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void approvalCopiesWholeAggregateAndMarksReviewer() throws Exception {
        Long requestId=createRegistration(registration());String result=approve(requestId);String raw=token(result);
        Long institutionId=((Number)JsonPath.read(result,"$.institutionId")).longValue();Long userId=((Number)JsonPath.read(result,"$.userId")).longValue();
        assertThat(count("institutions")).isEqualTo(4);assertThat(count("units")).isEqualTo(5);
        assertThat(jdbc.queryForObject("SELECT status FROM institutions WHERE id=?",String.class,institutionId)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT value FROM institution_contacts WHERE institution_id=?",String.class,institutionId)).isEqualTo("public@example.invalid");
        var unit=jdbc.queryForMap("SELECT city_id,latitude,longitude FROM units WHERE institution_id=?",institutionId);
        assertThat(((Number)unit.get("city_id")).longValue()).isEqualTo(id("cities","name","Luz"));assertThat(unit.get("latitude")).isNull();assertThat(unit.get("longitude")).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM unit_contacts c JOIN units u ON c.unit_id=u.id WHERE u.institution_id=?",Long.class,institutionId)).isEqualTo(1);
        var user=users.findById(userId).orElseThrow();assertThat(user.getStatus()).isEqualTo(UserStatus.INVITED);assertThat(user.getPasswordHash()).isNull();assertThat(user.getPlatformRole()).isEqualTo(PlatformRole.USER);
        assertThat(jdbc.queryForObject("SELECT role FROM institution_memberships WHERE user_id=?",String.class,userId)).isEqualTo("OWNER");
        assertThat(jdbc.queryForObject("SELECT status FROM institution_memberships WHERE user_id=?",String.class,userId)).isEqualTo("ACTIVE");
        var request=jdbc.queryForMap("SELECT reviewed_by_user_id,reviewed_at,review_notes FROM registration_requests WHERE id=?",requestId);
        assertThat(((Number)request.get("reviewed_by_user_id")).longValue()).isEqualTo(admin.getId());assertThat(request.get("reviewed_at")).isNotNull();assertThat(request.get("review_notes")).isEqualTo("Conferido H2");
        assertThat(raw).matches("[A-Za-z0-9_-]{43}");assertThat(jdbc.queryForObject("SELECT token_hash FROM account_invitation_tokens",String.class)).isEqualTo(InvitationService.hash(raw)).doesNotContain(raw);
        adminPost("/api/admin/registration-requests/"+requestId+"/approve",Map.of()).andExpect(status().isConflict());assertThat(count("institutions")).isEqualTo(4);
        mvc.perform(get("/api/admin/registration-requests/"+requestId).with(user(new UserPrincipal(admin)))).andExpect(status().isOk()).andExpect(content().string(not(containsString(raw)))).andExpect(jsonPath("$.categoryName").value("Educação"));
    }
    @Test void existingActiveUserKeepsPasswordAndNeedsNoInvitation() throws Exception {
        var u=account("responsible@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);String hash=u.getPasswordHash();String result=approve(createRegistration(registration()));
        assertThat(((Number)JsonPath.read(result,"$.userId")).longValue()).isEqualTo(u.getId());assertThat(JsonPath.<Object>read(result,"$.invitation")).isNull();
        assertThat(users.findById(u.getId()).orElseThrow().getPasswordHash()).isEqualTo(hash);assertThat(count("account_invitation_tokens")).isZero();
    }
    @Test void invitedUserIsReusedAndOldInvitationInvalidated() throws Exception {
        var u=account("responsible@example.invalid",UserStatus.INVITED,PlatformRole.USER);
        String first=adminPost("/api/admin/users/"+u.getId()+"/invitation",Map.of()).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String old=JsonPath.read(first,"$.activationUrl");old=old.substring(old.indexOf("#token=")+7);String approved=approve(createRegistration(registration()));
        assertThat(((Number)JsonPath.read(approved,"$.userId")).longValue()).isEqualTo(u.getId());
        mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",old))).andExpect(jsonPath("$.state").value("ALREADY_USED"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_invitation_tokens WHERE used_at IS NULL",Long.class)).isEqualTo(1);
    }
    @ParameterizedTest @EnumSource(value=UserStatus.class,names={"INACTIVE","SUSPENDED"})
    void unavailableAccountsAreNotReactivated(UserStatus status) throws Exception {
        var u=account("responsible@example.invalid",status,PlatformRole.USER);Long id=createRegistration(registration());
        adminPost("/api/admin/registration-requests/"+id+"/approve",Map.of()).andExpect(status().isConflict());
        assertThat(users.findById(u.getId()).orElseThrow().getStatus()).isEqualTo(status);
    }
    @Test void accessSearchReturnsActiveMinimalDataAndEscapesWildcards() throws Exception {
        mvc.perform(get("/api/institutions/access-search").param("q","Al")).andExpect(status().isOk()).andExpect(jsonPath("$[0].displayName").value("Alpha Teste"))
            .andExpect(jsonPath("$[0].legalName").doesNotExist()).andExpect(jsonPath("$[0].locations[0].city").exists());
        mvc.perform(get("/api/institutions/access-search").param("q","11.222.333/0001-81")).andExpect(jsonPath("$.length()").value(1));
        mvc.perform(get("/api/institutions/access-search").param("q","Beta")).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/institutions/access-search").param("q","%%")).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void anonymousAndSessionAccessUseOnlyServerIdentity() throws Exception {
        var input=access();Long id=createAccess(input);assertThat(jdbc.queryForMap("SELECT requester_user_id FROM access_requests WHERE id=?",id).get("requester_user_id")).isNull();
        var u=account("session@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);input.put("requesterEmail",u.getEmail());input.put("requesterName","Nome arbitrário");
        mvc.perform(jsonPost("/api/access-requests",input).with(user(new UserPrincipal(u)))).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("SELECT requester_name FROM access_requests WHERE requester_user_id=?",String.class,u.getId())).isEqualTo(u.getName());
        input.put("requesterEmail","other@example.invalid");mvc.perform(jsonPost("/api/access-requests",input).with(user(new UserPrincipal(u)))).andExpect(status().isConflict());
        input.put("requesterUserId",admin.getId());mvc.perform(jsonPost("/api/access-requests",input)).andExpect(status().isBadRequest());
    }
    @Test void duplicateAccessInactiveInstitutionAndExistingMemberAreBlocked() throws Exception {
        var input=access();createAccess(input);mvc.perform(jsonPost("/api/access-requests",input)).andExpect(status().isConflict());
        input.put("institutionId",id("institutions","display_name","Beta Teste"));mvc.perform(jsonPost("/api/access-requests",input)).andExpect(status().isConflict());
        input.put("institutionId",999999);mvc.perform(jsonPost("/api/access-requests",input)).andExpect(status().isNotFound());
    }
    @ParameterizedTest @ValueSource(strings={"EDITOR","MANAGER","OWNER"})
    void accessApprovalUsesExplicitSystemRoleAndPreservesExistingAccount(String role) throws Exception {
        var u=account("access@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);Long id=createAccess(access());
        adminPost("/api/admin/access-requests/"+id+"/approve",Map.of("notes","Análise")).andExpect(status().isBadRequest());
        adminPost("/api/admin/access-requests/"+id+"/approve",Map.of("membershipRole",role)).andExpect(status().isOk()).andExpect(jsonPath("$.invitation").isEmpty());
        assertThat(jdbc.queryForObject("SELECT role FROM institution_memberships WHERE user_id=?",String.class,u.getId())).isEqualTo(role);
        mvc.perform(jsonPost("/api/access-requests",access())).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"reject","needs-changes","cancel"})
    void accessReviewStoresReviewerAndNotes(String action) throws Exception {
        Long id=createAccess(access());adminPost("/api/admin/access-requests/"+id+"/"+action,Map.of("notes","Motivo H2")).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("SELECT reviewed_by_user_id FROM access_requests WHERE id=?",Long.class,id)).isEqualTo(admin.getId());
        assertThat(count("institution_memberships")).isZero();
    }
    @Test void adminPaginationFilterAndSummaryWork() throws Exception {
        Long id=createRegistration(registration());createAccess(access());
        mvc.perform(get("/api/admin/registration-requests").param("status","PENDING").param("size","1").with(user(new UserPrincipal(admin))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(id)).andExpect(jsonPath("$.content[0].responsibleEmail").doesNotExist());
        mvc.perform(get("/api/admin/requests/summary").with(user(new UserPrincipal(admin)))).andExpect(jsonPath("$.pendingRequests").value(2));
        mvc.perform(get("/api/admin/access-requests").param("size","101").with(user(new UserPrincipal(admin)))).andExpect(status().isBadRequest());
    }
    @Test void acceptActivatesUserWithBcryptAndTokenCannotBeReused() throws Exception {
        String result=approve(createRegistration(registration()));String raw=token(result);
        mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",raw))).andExpect(jsonPath("$.state").value("READY")).andExpect(jsonPath("$.maskedEmail").value(containsString("***")));
        mvc.perform(jsonPost("/api/account-invitations/accept",Map.of("token",raw,"password",PASSWORD,"confirmPassword",PASSWORD))).andExpect(status().isOk());
        var u=users.findByEmail("responsible@example.invalid").orElseThrow();assertThat(u.getStatus()).isEqualTo(UserStatus.ACTIVE);assertThat(u.getEmailVerifiedAt()).isNotNull();
        assertThat(u.getPasswordHash()).startsWith("$2a$12$");assertThat(new BCryptPasswordEncoder().matches(PASSWORD,u.getPasswordHash())).isTrue();
        mvc.perform(jsonPost("/api/account-invitations/accept",Map.of("token",raw,"password",PASSWORD,"confirmPassword",PASSWORD))).andExpect(status().isBadRequest());
        mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",raw))).andExpect(jsonPath("$.state").value("ALREADY_USED"));
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("email",u.getEmail()).param("password",PASSWORD)).andExpect(status().isOk());
        adminPost("/api/admin/users/"+u.getId()+"/invitation",Map.of()).andExpect(status().isConflict());
    }
    @ParameterizedTest @ValueSource(strings={"short","bytes","confirmation","invalid","expired"})
    void invalidInvitationOrPasswordDoesNotActivate(String failure) throws Exception {
        String raw=token(approve(createRegistration(registration())));String password=PASSWORD,confirmation=PASSWORD;
        switch(failure) { case "short"->password=confirmation="x";case "bytes"->password=confirmation="ã".repeat(40);case "confirmation"->confirmation=UUID.randomUUID().toString();case "invalid"->raw="A".repeat(43);case "expired"->jdbc.update("UPDATE account_invitation_tokens SET expires_at=?",LocalDateTime.now().minusHours(1)); }
        if(failure.equals("expired")) mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",raw))).andExpect(jsonPath("$.state").value("EXPIRED"));
        if(failure.equals("invalid")) mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",raw))).andExpect(jsonPath("$.state").value("INVALID"));
        mvc.perform(jsonPost("/api/account-invitations/accept",Map.of("token",raw,"password",password,"confirmPassword",confirmation))).andExpect(status().isBadRequest());
        assertThat(users.findByEmail("responsible@example.invalid").orElseThrow().getStatus()).isEqualTo(UserStatus.INVITED);
    }
    @ParameterizedTest @ValueSource(strings={"PENDING","REVOKED"})
    void accessApprovalReactivatesExistingMembershipWithoutDuplicate(String state) throws Exception {
        var u=account("access@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);Long institution=id("institutions","display_name","Alpha Teste");
        jdbc.update("INSERT INTO institution_memberships(user_id,institution_id,role,status) VALUES(?,?,'EDITOR',?)",u.getId(),institution,state);
        Long request=createAccess(access());adminPost("/api/admin/access-requests/"+request+"/approve",Map.of("membershipRole","MANAGER")).andExpect(status().isOk());
        assertThat(count("institution_memberships")).isEqualTo(1);assertThat(jdbc.queryForObject("SELECT status FROM institution_memberships",String.class)).isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject("SELECT role FROM institution_memberships",String.class)).isEqualTo("MANAGER");
    }
    @Test void accessApprovalCreatesInvitedAccountAndManualInvitation() throws Exception {
        Long request=createAccess(access());adminPost("/api/admin/access-requests/"+request+"/approve",Map.of("membershipRole","EDITOR"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.invitation.deliveryMode").value("MANUAL")).andExpect(jsonPath("$.invitation.activationUrl").value(containsString("#token=")));
        assertThat(users.findByEmail("access@example.invalid").orElseThrow().getStatus()).isEqualTo(UserStatus.INVITED);
        assertThat(jdbc.queryForObject("SELECT role FROM institution_memberships",String.class)).isEqualTo("EDITOR");
    }
    @ParameterizedTest @EnumSource(value=UserStatus.class,names={"ACTIVE","INACTIVE","SUSPENDED"})
    void reissueIsRestrictedToInvitedAccounts(UserStatus state) throws Exception {
        var u=account("unavailable@example.invalid",state,PlatformRole.USER);
        adminPost("/api/admin/users/"+u.getId()+"/invitation",Map.of()).andExpect(status().isConflict());
        mvc.perform(jsonPost("/api/admin/users/"+u.getId()+"/invitation",Map.of())).andExpect(status().isUnauthorized());assertThat(count("account_invitation_tokens")).isZero();
    }
    @Test void invitationCannotResetPasswordOfAlreadyActiveAccount() throws Exception {
        String raw=token(approve(createRegistration(registration())));var u=users.findByEmail("responsible@example.invalid").orElseThrow();
        u.setStatus(UserStatus.ACTIVE);u.setPasswordHash(HASH);users.saveAndFlush(u);
        mvc.perform(jsonPost("/api/account-invitations/inspect",Map.of("token",raw))).andExpect(jsonPath("$.state").value("INVALID"));
        mvc.perform(jsonPost("/api/account-invitations/accept",Map.of("token",raw,"password",PASSWORD,"confirmPassword",PASSWORD))).andExpect(status().isBadRequest());
        assertThat(users.findById(u.getId()).orElseThrow().getPasswordHash()).isEqualTo(HASH);
    }
    @Test void activeAccountWithoutFunctionalPasswordIsNotSilentlyReactivated() throws Exception {
        var u=account("responsible@example.invalid",UserStatus.ACTIVE,PlatformRole.USER);u.setPasswordHash(null);users.saveAndFlush(u);
        adminPost("/api/admin/registration-requests/"+createRegistration(registration())+"/approve",Map.of()).andExpect(status().isConflict());
        assertThat(users.findById(u.getId()).orElseThrow().getPasswordHash()).isNull();
    }

}
