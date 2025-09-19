package com.example.treeapi.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class AppConfig {

    /**
     * WebSocket 업데이트를 위한 전역 스케줄러를 Bean으로 등록합니다.
     * Spring이 애플리케이션 생명주기에 맞춰 스케줄러를 관리합니다. (애플리케이션 종료 시 안전하게 종료)
     * @return ScheduledExecutorService 인스턴스
     */
    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService webSocketScheduler() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}

