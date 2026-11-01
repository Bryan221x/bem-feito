package com.bemfeito.api;

import java.util.*;
import java.util.concurrent.*;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import com.bemfeito.api.requests.service.InvitationService;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

/** SMTP capture bound only to loopback and ephemeral H2. Never sends external mail. */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:recovery-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "app.mail.enabled=true","app.mail.from=noreply@example.invalid","spring.mail.properties.mail.smtp.auth=false","spring.mail.properties.mail.smtp.starttls.enable=false"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class PasswordRecoveryTests {
    static final Capture SMTP=new Capture();
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.mail.host",()->"127.0.0.1");registry.add("spring.mail.port",()->SMTP.server.getLocalPort());
    }
    @Autowired InvitationService invitations;
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired JdbcTemplate jdbc;
    @Autowired BCryptPasswordEncoder encoder; @Autowired tools.jackson.databind.ObjectMapper mapper;
    private String email,password; private User account;
    @BeforeEach void setup() {
        SMTP.messages.clear();email=UUID.randomUUID()+"@example.invalid";password=UUID.randomUUID().toString();
        account=new User();account.setName("Conta isolada");account.setEmail(email);account.setStatus(UserStatus.ACTIVE);account.setPlatformRole(PlatformRole.USER);account.setPasswordHash(encoder.encode(password));users.saveAndFlush(account);
    }
    private org.springframework.test.web.servlet.ResultActions postJson(String path,Object body) throws Exception {
        return mvc.perform(post(path).with(csrf()).with(r->{r.setRemoteAddr(UUID.randomUUID().toString());return r;}).contentType("application/json").content(mapper.writeValueAsString(body)));
    }
    private String requestToken() throws Exception {
        postJson("/api/password-recovery/request",Map.of("email",email)).andExpect(status().isOk());
        MimeMessage message=SMTP.messages.poll(5,TimeUnit.SECONDS);assertThat(message).isNotNull();
        String body=RequestTransactionTests.mailText(message);var matcher=java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(body);
        assertThat(matcher.find()).isTrue();return matcher.group(1);
    }
    private MockHttpSession login(String value) throws Exception {
        return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",value)).andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }
    @Test void capturedMailResetsPasswordRejectsReuseAndInvalidatesOldSession() throws Exception {
        var session=login(password);String raw=requestToken(),next=UUID.randomUUID().toString();
        String stored=jdbc.queryForObject("select token_hash from password_reset_tokens where user_id=?",String.class,account.getId());
        assertThat(stored.equals(InvitationService.hash(raw))).isTrue();assertThat(stored.equals(raw)).isFalse();
        var body=Map.of("token",raw,"password",next,"confirmPassword",next);
        postJson("/api/password-recovery/reset",body).andExpect(status().isOk());
        postJson("/api/password-recovery/reset",body).andExpect(status().isBadRequest());
        mvc.perform(get("/api/auth/me").session(session)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").with(csrf()).param("email",email).param("password",password)).andExpect(status().isUnauthorized());
        login(next);
    }
    @Test void unknownAndKnownAccountsHaveSamePublicResponseAndCooldown() throws Exception {
        String unknown=postJson("/api/password-recovery/request",Map.of("email","missing@example.invalid")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String known=postJson("/api/password-recovery/request",Map.of("email",email)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertThat(known).isEqualTo(unknown);assertThat(SMTP.messages.poll(5,TimeUnit.SECONDS)).isNotNull();
        postJson("/api/password-recovery/request",Map.of("email",email)).andExpect(status().isOk());assertThat(SMTP.messages).isEmpty();
    }
    @Test void expiredTokenInvalidTokenAndPasswordValidation() throws Exception {
        String token=requestToken();
        postJson("/api/password-recovery/reset",Map.of("token",token,"password","short","confirmPassword","short")).andExpect(status().isBadRequest());
        jdbc.update("update password_reset_tokens set expires_at=? where user_id=?",java.time.LocalDateTime.now().minusMinutes(1),account.getId());
        postJson("/api/password-recovery/reset",Map.of("token",token,"password",password,"confirmPassword",password)).andExpect(status().isBadRequest());
        postJson("/api/password-recovery/reset",Map.of("token","invalid","password",password,"confirmPassword",password)).andExpect(status().isBadRequest());
    }
    @Test void activationAndExistingAccountNotificationReachLocalSmtp() throws Exception {
        var principal=org.springframework.security.core.userdetails.User.withUsername("test-admin").password("unused").roles("PLATFORM_ADMIN").build();
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(principal,null,principal.getAuthorities()));
        try {
            var invited=new User();invited.setEmail(UUID.randomUUID()+"@example.invalid");invited.setName("Convite isolado");invited.setStatus(UserStatus.INVITED);invited.setPlatformRole(PlatformRole.USER);users.saveAndFlush(invited);
            var result=invitations.issue(invited,"Instituição H2");assertThat(result.deliveryMode()).isEqualTo("EMAIL_SENT");assertThat(result.activationUrl()).isNull();
            var mail=SMTP.messages.poll(5,TimeUnit.SECONDS);assertThat(mail).isNotNull();String text=RequestTransactionTests.mailText(mail);
            var match=java.util.regex.Pattern.compile("#token=([A-Za-z0-9_-]{43})").matcher(text);assertThat(match.find()).isTrue();
            invitations.accept(new com.bemfeito.api.requests.dto.RequestInput.Accept(match.group(1),password,password));
            assertThat(users.findById(invited.getId()).orElseThrow().getStatus()).isEqualTo(UserStatus.ACTIVE);
            String hash=account.getPasswordHash();var notice=invitations.notifyApproval(account,"Instituição H2");
            assertThat(notice.deliveryMode()).isEqualTo("EMAIL_SENT");assertThat(SMTP.messages.poll(5,TimeUnit.SECONDS)).isNotNull();
            assertThat(users.findById(account.getId()).orElseThrow().getPasswordHash().equals(hash)).isTrue();
        } finally { org.springframework.security.core.context.SecurityContextHolder.clearContext(); }
    }
    @Test void csrfAndRateLimitRemainRequired() throws Exception {
        mvc.perform(post("/api/password-recovery/request").contentType("application/json").content("{\"email\":\"missing@example.invalid\"}")).andExpect(status().isForbidden());
        String ip=UUID.randomUUID().toString();
        for(int n=0;n<6;n++) mvc.perform(post("/api/password-recovery/request").with(csrf()).with(r->{r.setRemoteAddr(ip);return r;}).contentType("application/json").content("{\"email\":\"missing@example.invalid\"}"))
            .andExpect(status().is(n<5?200:429));
    }
    @AfterAll static void close() throws Exception { SMTP.server.close(); }
    static final class Capture {
        final ServerSocket server; final BlockingQueue<MimeMessage> messages=new LinkedBlockingQueue<>();
        Capture() {
            try { server=new ServerSocket(0,20,InetAddress.getLoopbackAddress()); }
            catch(IOException e) { throw new IllegalStateException(e); }
            Thread.ofPlatform().daemon().start(()->{while(!server.isClosed()) {try {handle(server.accept());}catch(Exception ignored) {}}});
        }
        void handle(Socket socket) throws Exception {
            try(socket;var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));var writer=new PrintWriter(socket.getOutputStream(),true,StandardCharsets.US_ASCII)) {
                writer.print("220 local-capture\r\n");writer.flush();String line;
                while((line=reader.readLine())!=null) {
                    if(line.startsWith("DATA")) {
                        writer.print("354 send data\r\n");writer.flush();var body=new StringBuilder();
                        while((line=reader.readLine())!=null && !line.equals(".")) body.append(line.startsWith("..")?line.substring(1):line).append("\r\n");
                        messages.add(new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(body.toString().getBytes(StandardCharsets.US_ASCII))));
                        writer.print("250 accepted\r\n");
                    } else if(line.startsWith("QUIT")) {writer.print("221 bye\r\n");writer.flush();return;}
                    else writer.print("250 OK\r\n");
                    writer.flush();
                }
            }
        }
    }
}
