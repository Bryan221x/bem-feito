package com.bemfeito.api.security;

import java.net.URI;
import java.nio.file.Path;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

/** Fail before serving traffic when the production profile is unsafe. Never echo values. */
@Configuration
@Profile("prod")
public class ProductionConfiguration {
    public ProductionConfiguration(Environment env) { validate(env); }
    public static void validate(Environment env) {
        require(env.getProperty("server.servlet.session.cookie.secure", Boolean.class, false), "Cookie Secure obrigatório");
        require(env.getProperty("server.servlet.session.cookie.http-only", Boolean.class, false), "Cookie HttpOnly obrigatório");
        require("lax".equalsIgnoreCase(env.getProperty("server.servlet.session.cookie.same-site")), "SameSite Lax obrigatório");
        require(!env.getProperty("app.bootstrap-admin.enabled", Boolean.class, false), "Bootstrap deve estar desabilitado em produção");
        require(env.getProperty("app.abuse-protection.enabled", Boolean.class, false), "Proteção contra abuso obrigatória");
        require("validate".equals(env.getProperty("spring.jpa.hibernate.ddl-auto")), "Hibernate deve somente validar o schema");
        require(!env.getProperty("spring.datasource.password", "").isBlank(), "Credencial de banco ausente");
        require(!env.getProperty("spring.datasource.username", "root").equalsIgnoreCase("root"), "Banco exige usuário restrito");
        require(httpsOrigin(env.getProperty("app.frontend-base-url", "")), "Origem HTTPS obrigatória");
        String origins = env.getProperty("app.cors.allowed-origins", "");
        require(!origins.isBlank(), "CORS exige origens explícitas");
        for (String origin : origins.split(",")) require(httpsOrigin(origin.trim()), "Origem CORS deve ser HTTPS exata");
        String upload = env.getProperty("app.upload.dir", "");
        require(!upload.isBlank() && Path.of(upload).isAbsolute(), "Upload exige diretório absoluto persistente");
        if (env.getProperty("app.mail.enabled", Boolean.class, false)) {
            require(!env.getProperty("spring.mail.host", "").isBlank() && !env.getProperty("app.mail.from", "").isBlank(), "SMTP exige host e remetente");
            require(env.getProperty("spring.mail.properties.mail.smtp.starttls.required", Boolean.class, false)
                || env.getProperty("spring.mail.properties.mail.smtp.ssl.enable", Boolean.class, false), "SMTP exige TLS");
        }
        require(!env.getProperty("spring.mail.properties.mail.debug", Boolean.class, false), "Debug SMTP proibido");
    }
    private static boolean httpsOrigin(String value) {
        try {
            URI uri = URI.create(value);
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getUserInfo() == null
                && uri.getQuery() == null && uri.getFragment() == null && uri.getPath().isEmpty();
        } catch (IllegalArgumentException e) { return false; }
    }
    private static void require(boolean valid, String message) {
        if (!valid) throw new IllegalStateException(message);
    }
}
