package com.bemfeito.api;

import org.springframework.boot.SpringApplication;

/** Opt-in browser tests use an isolated, ephemeral H2 database, never MySQL. */
public final class BrowserTestServer {
    private BrowserTestServer() {}
    public static void main(String[] args) {
        System.setProperty("spring.datasource.url", "jdbc:h2:mem:browser;MODE=MySQL;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE;DB_CLOSE_DELAY=-1");
        System.setProperty("spring.datasource.username", "sa");
        System.setProperty("spring.datasource.password", "");
        System.setProperty("spring.datasource.driver-class-name", "org.h2.Driver");
        System.setProperty("spring.sql.init.mode", "always");
        System.setProperty("spring.sql.init.schema-locations", "classpath:reference-schema.sql");
        System.setProperty("spring.sql.init.data-locations", "classpath:reference-fixtures.sql");
        System.setProperty("spring.jpa.hibernate.ddl-auto", "validate");
        System.setProperty("server.port", "8081");
        System.setProperty("app.bootstrap-admin.enabled", "false");
        System.setProperty("app.cors.allowed-origins", "http://127.0.0.1:5501");
        System.setProperty("app.frontend-base-url", "http://127.0.0.1:5501");
        System.setProperty("app.upload.dir", "target/browser-test-uploads");
        System.setProperty("app.mail.enabled", "false");
        String smtpPort=System.getenv("TEST_SMTP_PORT");
        if(smtpPort!=null) {
            System.setProperty("app.mail.enabled","true");System.setProperty("app.mail.from","noreply@example.invalid");
            System.setProperty("spring.mail.host","127.0.0.1");System.setProperty("spring.mail.port",smtpPort);
            System.setProperty("spring.mail.properties.mail.smtp.auth","false");System.setProperty("spring.mail.properties.mail.smtp.starttls.enable","false");
            System.setProperty("spring.mail.properties.mail.smtp.starttls.required","false");System.setProperty("spring.mail.properties.mail.smtp.ssl.enable","false");
        }
        var context=SpringApplication.run(BemFeitoApiApplication.class, args);
        // Fixture exclusiva do H2 efêmero: não executa o bootstrap operacional.
        var admin=new com.bemfeito.api.user.entity.User();
        admin.setName(System.getenv("TEST_ADMIN_NAME"));admin.setEmail(System.getenv("TEST_ADMIN_EMAIL"));
        admin.setPasswordHash(context.getBean(org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder.class).encode(System.getenv("TEST_ADMIN_PASSWORD")));
        admin.setPlatformRole(com.bemfeito.api.user.entity.PlatformRole.PLATFORM_ADMIN);
        admin.setStatus(com.bemfeito.api.user.entity.UserStatus.ACTIVE);
        admin.setEmailVerifiedAt(java.time.LocalDateTime.now());
        context.getBean(com.bemfeito.api.user.repository.UserRepository.class).saveAndFlush(admin);
        System.out.println("BEMFEITO_BROWSER_TEST_READY");
    }
}
