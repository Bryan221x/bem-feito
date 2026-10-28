package com.bemfeito.api;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.security.bootstrap.AdminBootstrapService;
import com.bemfeito.api.security.bootstrap.AdminBootstrapRunner;
import com.bemfeito.api.user.entity.*;
import com.bemfeito.api.user.repository.UserRepository;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties={
    "spring.datasource.url=jdbc:h2:mem:bootstrap-${random.uuid};MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1",
    "spring.datasource.username=sa","spring.datasource.password=","spring.sql.init.mode=always",
    "spring.sql.init.schema-locations=classpath:reference-schema.sql","spring.sql.init.data-locations=classpath:reference-fixtures.sql",
    "app.bootstrap-admin.enabled=false"})
@Transactional
class AdminBootstrapTests {
    @Autowired AdminBootstrapService bootstrap;
    @Autowired UserRepository users;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired ApplicationContext context;

    @Test void bootstrapIsDisabledAndDoesNotCreateAccountsByDefault() {
        assertThat(context.getBeansOfType(AdminBootstrapRunner.class)).isEmpty();
        assertThat(users.count()).isZero();
    }
    @Test void explicitBootstrapCreatesVerifiedActiveAdminWithBcryptAndNormalizesEmail() {
        String secret=UUID.randomUUID().toString();
        bootstrap.initialize("Admin Teste","  FIRST@EXAMPLE.INVALID ",secret);
        var admin=users.findByEmail("first@example.invalid").orElseThrow();
        assertThat(admin.getPlatformRole()).isEqualTo(PlatformRole.PLATFORM_ADMIN);
        assertThat(admin.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(admin.getEmailVerifiedAt()).isNotNull();
        assertThat(admin.getPasswordHash()).startsWith("$2a$12$").isNotEqualTo(secret);
        assertThat(encoder.matches(secret,admin.getPasswordHash())).isTrue();
    }
    @Test void bootstrapIsIdempotentAndNeverResetsExistingPassword() {
        String first=UUID.randomUUID().toString();
        bootstrap.initialize("Admin Teste","first@example.invalid",first);
        String original=users.findByEmail("first@example.invalid").orElseThrow().getPasswordHash();
        bootstrap.initialize("Outro nome","FIRST@EXAMPLE.INVALID",UUID.randomUUID().toString());
        assertThat(users.count()).isEqualTo(1);
        assertThat(users.findByEmail("first@example.invalid").orElseThrow().getPasswordHash()).isEqualTo(original);
    }
    @Test void bootstrapRejectsMissingOrInvalidConfigurationWithoutPersistingAccounts() {
        assertThatThrownBy(()->bootstrap.initialize(null,null,null)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->bootstrap.initialize("Admin","invalid",UUID.randomUUID().toString())).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(()->bootstrap.initialize("Admin","first@example.invalid","x".repeat(73))).isInstanceOf(IllegalStateException.class);
        assertThat(users.count()).isZero();
    }
    @Test void bootstrapDoesNotPromoteExistingUserOrCreateSecondAdministrator() {
        var user=new User();user.setName("Teste");user.setEmail("existing@example.invalid");
        user.setStatus(UserStatus.ACTIVE);user.setPlatformRole(PlatformRole.USER);users.saveAndFlush(user);
        assertThatThrownBy(()->bootstrap.initialize("Admin","existing@example.invalid",UUID.randomUUID().toString())).isInstanceOf(IllegalStateException.class);
        bootstrap.initialize("Primeiro","first@example.invalid",UUID.randomUUID().toString());
        assertThatThrownBy(()->bootstrap.initialize("Segundo","second@example.invalid",UUID.randomUUID().toString())).isInstanceOf(IllegalStateException.class);
        assertThat(users.count()).isEqualTo(2);
    }
}
