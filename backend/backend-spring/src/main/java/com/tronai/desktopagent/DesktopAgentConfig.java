package com.tronai.desktopagent;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableScheduling
@EnableConfigurationProperties(DesktopAgentProperties.class)
public class DesktopAgentConfig {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService desktopAgentExecutor() {
        return Executors.newFixedThreadPool(4, runnable -> {
            Thread thread = new Thread(runnable, "desktop-agent-orchestrator");
            thread.setDaemon(true);
            return thread;
        });
    }
}
