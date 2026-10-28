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
        System.setProperty("app.bootstrap-admin.enabled", "true");
        System.setProperty("app.cors.allowed-origins", "http://127.0.0.1:5501");
        System.setProperty("app.frontend-base-url", "http://127.0.0.1:5501");
        System.setProperty("app.upload.dir", "target/browser-test-uploads");
        System.setProperty("app.mail.enabled", "false");
        SpringApplication.run(BemFeitoApiApplication.class, args);
        System.out.println("BEMFEITO_BROWSER_TEST_READY");
    }
}
