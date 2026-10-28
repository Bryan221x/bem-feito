package com.bemfeito.api.requests.service;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.bemfeito.api.requests.dto.RequestInput;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.entity.AccountInvitationToken;
import com.bemfeito.api.requests.repository.AccountInvitationTokenRepository;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import static com.bemfeito.api.common.validation.RequestValidation.*;

@Service
@Transactional
@PreAuthorize("denyAll()")
public class InvitationService {
    private final AccountInvitationTokenRepository tokens;
    private final UserRepository users;
    private final BCryptPasswordEncoder passwords;
    private final ApplicationEventPublisher events;
    private final Duration ttl;
    private final String frontend;
    private final boolean mailEnabled;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final SecureRandom random=new SecureRandom();

    public InvitationService(AccountInvitationTokenRepository tokens,UserRepository users,BCryptPasswordEncoder passwords,
            ApplicationEventPublisher events,@Value("${app.invitation.ttl:48h}") Duration ttl,
            @Value("${app.frontend-base-url:http://127.0.0.1:5500}") String frontend,
            @Value("${app.mail.enabled:false}") boolean mailEnabled) {
        this.tokens=tokens;this.users=users;this.passwords=passwords;this.events=events;this.ttl=ttl;this.mailEnabled=mailEnabled;
        URI uri=URI.create(frontend);
        if (ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofDays(30))>0)
            throw new IllegalStateException("TTL do convite deve estar entre 1 nanossegundo e 30 dias");
        if (uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null
                || !("http".equals(uri.getScheme()) || "https".equals(uri.getScheme())))
            throw new IllegalStateException("APP_FRONTEND_BASE_URL deve ser URL HTTP(S) sem credenciais, query ou fragmento");
        this.frontend=frontend.replaceAll("/+$", "");
    }

    public static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 indisponível"); }
    }

    /** Administrative issue/reissue operations always serialize on the real user row. */
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Invitation issue(User user,String institutionName) {
        user=users.findForInvitation(user.getId()).orElseThrow(()->notFound("User"));
        entityManager.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(user.getStatus()!=UserStatus.INVITED) throw conflict("Somente contas INVITED podem receber convite de ativação");
        var now=LocalDateTime.now();tokens.invalidateUnused(user.getId(),now);
        byte[] entropy=new byte[32];random.nextBytes(entropy);
        String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        var token=new AccountInvitationToken();token.setUser(user);token.setTokenHash(hash(raw));token.setExpiresAt(now.plus(ttl));
        tokens.saveAndFlush(token);
        String url=frontend+"/pages/account-activation.html#token="+raw;
        if(mailEnabled) events.publishEvent(new InvitationMail(user.getId(),user.getEmail(),user.getName(),institutionName,url,token.getExpiresAt()));
        return new Invitation(user.getId(),mailEnabled?"EMAIL":"MANUAL",mailEnabled?null:url,token.getExpiresAt());
    }

    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public Invitation reissue(Long userId) {
        User user=users.findForInvitation(userId).orElseThrow(()->notFound("User"));
        return issue(user,"seus vínculos institucionais aprovados");
    }

    @Transactional(readOnly=true)
    @PreAuthorize("permitAll()")
    public Inspection inspect(RequestInput.Inspect request) {
        var token=lookup(request.token());
        if(token==null) return new Inspection("INVALID",null,null,null);
        String state=state(token);
        if(!state.equals("READY")) return new Inspection(state,null,null,null);
        String email=token.getUser().getEmail();int at=email.indexOf('@');
        String masked=email.substring(0,1)+"***"+(at<0?"":email.substring(at));
        return new Inspection(state,token.getUser().getName(),masked,token.getExpiresAt());
    }

    @PreAuthorize("permitAll()")
    public Map<String,String> accept(RequestInput.Accept request) {
        require(request.password()!=null && request.password().length()>=12
            && request.password().getBytes(StandardCharsets.UTF_8).length<=72,"Senha deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8");
        require(Objects.equals(request.password(),request.confirmPassword()),"Confirmação de senha divergente");
        var candidate=lookup(request.token());
        if(candidate==null) throw invalid();
        // All token mutations lock User first, then token, including concurrent reissues/accepts.
        User user=users.findForInvitation(candidate.getUser().getId()).orElseThrow(InvitationService::invalid);
        entityManager.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        var token=tokens.findForUse(hash(request.token())).orElseThrow(InvitationService::invalid);
        entityManager.refresh(token,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        if(!state(token).equals("READY") || user.getStatus()!=UserStatus.INVITED) throw invalid();
        user.setPasswordHash(passwords.encode(request.password()));user.setStatus(UserStatus.ACTIVE);user.setEmailVerifiedAt(LocalDateTime.now());
        token.setUsedAt(LocalDateTime.now());tokens.flush();tokens.invalidateUnused(user.getId(),LocalDateTime.now());
        users.flush();return Map.of("state","SUCCESS");
    }

    private AccountInvitationToken lookup(String raw) {
        if(raw==null || !raw.matches("[A-Za-z0-9_-]{43}")) return null;
        var token=tokens.findByTokenHash(hash(raw)).orElse(null);
        if(token!=null) entityManager.refresh(token); // Observe bulk invalidations even in a surrounding transaction.
        return token;
    }
    private String state(AccountInvitationToken token) {
        if(token.getUsedAt()!=null) return "ALREADY_USED";
        if(!token.getExpiresAt().isAfter(LocalDateTime.now())) return "EXPIRED";
        return token.getUser().getStatus()==UserStatus.INVITED?"READY":"INVALID";
    }
    private static ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Convite inválido ou indisponível; solicite nova emissão"); }
    private static ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
}
