package com.bemfeito.api.security.recovery;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
@Configuration @EnableAsync
public class RecoveryConfiguration {
    @Bean(name="passwordRecoveryExecutor") public ThreadPoolTaskExecutor executor() {
        var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(1);executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);executor.setThreadNamePrefix("password-recovery-");
        executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(10);return executor;
    }
}
