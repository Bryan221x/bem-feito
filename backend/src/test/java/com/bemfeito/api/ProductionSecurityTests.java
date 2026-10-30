package com.bemfeito.api;

import com.bemfeito.api.security.*;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.*;
import tools.jackson.databind.ObjectMapper;
import static org.assertj.core.api.Assertions.*;

class ProductionSecurityTests {
    private MockEnvironment environment() {
        return new MockEnvironment().withProperty("server.servlet.session.cookie.secure", "true")
            .withProperty("server.servlet.session.cookie.http-only", "true")
            .withProperty("server.servlet.session.cookie.same-site", "lax")
            .withProperty("app.abuse-protection.enabled", "true")
            .withProperty("spring.jpa.hibernate.ddl-auto", "validate")
            .withProperty("spring.datasource.username", "restricted")
            .withProperty("spring.datasource.password", java.util.UUID.randomUUID().toString())
            .withProperty("app.frontend-base-url", "https://example.invalid")
            .withProperty("app.cors.allowed-origins", "https://example.invalid")
            .withProperty("app.upload.dir", java.nio.file.Path.of("target/uploads").toAbsolutePath().toString());
    }
    @Test void acceptsSecureProductionConfiguration() { ProductionConfiguration.validate(environment()); }
    @Test void rejectsInsecureCookieBootstrapRootAndSchemaMutation() {
        for (var entry : java.util.Map.of("server.servlet.session.cookie.secure", "false",
                "app.bootstrap-admin.enabled", "true", "spring.datasource.username", "root",
                "spring.jpa.hibernate.ddl-auto", "update", "app.abuse-protection.enabled", "false").entrySet()) {
            assertThatThrownBy(() -> ProductionConfiguration.validate(environment().withProperty(entry.getKey(), entry.getValue())))
                .isInstanceOf(IllegalStateException.class);
        }
    }
    @Test void rejectsUntrustedOriginsAndPlainSmtp() {
        for (String origin : java.util.List.of("http://example.invalid", "https://*.example.invalid", "https://example.invalid/path", "https://user@example.invalid")) {
            assertThatThrownBy(() -> ProductionConfiguration.validate(environment().withProperty("app.cors.allowed-origins", origin)))
                .isInstanceOf(IllegalStateException.class);
        }
        assertThatThrownBy(() -> ProductionConfiguration.validate(environment().withProperty("app.mail.enabled", "true")))
            .isInstanceOf(IllegalStateException.class);
    }
    private MockHttpServletResponse request(AbuseProtectionFilter filter, String ip, String method, String path) throws Exception {
        var request = new MockHttpServletRequest(method, path); request.setRemoteAddr(ip);
        request.addHeader("X-Forwarded-For", java.util.UUID.randomUUID().toString());
        var response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain()); return response;
    }
    @Test void boundsLoginRegardlessOfSpoofedForwardedHeaderAndSeparatesClients() throws Exception {
        var filter = new AbuseProtectionFilter(new SecurityJson(new ObjectMapper()), true);
        for (int i = 0; i < 10; i++) assertThat(request(filter,"127.0.0.1","POST","/api/auth/login").getStatus()).isEqualTo(200);
        var denied = request(filter,"127.0.0.1","POST","/api/auth/login");
        assertThat(denied.getStatus()).isEqualTo(429); assertThat(denied.getHeader("Retry-After")).isEqualTo("60");
        assertThat(request(filter,"127.0.0.2","POST","/api/auth/login").getStatus()).isEqualTo(200);
        assertThat(request(filter,"127.0.0.1","GET","/api/institutions").getStatus()).isEqualTo(200);
    }
    @Test void publicFormsShareBudgetAndLocalDevelopmentCanDisableIt() throws Exception {
        var filter = new AbuseProtectionFilter(new SecurityJson(new ObjectMapper()), true);
        for (int i = 0; i < 10; i++) request(filter,"127.0.0.1","POST","/api/registration-requests");
        assertThat(request(filter,"127.0.0.1","POST","/api/access-requests").getStatus()).isEqualTo(429);
        var local = new AbuseProtectionFilter(new SecurityJson(new ObjectMapper()), false);
        for (int i = 0; i < 12; i++) assertThat(request(local,"127.0.0.1","POST","/api/auth/login").getStatus()).isEqualTo(200);
    }
    @Test void windowExpiresWithoutWaiting() throws Exception {
        var time = new java.util.concurrent.atomic.AtomicLong(0);
        Clock clock = new Clock() {
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return Instant.ofEpochMilli(time.get()); }
        };
        var filter = new AbuseProtectionFilter(new SecurityJson(new ObjectMapper()), true, clock);
        for (int i = 0; i < 11; i++) request(filter,"127.0.0.1","POST","/api/auth/login");
        time.set(60000);
        assertThat(request(filter,"127.0.0.1","POST","/api/auth/login").getStatus()).isEqualTo(200);
    }
}
