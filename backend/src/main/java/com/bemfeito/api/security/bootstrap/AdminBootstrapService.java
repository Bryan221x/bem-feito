package com.bemfeito.api.security.bootstrap;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;

@Service
public class AdminBootstrapService {
    private final UserRepository users;
    private final BCryptPasswordEncoder encoder;
    private final Validator validator;
    public AdminBootstrapService(UserRepository users,BCryptPasswordEncoder encoder,Validator validator) {
        this.users=users;this.encoder=encoder;this.validator=validator;
    }
    private record Identity(@NotBlank @Size(max=150) String name,@NotBlank @Email @Size(max=255) String email) {}
    @Transactional
    public void initialize(String name,String email,String password) {
        String normalizedEmail=User.normalizeEmail(email);
        if(!validator.validate(new Identity(name==null?null:name.trim(),normalizedEmail)).isEmpty()
            || password==null || password.length()<12 || password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new IllegalStateException("Bootstrap exige nome, e-mail válido e senha de 12 caracteres ou mais, com até 72 bytes UTF-8");
        var existing=users.findByEmail(normalizedEmail);
        if(existing.isPresent()) {
            if(existing.get().getPlatformRole()!=PlatformRole.PLATFORM_ADMIN || existing.get().getStatus()!=UserStatus.ACTIVE)
                throw new IllegalStateException("Bootstrap não altera a role/status de conta existente");
            return; // Nunca redefine senha, nome ou verificação de uma conta existente.
        }
        if(users.existsByPlatformRole(PlatformRole.PLATFORM_ADMIN))
            throw new IllegalStateException("O primeiro administrador já foi configurado; desabilite o bootstrap");
        var user=new User();user.setName(name.trim());user.setEmail(normalizedEmail);
        user.setPasswordHash(encoder.encode(password));user.setPlatformRole(PlatformRole.PLATFORM_ADMIN);
        user.setStatus(UserStatus.ACTIVE);user.setEmailVerifiedAt(LocalDateTime.now());users.saveAndFlush(user);
    }
}
