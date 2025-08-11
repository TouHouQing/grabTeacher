package com.touhouqing.grabteacherbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAspectJAutoProxy
@EnableAsync
@EnableScheduling
public class GrabTeacherBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(GrabTeacherBackendApplication.class, args);
    }

}