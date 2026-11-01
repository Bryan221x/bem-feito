package com.bemfeito.api.security.recovery;
import java.time.*;
import java.security.SecureRandom;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import com.bemfeito.api.requests.service.*;
import com.bemfeito.api.requests.dto.RequestOutput.Invitation;
import static com.bemfeito.api.common.validation.RequestValidation.require;
@Service @Transactional @PreAuthorize("permitAll()")
public class PasswordRecoveryService {
    private final UserRepository users; private final PasswordResetRepository tokens;
    private final BCryptPasswordEncoder passwords; private final ApplicationEventPublisher events;
    private final boolean enabled; private final String frontend; private final Duration ttl;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager entityManager;
    private final SecureRandom random=new SecureRandom();
    public PasswordRecoveryService(UserRepository users,PasswordResetRepository tokens,BCryptPasswordEncoder passwords,
        ApplicationEventPublisher events,@Value("${app.mail.enabled:false}") boolean enabled,
        @Value("${app.frontend-base-url:http://127.0.0.1:5500}") String frontend,@Value("${app.password-reset.ttl:15m}") Duration ttl) {
        this.users=users;this.tokens=tokens;this.passwords=passwords;this.events=events;this.enabled=enabled;this.frontend=frontend.replaceAll("/+$","");this.ttl=ttl;
        if(ttl.isNegative() || ttl.isZero() || ttl.compareTo(Duration.ofMinutes(30))>0) throw new IllegalStateException("TTL de recuperação deve ser positivo e no máximo 30 minutos");
    }
    @org.springframework.scheduling.annotation.Async("passwordRecoveryExecutor")
    public java.util.concurrent.CompletableFuture<Void> request(String email) {
        if(!enabled) return java.util.concurrent.CompletableFuture.completedFuture(null);
        var user=users.findForInvitationByEmail(User.normalizeEmail(email)).orElse(null);
        if(user==null || user.getStatus()!=UserStatus.ACTIVE || !user.hasUsablePasswordHash()) return java.util.concurrent.CompletableFuture.completedFuture(null);
        var now=LocalDateTime.now();
        if(tokens.existsByUserIdAndCreatedAtAfter(user.getId(),now.minusMinutes(1))) return java.util.concurrent.CompletableFuture.completedFuture(null);
        tokens.invalidate(user.getId(),now);
        byte[] entropy=new byte[32];random.nextBytes(entropy);String raw=Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);
        var token=new PasswordResetToken();token.user=user;token.tokenHash=InvitationService.hash(raw);token.createdAt=now;token.expiresAt=now.plus(ttl);tokens.saveAndFlush(token);
        String url=frontend+"/pages/password-recovery.html#token="+raw;
        var receipt=new Invitation(user.getId(),"PENDING",null,token.expiresAt,"RESET");
        events.publishEvent(new InvitationMail(user.getId(),user.getEmail(),user.getName(),"",url,token.expiresAt,receipt));
        return java.util.concurrent.CompletableFuture.completedFuture(null);
    }
    public void reset(String raw,String password,String confirmation) {
        require(raw!=null && raw.matches("[A-Za-z0-9_-]{43}"),"Link inválido ou expirado. Solicite outro link.");
        require(password!=null && password.length()>=12 && password.getBytes(StandardCharsets.UTF_8).length<=72,"Senha deve ter pelo menos 12 caracteres e no máximo 72 bytes UTF-8");
        require(Objects.equals(password,confirmation),"Confirmação de senha divergente");
        var candidate=tokens.findByTokenHash(InvitationService.hash(raw)).orElse(null);
        require(candidate!=null,"Link inválido ou expirado. Solicite outro link.");
        var user=users.findForInvitation(candidate.user.getId()).orElseThrow();
        entityManager.refresh(user,jakarta.persistence.LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(candidate);
        require(candidate.usedAt==null && candidate.expiresAt.isAfter(LocalDateTime.now()) && user.getStatus()==UserStatus.ACTIVE,"Link inválido ou expirado. Solicite outro link.");
        user.setPasswordHash(passwords.encode(password));user.invalidateSessions();users.flush();tokens.invalidate(user.getId(),LocalDateTime.now());
    }
}
