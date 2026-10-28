package com.bemfeito.api;

import java.util.*;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import com.bemfeito.api.security.UserPrincipal;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import com.bemfeito.api.institution.repository.InstitutionRepository;
import com.bemfeito.api.institution.membership.entity.*;
import com.bemfeito.api.institution.membership.repository.InstitutionMembershipRepository;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Login, cookies de sessão e autorização reais; todas as contas existem apenas no H2. */
@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:security-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:reference-schema.sql","spring.sql.init.data-locations=classpath:reference-fixtures.sql",
    "app.bootstrap-admin.enabled=false","app.upload.dir=target/security-test-uploads"})
@AutoConfigureMockMvc @Transactional @Sql("/institutional-fixtures.sql")
@Import(SecurityApiTests.ProbeConfig.class)
class SecurityApiTests {
    private static final String PASSWORD=UUID.randomUUID().toString();
    @Test void uploadHonorsActiveMembershipAndResourceOwnership() throws Exception {
        var output=new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",output);
        var file=new org.springframework.mock.web.MockMultipartFile("file","image.png","image/png",output.toByteArray());
        var editor=login("editor");
        String body=mvc.perform(multipart(institutionPath()+"/history-images/upload").file(file).param("altText","Foto acessível").session(editor).header("X-CSRF-TOKEN",token(editor)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id=JsonPath.read(body,"$.id");
        mvc.perform(multipart("/api/institutions/"+beta()+"/history-images/upload").file(file).param("altText","Foto").session(editor).header("X-CSRF-TOKEN",token(editor))).andExpect(status().isForbidden());
        var other=login("other");
        mvc.perform(delete("/api/institutions/"+beta()+"/history-images/"+id).session(other).header("X-CSRF-TOKEN",token(other))).andExpect(status().isForbidden());
        for(String account:List.of("pending","revoked")) {
            var session=login(account);
            mvc.perform(multipart(institutionPath()+"/history-images/upload").file(file).param("altText","Foto").session(session).header("X-CSRF-TOKEN",token(session))).andExpect(status().isForbidden());
        }
    }
    private static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired InstitutionRepository institutions;
    @Autowired InstitutionMembershipRepository memberships;
    @Autowired tools.jackson.databind.ObjectMapper mapper;
    private final Map<String,User> accounts=new HashMap<>();
    private Long alphaId;
    private Long betaId;

    @TestConfiguration static class ProbeConfig {
        @Bean ProbeController securityProbeController() { return new ProbeController(); }
    }
    @RestController static class ProbeController {
        @GetMapping("/api/admin/security-probe") Map<String,Boolean> probe() { return Map.of("authorized",true); }
    }

    private Long institution(String name) { return jdbc.queryForObject("SELECT id FROM institutions WHERE display_name=?",Long.class,name); }
    private Long alpha() { return alphaId; }
    private Long beta() { return betaId; }
    private Long unit() { return jdbc.queryForObject("SELECT id FROM units WHERE name='Unidade Luz'",Long.class); }
    private Long foreignUnit() { return jdbc.queryForObject("SELECT id FROM units WHERE name='Unidade Oculta'",Long.class); }
    private String institutionPath() { return "/api/institutions/"+alpha(); }
    private String unitPath() { return "/api/units/"+unit(); }
    private String unitRequest() {
        Long city=jdbc.queryForObject("SELECT id FROM cities WHERE name='Luz'",Long.class);
        return "{\"cityId\":"+city+",\"name\":\"Nova unidade teste\",\"postalCode\":\"35595000\",\"street\":\"Rua teste\",\"number\":\"1\",\"neighborhood\":\"Centro\",\"status\":\"ACTIVE\"}";
    }

    @BeforeEach void createAccountsInIsolatedDatabase() {
        alphaId=institution("Alpha Teste");betaId=institution("Beta Teste");
        for(String name:List.of("owner","manager","editor","other","pending","revoked","admin","invited","inactive","suspended","nohash","badhash")) {
            var user=new User();user.setName(name);user.setEmail(name+"@example.invalid");user.setPasswordHash(HASH);
            user.setPlatformRole(name.equals("admin")?PlatformRole.PLATFORM_ADMIN:PlatformRole.USER);
            user.setStatus(switch(name) { case "invited"->UserStatus.INVITED; case "inactive"->UserStatus.INACTIVE; case "suspended"->UserStatus.SUSPENDED; default->UserStatus.ACTIVE; });
            if(name.equals("nohash")) user.setPasswordHash(null);
            if(name.equals("badhash")) user.setPasswordHash("invalid-hash-test-only");
            accounts.put(name,users.saveAndFlush(user));
        }
        for(String name:List.of("owner","manager","editor","pending","revoked")) {
            var role=switch(name) {case "owner"->MembershipRole.OWNER;case "manager"->MembershipRole.MANAGER;default->MembershipRole.EDITOR;};
            var status=switch(name) {case "pending"->MembershipStatus.PENDING;case "revoked"->MembershipStatus.REVOKED;default->MembershipStatus.ACTIVE;};
            membership(name,alpha(),role,status);
        }
        membership("other",beta(),MembershipRole.OWNER,MembershipStatus.ACTIVE);
    }
    private void membership(String user,Long institutionId,MembershipRole role,MembershipStatus status) {
        var m=new InstitutionMembership();m.setUser(accounts.get(user));m.setInstitution(institutions.findById(institutionId).orElseThrow());m.setRole(role);m.setStatus(status);memberships.saveAndFlush(m);
    }
    private String token(MockHttpSession session) throws Exception {
        var body=mvc.perform(get("/api/auth/csrf").session(session)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(body,"$.token");
    }
    private MockHttpSession login(String account) throws Exception {
        var session=new MockHttpSession();
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token(session))
            .contentType("application/x-www-form-urlencoded").param("email",account+"@example.invalid").param("password",PASSWORD))
            .andExpect(status().isOk()).andExpect(jsonPath("$.authenticated",is(true)));
        return session;
    }
    private ResultActions mutate(MockHttpServletRequestBuilder request,MockHttpSession session,String body) throws Exception {
        return mvc.perform(request.session(session).header("X-CSRF-TOKEN",token(session)).contentType("application/json").content(body));
    }

    @Test void publicEndpointsDoNotRequireAuthentication() throws Exception {
        for(String path:List.of("/api/health","/api/states","/api/cities","/api/cities/available","/api/institution-categories",
            "/api/service-areas","/api/need-categories","/api/institutions",institutionPath(),institutionPath()+"/units",unitPath(),"/api/auth/csrf"))
            mvc.perform(get(path)).andExpect(status().isOk());
    }
    @Test void protectedEndpointsReturnJson401WithoutHtmlRedirect() throws Exception {
        for(String path:List.of("/api/auth/me","/api/me/institutions","/api/management/institutions","/api/management/units/"+unit(),institutionPath()+"/members"))
            mvc.perform(get(path)).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(header().doesNotExist("Location")).andExpect(jsonPath("$.status",is(401)));
        var session=new MockHttpSession();mutate(patch(institutionPath()),session,"{\"history\":\"x\"}").andExpect(status().isUnauthorized());
    }
    @Test void invalidCredentialsAndNonActiveAccountsHaveSameGenericError() throws Exception {
        for(String account:List.of("unknown","invited","inactive","suspended","nohash","badhash","owner")) {
            var session=new MockHttpSession();
            mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token(session))
                .param("email",account+"@example.invalid").param("password",account.equals("owner")?UUID.randomUUID().toString():PASSWORD))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.message",is("E-mail ou senha inválidos.")));
        }
    }
    @Test void activeLoginNormalizesEmailAndPersistsSessionWithoutHash() throws Exception {
        var session=new MockHttpSession();String initial=session.getId();
        String response=mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",token(session))
            .param("email","  OWNER@EXAMPLE.INVALID ").param("password",PASSWORD))
            .andExpect(status().isOk()).andExpect(jsonPath("$.user.email",is("owner@example.invalid")))
            .andReturn().getResponse().getContentAsString();
        assertThat(response).doesNotContain("password",HASH,PASSWORD);
        assertThat(session.getId()).isNotEqualTo(initial);
        var context=(SecurityContext)session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
        assertThat(((UserPrincipal)context.getAuthentication().getPrincipal()).getPassword()).isNull();
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isOk())
            .andExpect(jsonPath("$.name",is("owner"))).andExpect(jsonPath("$.passwordHash").doesNotExist());
    }
    @Test void logoutInvalidatesSessionClearsCookieAndOldSessionCannotBeUsed() throws Exception {
        var session=login("owner");String oldId=session.getId();
        mvc.perform(get("/api/auth/logout").session(session)).andExpect(status().isNotFound());
        assertThat(session.isInvalid()).isFalse();
        mutate(post("/api/auth/logout"),session,"{}").andExpect(status().isOk())
            .andExpect(cookie().maxAge("JSESSIONID",0));
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/auth/me").cookie(new jakarta.servlet.http.Cookie("JSESSIONID",oldId))).andExpect(status().isUnauthorized());
    }
    @Test void csrfMissingInvalidOrStaleIsRejectedIncludingLoginLogoutAndMutations() throws Exception {
        mvc.perform(post("/api/auth/login").param("email","owner@example.invalid").param("password",PASSWORD)).andExpect(status().isForbidden());
        var session=login("owner");
        for(var request:List.of(post(institutionPath()+"/units"),patch(institutionPath()),delete(unitPath()+"/needs/1"),post("/api/auth/logout")))
            mvc.perform(request.session(session).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(patch(institutionPath()).session(session).header("X-CSRF-TOKEN","invalid").contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mutate(patch(institutionPath()),session,"{\"history\":\"Atualizada\"}").andExpect(status().isOk());
    }
    @Test void loginRotatesCsrfToken() throws Exception {
        var session=new MockHttpSession();String old=token(session);
        mvc.perform(post("/api/auth/login").session(session).header("X-CSRF-TOKEN",old).param("email","owner@example.invalid").param("password",PASSWORD)).andExpect(status().isOk());
        mvc.perform(patch(institutionPath()).session(session).header("X-CSRF-TOKEN",old).contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mutate(patch(institutionPath()),session,"{}").andExpect(status().isOk());
    }
    @Test void corsAllowsConfiguredOriginsAndCredentialedPreflight() throws Exception {
        mvc.perform(options("/api/auth/login").header("Origin","http://127.0.0.1:5500")
            .header("Access-Control-Request-Method","POST").header("Access-Control-Request-Headers","content-type,x-csrf-token"))
            .andExpect(status().isOk()).andExpect(header().string("Access-Control-Allow-Origin","http://127.0.0.1:5500"))
            .andExpect(header().string("Access-Control-Allow-Credentials","true"));
        mvc.perform(get("/api/auth/csrf").header("Origin","https://untrusted.example.invalid"))
            .andExpect(status().isForbidden()).andExpect(header().doesNotExist("Access-Control-Allow-Origin"))
            .andExpect(jsonPath("$.status",is(403)));
    }
    @Test void platformRoutesAreAdminOnlyAndRemainReservedForFutureEndpoints() throws Exception {
        mvc.perform(get("/api/admin/security-probe").session(login("owner"))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/security-probe").session(login("admin"))).andExpect(status().isOk());
        mutate(post("/api/auth/register"),login("owner"),"{}").andExpect(status().isNotFound());
    }
    @Test void activeMembershipCanAccessManagementButOtherPendingAndRevokedCannot() throws Exception {
        for(String user:List.of("owner","manager","editor"))
            mvc.perform(get("/api/management/institutions/"+alpha()).session(login(user))).andExpect(status().isOk());
        for(String user:List.of("other","pending","revoked")) {
            var session=login(user);
            mvc.perform(get("/api/management/institutions/"+alpha()).session(session)).andExpect(status().isForbidden());
            mutate(patch(institutionPath()),session,"{\"history\":\"x\"}").andExpect(status().isForbidden());
        }
    }
    @Test void usersOnlyReceiveTheirOwnActiveMembershipsAndServerPermissions() throws Exception {
        mvc.perform(get("/api/me/institutions").session(login("owner"))).andExpect(status().isOk())
            .andExpect(jsonPath("$[*].institutionId",contains(alpha().intValue())))
            .andExpect(jsonPath("$[0].permissions",hasItem("MANAGE_MEMBERSHIPS")));
        mvc.perform(get("/api/me/institutions").session(login("pending"))).andExpect(content().json("[]"));
        mvc.perform(get("/api/me/institutions").session(login("editor"))).andExpect(jsonPath("$[0].permissions",not(hasItem("CREATE_UNIT"))));
        mvc.perform(get("/api/management/institutions").session(login("owner"))).andExpect(status().isForbidden());
    }
    @Test void ownerEditsLegalDataButCannotChangePlatformControlledStatus() throws Exception {
        var session=login("owner");
        mutate(patch(institutionPath()),session,"{\"legalName\":\"Razão corrigida\"}").andExpect(status().isOk());
        mutate(patch(institutionPath()),session,"{\"status\":\"SUSPENDED\"}").andExpect(status().isForbidden());
        mutate(patch(institutionPath()),login("admin"),"{\"status\":\"SUSPENDED\"}").andExpect(status().isOk());
    }
    @Test void managerEditsProfileButNotLegalDataMembershipsOrStatus() throws Exception {
        var session=login("manager");
        mutate(patch(institutionPath()),session,"{\"displayName\":\"Perfil atualizado\"}").andExpect(status().isOk());
        for(String body:List.of("{\"legalName\":\"x\"}","{\"cnpj\":\"12345678000195\"}","{\"status\":\"INACTIVE\"}"))
            mutate(patch(institutionPath()),session,body).andExpect(status().isForbidden());
        mvc.perform(get(institutionPath()+"/members").session(session)).andExpect(status().isForbidden());
    }
    @Test void editorIsRestrictedToPublicContentAndUnitDescription() throws Exception {
        var session=login("editor");
        mutate(patch(institutionPath()),session,"{\"objective\":\"Conteúdo\",\"history\":null}").andExpect(status().isOk());
        mutate(patch(unitPath()),session,"{\"description\":\"Descrição\"}").andExpect(status().isOk());
        for(String body:List.of("{\"status\":\"INACTIVE\"}","{\"name\":\"x\"}","{\"street\":\"x\"}","{\"document\":null}"))
            mutate(patch(unitPath()),session,body).andExpect(status().isForbidden());
        mutate(patch(institutionPath()),session,"{\"displayName\":\"x\"}").andExpect(status().isForbidden());
        mutate(post(institutionPath()+"/units"),session,unitRequest()).andExpect(status().isForbidden());
    }
    @Test void creationOfInstitutionRequiresPlatformAdminEvenForOwner() throws Exception {
        mutate(post("/api/institutions"),login("owner"),"{}").andExpect(status().isForbidden());
        mutate(post("/api/institutions"),login("admin"),"{}").andExpect(status().isBadRequest());
    }
    @Test void transitiveUnitAuthorizationChecksActualInstitution() throws Exception {
        var session=login("owner");
        mvc.perform(get("/api/management/units/"+foreignUnit()).session(session)).andExpect(status().isForbidden());
        mutate(patch("/api/units/"+foreignUnit()),session,"{\"description\":\"x\"}").andExpect(status().isForbidden());
        mutate(put("/api/units/"+foreignUnit()+"/opening-hours/1"),session,"{\"closed\":true}").andExpect(status().isForbidden());
        mutate(put("/api/units/"+foreignUnit()+"/service-areas"),session,"{\"serviceAreaIds\":[]}").andExpect(status().isForbidden());
    }
    @Test void needAuthorizationChecksBothUnitAndRealNeedParent() throws Exception {
        Long need=jdbc.queryForObject("SELECT id FROM needs WHERE title='Alimentos prioridade alta'",Long.class);
        var session=login("editor");
        mutate(patch(unitPath()+"/needs/"+need),session,"{\"status\":\"FULFILLED\"}").andExpect(status().isOk());
        mutate(patch("/api/units/"+foreignUnit()+"/needs/"+need),session,"{\"title\":\"x\"}").andExpect(status().isForbidden());
        mutate(patch(unitPath()+"/needs/"+need),login("other"),"{\"title\":\"x\"}").andExpect(status().isForbidden());
    }
    @Test void contactAuthorizationRejectsForgedParentsAndPrivateReads() throws Exception {
        Long general=jdbc.queryForObject("SELECT id FROM institution_contacts WHERE value='private@example.org'",Long.class);
        Long local=jdbc.queryForObject("SELECT id FROM unit_contacts WHERE value='unit-private@example.org'",Long.class);
        var session=login("owner");
        mutate(patch("/api/institutions/"+beta()+"/contacts/"+general),session,"{\"label\":\"x\"}").andExpect(status().isForbidden());
        mutate(patch("/api/units/"+foreignUnit()+"/contacts/"+local),session,"{\"label\":\"x\"}").andExpect(status().isForbidden());
        mvc.perform(get("/api/management/institutions/"+alpha()+"/contacts").session(login("other"))).andExpect(status().isForbidden());
        mutate(patch(institutionPath()+"/contacts/"+general),session,"{\"label\":\"Autorizado\"}").andExpect(status().isOk());
    }
    @Test void onlyOwnersAndAdminsManageMembershipsAndLastOwnerIsProtected() throws Exception {
        var session=login("owner");
        Long owner=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("owner").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow().getId();
        String path=institutionPath()+"/members/"+owner;
        mvc.perform(get(institutionPath()+"/members").session(session)).andExpect(status().isOk()).andExpect(jsonPath("$[0].user.passwordHash").doesNotExist());
        for(String body:List.of("{\"role\":\"EDITOR\"}","{\"status\":\"REVOKED\"}"))
            mutate(patch(path),session,body).andExpect(status().isConflict());
        mutate(patch(path),login("manager"),"{\"role\":\"EDITOR\"}").andExpect(status().isForbidden());
        mutate(patch(path),login("admin"),"{\"role\":\"EDITOR\"}").andExpect(status().isOk());
    }
    @Test void membershipRevocationTakesEffectOnExistingSession() throws Exception {
        var editor=login("editor");var owner=login("owner");
        Long id=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("editor").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow().getId();
        mutate(patch(institutionPath()+"/members/"+id),owner,"{\"status\":\"REVOKED\"}").andExpect(status().isOk());
        mutate(patch(institutionPath()),editor,"{\"description\":\"x\"}").andExpect(status().isForbidden());
    }
    @Test void suspendedAccountLosesExistingSessionAndAdminRoleChangesApplyImmediately() throws Exception {
        var owner=login("owner");accounts.get("owner").setStatus(UserStatus.SUSPENDED);users.flush();
        mvc.perform(get("/api/auth/me").session(owner)).andExpect(status().isUnauthorized());
        var admin=login("admin");accounts.get("admin").setPlatformRole(PlatformRole.USER);users.flush();
        mvc.perform(get("/api/admin/security-probe").session(admin)).andExpect(status().isForbidden());
    }

    @Test void ownerAndManagerCanCreateUnitsButOnlyOwnerChangesUnitStatus() throws Exception {
        for(String account:List.of("owner","manager"))
            mutate(post(institutionPath()+"/units"),login(account),unitRequest()).andExpect(status().isCreated());
        mutate(patch(unitPath()),login("manager"),"{\"status\":\"INACTIVE\"}").andExpect(status().isForbidden());
        mutate(patch(unitPath()),login("owner"),"{\"status\":\"INACTIVE\"}").andExpect(status().isOk());
    }
    @Test void historyImagesCannotBeChangedUsingForeignOrForgedInstitution() throws Exception {
        var owner=login("owner");
        String body=mutate(post(institutionPath()+"/history-images"),owner,
            "{\"imageUrl\":\"https://example.invalid/photo.jpg\",\"altText\":\"Foto de teste\",\"displayOrder\":0}")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Long image=((Number)JsonPath.read(body,"$.id")).longValue();
        mutate(patch("/api/institutions/"+beta()+"/history-images/"+image),owner,"{\"caption\":\"x\"}")
            .andExpect(status().isForbidden());
        mutate(delete(institutionPath()+"/history-images/"+image),login("other"),"{}").andExpect(status().isForbidden());
        mutate(patch(institutionPath()+"/history-images/"+image),login("editor"),"{\"caption\":\"Atualizada\"}").andExpect(status().isOk());
    }
    @Test void anOwnerCanTransferResponsibilityWithoutLeavingAnInstitutionOwnerless() throws Exception {
        var session=login("owner");
        Long manager=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("manager").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow().getId();
        Long owner=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("owner").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow().getId();
        mutate(patch(institutionPath()+"/members/"+manager),session,"{\"role\":\"OWNER\"}").andExpect(status().isOk());
        mutate(patch(institutionPath()+"/members/"+owner),session,"{\"status\":\"REVOKED\"}").andExpect(status().isOk());
        assertThat(memberships.countByInstitutionIdAndRoleAndStatus(alpha(),MembershipRole.OWNER,MembershipStatus.ACTIVE)).isEqualTo(1);
        mvc.perform(get(institutionPath()+"/members").session(session)).andExpect(status().isForbidden());
    }
    @Test void fullInstitutionPutDoesNotBypassRoleOrPlatformRestrictions() throws Exception {
        Long category=jdbc.queryForObject("SELECT category_id FROM institutions WHERE id=?",Long.class,alpha());
        var request=new com.bemfeito.api.institution.dto.InstitutionRequest(category,"Alpha Teste","Razão Alpha Teste",
            "11222333000181","Objetivo atualizado","Descrição preservada","História sem fotos","https://example.org",
            com.bemfeito.api.common.domain.InstitutionStatus.ACTIVE);
        String body=mapper.writeValueAsString(request);
        var manager=login("manager");var editor=login("editor");
        mutate(put(institutionPath()),manager,body).andExpect(status().isOk());
        mutate(put(institutionPath()),editor,body).andExpect(status().isOk());
        for(String account:List.of("manager","editor")) {
            var session=login(account);
            mutate(put(institutionPath()),session,body.replace("Razão Alpha Teste","Outra razão"))
                .andExpect(status().isForbidden());
            mutate(put(institutionPath()),session,body.replace("ACTIVE","SUSPENDED"))
                .andExpect(status().isForbidden());
        }
        mutate(put(institutionPath()),editor,body.replace("\"displayName\":\"Alpha Teste\"","\"displayName\":\"Outro nome\""))
            .andExpect(status().isForbidden());
    }
    @Test void fullUnitPutAllowsEditorDescriptionOnlyAndDoesNotBypassManagerLegalRestrictions() throws Exception {
        Long city=jdbc.queryForObject("SELECT city_id FROM units WHERE id=?",Long.class,unit());
        var request=new com.bemfeito.api.unit.dto.UnitRequest(city,"Unidade Luz",null,"Descrição atualizada","35595000",
            "Rua de Teste","10",null,"Centro",null,null,com.bemfeito.api.common.domain.UnitStatus.ACTIVE);
        String body=mapper.writeValueAsString(request);
        var editor=login("editor");var manager=login("manager");
        mutate(put(unitPath()),editor,body).andExpect(status().isOk());
        mutate(put(unitPath()),editor,body.replace("Rua de Teste","Outro logradouro")).andExpect(status().isForbidden());
        for(var session:List.of(editor,manager)) {
            mutate(put(unitPath()),session,body.replace("ACTIVE","INACTIVE")).andExpect(status().isForbidden());
            mutate(put(unitPath()),session,body.replace("\"document\":null","\"document\":\"12345678000195\""))
                .andExpect(status().isForbidden());
        }
        mutate(put(unitPath()),manager,body.replace("Rua de Teste","Outro logradouro")).andExpect(status().isOk());
    }
    @Test void apiErrorsRemainJsonForHtmlClientsAndBasicHeaderDoesNotAuthenticate() throws Exception {
        mvc.perform(get("/api/auth/me").accept("text/html")).andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith("application/json")).andExpect(header().doesNotExist("Location"));
        String basic=Base64.getEncoder().encodeToString(("owner@example.invalid:"+PASSWORD).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mvc.perform(get("/api/auth/me").header("Authorization","Basic "+basic)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/management/institutions").session(login("editor")).accept("text/html"))
            .andExpect(status().isForbidden()).andExpect(content().contentTypeCompatibleWith("application/json"))
            .andExpect(jsonPath("$.message",is("Acesso negado")))
            .andExpect(header().doesNotExist("Location"));
    }
    @Test void nonActiveAccountsDoNotCountAsReplacementForTheLastActiveOwner() throws Exception {
        var session=login("owner");
        var second=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("manager").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow();
        second.setRole(MembershipRole.OWNER);memberships.flush();
        Long owner=memberships.findByUserIdAndInstitutionIdAndStatus(accounts.get("owner").getId(),alpha(),MembershipStatus.ACTIVE).orElseThrow().getId();
        for(UserStatus state:List.of(UserStatus.INVITED,UserStatus.INACTIVE,UserStatus.SUSPENDED)) {
            accounts.get("manager").setStatus(state);users.flush();
            mutate(patch(institutionPath()+"/members/"+owner),session,"{\"status\":\"REVOKED\"}")
                .andExpect(status().isConflict());
            mutate(patch(institutionPath()+"/members/"+owner),session,"{\"role\":\"EDITOR\"}")
                .andExpect(status().isConflict());
        }
        accounts.get("manager").setStatus(UserStatus.ACTIVE);
        for(String unusableHash:Arrays.asList(null,"invalid-hash-test-only")) {
            accounts.get("manager").setPasswordHash(unusableHash);users.flush();
            mutate(patch(institutionPath()+"/members/"+owner),session,"{\"status\":\"REVOKED\"}")
                .andExpect(status().isConflict());
        }
        mutate(patch(institutionPath()+"/members/"+second.getId()),session,"{\"status\":\"REVOKED\"}")
            .andExpect(status().isOk());
    }
}
