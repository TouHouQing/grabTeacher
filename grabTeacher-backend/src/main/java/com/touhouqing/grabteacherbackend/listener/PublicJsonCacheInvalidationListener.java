package com.touhouqing.grabteacherbackend.listener;

import com.touhouqing.grabteacherbackend.event.SubjectChangedEvent;
import com.touhouqing.grabteacherbackend.service.CacheWarmupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicJsonCacheInvalidationListener {

    private final CacheWarmupService cacheWarmupService;

    @Async
    @EventListener
    public void onSubjectChanged(SubjectChangedEvent event) {
        try {
            cacheWarmupService.invalidatePublicActiveSubjectsJson();
            log.info("SubjectChangedEvent handled: {}", event.getChangeType());
        } catch (Exception e) {
            log.warn("Failed to invalidate active subjects json cache", e);
        }
    }
}

