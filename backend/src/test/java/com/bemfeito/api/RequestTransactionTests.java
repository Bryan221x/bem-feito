package com.bemfeito.api;

import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.bemfeito.api.requests.dto.RequestInput.*;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.service.*;
import com.bemfeito.api.requests.repository.*;
import com.bemfeito.api.requests.entity.*;
import com.bemfeito.api.security.UserPrincipal;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Committed transactions/concurrent connections, exclusively in a dedicated H2 database. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:request-transactions-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000",
    "app.mail.enabled=true","app.mail.from=noreply@example.invalid"})
@Sql("/institutional-fixtures.sql")
@Sql(scripts="/request-cleanup.sql",executionPhase=Sql.ExecutionPhase.AFTER_TEST_METHOD)
class RequestTransactionTests {
    @Autowired RequestSubmissionService submissions;
    @Autowired RequestReviewService reviews;
    @Autowired InvitationService invitations;
    @Autowired AccountInvitationTokenRepository tokens;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired PlatformTransactionManager manager;
    @MockitoBean JavaMailSender sender;
    private User admin;
    private static final String PASSWORD=UUID.randomUUID().toString();
    private static final String HASH=new BCryptPasswordEncoder(4).encode(PASSWORD);
    @BeforeEach void setup() { admin=account("transaction-admin@example.invalid",UserStatus.ACTIVE,PlatformRole.PLATFORM_ADMIN);authenticate(); }
    @AfterEach void clear() { SecurityContextHolder.clearContext(); }
    private User account(String email,UserStatus status,PlatformRole role) { var u=new User();u.setName("Conta H2");u.setEmail(email);u.setStatus(status);u.setPlatformRole(role);u.setPasswordHash(status==UserStatus.INVITED?null:HASH);return users.saveAndFlush(u); }
    private void authenticate() { var p=new UserPrincipal(admin);SecurityContextHolder.getContext().setAuthentication(UsernamePasswordAuthenticationToken.authenticated(p,null,p.getAuthorities())); }
    private Long id(String table,String field,String value) {return jdbc.queryForObject("SELECT id FROM "+table+" WHERE "+field+"=?",Long.class,value);}
    private long count(String table) {return jdbc.queryForObject("SELECT COUNT(*) FROM "+table,Long.class);}
    private Registration registration() { return new Registration(id("institution_categories","name","Educação"),"Instituição transação H2","Razão transação H2","04252011000110",null,null,null,null,
        new Responsible("Pessoa H2","transaction-person@example.invalid","31999990000","Diretoria"),List.of(new Contact(com.bemfeito.api.common.domain.ContactType.EMAIL,"public@example.invalid",null,true)),
        new MainUnit("Unidade aprovação H2",null,null,id("cities","name","Luz"),"35595000","Rua H2","1",null,"Centro",List.of())); }
    private Reviewed approve(Long id) { return reviews.registration(id,"approve",new Decision("Conferido",null)); }
    private String raw(Invitation invitation) {
        // SMTP mode never returns the URL: inspect the mocked message only in this test.
        var captured=org.mockito.ArgumentCaptor.forClass(SimpleMailMessage.class);verify(sender,atLeastOnce()).send(captured.capture());
        String body=captured.getValue().getText();return body.substring(body.indexOf("#token=")+7).split("\\n")[0];
    }
    @Test void databaseFailureRollsBackEntireApproval() {
        Long id=submissions.register(registration()).id();long institutions=count("institutions"),units=count("units"),usersBefore=count("users");
        jdbc.execute("ALTER TABLE units ADD CONSTRAINT test_failed_unit CHECK(name <> 'Unidade aprovação H2')");
        try { assertThatThrownBy(()->approve(id)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class); }
        finally { jdbc.execute("ALTER TABLE units DROP CONSTRAINT test_failed_unit"); }
        assertThat(count("institutions")).isEqualTo(institutions);assertThat(count("units")).isEqualTo(units);assertThat(count("users")).isEqualTo(usersBefore);
        assertThat(count("account_invitation_tokens")).isZero();assertThat(count("institution_memberships")).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM registration_requests WHERE id=?",String.class,id)).isEqualTo("PENDING");verifyNoInteractions(sender);
    }
    @Test void lateUserConflictRollsBackCopiedInstitutionAndUnit() {
        account("transaction-person@example.invalid",UserStatus.SUSPENDED,PlatformRole.USER);Long id=submissions.register(registration()).id();
        assertThatThrownBy(()->approve(id)).isInstanceOf(org.springframework.web.server.ResponseStatusException.class);
        assertThat(count("institutions")).isEqualTo(3);assertThat(count("units")).isEqualTo(4);assertThat(count("account_invitation_tokens")).isZero();
        assertThat(jdbc.queryForObject("SELECT status FROM registration_requests WHERE id=?",String.class,id)).isEqualTo("PENDING");
    }
    @Test void emailRunsOnlyAfterCommitAndManualUrlIsAbsentInSmtpMode() {
        var tx=new TransactionTemplate(manager);
        Reviewed result=tx.execute(s->{var created=submissions.register(registration());var reviewed=approve(created.id());verifyNoInteractions(sender);return reviewed;});
        verify(sender,times(1)).send(any(SimpleMailMessage.class));assertThat(result.invitation().activationUrl()).isNull();assertThat(result.invitation().deliveryMode()).isEqualTo("EMAIL");
        String raw=raw(result.invitation());assertThat(raw).matches("[A-Za-z0-9_-]{43}");
        assertThat(tokens.findByTokenHash(InvitationService.hash(raw))).isPresent();
    }
    @Test void mailFailureDoesNotUndoApprovalAndReissueWorks() {
        doThrow(new org.springframework.mail.MailSendException("test failure without SMTP")).when(sender).send(any(SimpleMailMessage.class));
        Long id=submissions.register(registration()).id();Reviewed approved=approve(id);
        assertThat(approved.status()).isEqualTo(RequestStatus.APPROVED);assertThat(count("institution_memberships")).isEqualTo(1);
        reset(sender);var reissued=invitations.reissue(approved.userId());verify(sender).send(any(SimpleMailMessage.class));
        assertThat(reissued.userId()).isEqualTo(approved.userId());assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_invitation_tokens WHERE used_at IS NULL",Long.class)).isEqualTo(1);
    }
    @Test void rollbackNeverSendsEmailOrPersistsTokens() {
        var tx=new TransactionTemplate(manager);tx.executeWithoutResult(s->{var request=submissions.register(registration());approve(request.id());s.setRollbackOnly();});
        verifyNoInteractions(sender);assertThat(count("registration_requests")).isZero();assertThat(count("account_invitation_tokens")).isZero();assertThat(count("institutions")).isEqualTo(3);
    }
    private List<Boolean> race(Callable<Boolean> action) throws Exception {
        var gate=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try {
            var tasks=new ArrayList<Future<Boolean>>();for(int n=0;n<2;n++) tasks.add(pool.submit(()->{authenticate();try {gate.await();return action.call();}finally {SecurityContextHolder.clearContext();}}));
            gate.countDown();return List.of(tasks.get(0).get(25,TimeUnit.SECONDS),tasks.get(1).get(25,TimeUnit.SECONDS));
        } finally {pool.shutdownNow();}
    }
    @Test void concurrentApprovalCreatesExactlyOneAggregate() throws Exception {
        Long id=submissions.register(registration()).id();var results=race(()->{try {approve(id);return true;}catch(org.springframework.web.server.ResponseStatusException e){assertThat(e.getStatusCode().value()).isEqualTo(409);return false;}});
        assertThat(results).containsExactlyInAnyOrder(true,false);assertThat(count("institutions")).isEqualTo(4);assertThat(count("users")).isEqualTo(2);assertThat(count("institution_memberships")).isEqualTo(1);assertThat(count("account_invitation_tokens")).isEqualTo(1);
    }
    @Test void concurrentDuplicateSubmissionsHitUniqueConstraint() throws Exception {
        var results=race(()->{try {submissions.register(registration());return true;}catch(org.springframework.dao.DataIntegrityViolationException|org.springframework.web.server.ResponseStatusException e){return false;}});
        assertThat(results).containsExactlyInAnyOrder(true,false);assertThat(count("registration_requests")).isEqualTo(1);
    }
    @Test void concurrentAcceptCannotReuseToken() throws Exception {
        var approved=approve(submissions.register(registration()).id());String raw=raw(approved.invitation());
        var results=race(()->{try {invitations.accept(new Accept(raw,PASSWORD,PASSWORD));return true;}catch(org.springframework.web.server.ResponseStatusException e){return false;}});
        assertThat(results).containsExactlyInAnyOrder(true,false);assertThat(users.findById(approved.userId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
    }
    @Test void concurrentReissueLeavesOnlyOneUsableToken() throws Exception {
        var approved=approve(submissions.register(registration()).id());clearInvocations(sender);
        var results=race(()->{invitations.reissue(approved.userId());return true;});assertThat(results).containsOnly(true);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM account_invitation_tokens WHERE used_at IS NULL",Long.class)).isEqualTo(1);verify(sender,times(2)).send(any(SimpleMailMessage.class));
    }
}
