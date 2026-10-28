package com.bemfeito.api.security.bootstrap;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.bootstrap-admin.enabled",havingValue="true",matchIfMissing=false)
public class AdminBootstrapRunner implements ApplicationRunner {
    private final Environment environment;
    private final AdminBootstrapService bootstrap;
    public AdminBootstrapRunner(Environment environment,AdminBootstrapService bootstrap) {
        this.environment=environment;this.bootstrap=bootstrap;
    }
    @Override public void run(ApplicationArguments args) {
        bootstrap.initialize(environment.getProperty("APP_BOOTSTRAP_ADMIN_NAME"),
            environment.getProperty("APP_BOOTSTRAP_ADMIN_EMAIL"),environment.getProperty("APP_BOOTSTRAP_ADMIN_PASSWORD"));
    }
}
