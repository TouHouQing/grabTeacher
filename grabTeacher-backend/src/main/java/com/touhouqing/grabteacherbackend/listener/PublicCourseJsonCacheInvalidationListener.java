package com.touhouqing.grabteacherbackend.listener;

import com.touhouqing.grabteacherbackend.event.CourseChangedEvent;
import com.touhouqing.grabteacherbackend.service.CacheWarmupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PublicCourseJsonCacheInvalidationListener {

    private final CacheWarmupService cacheWarmupService;

    @Async
    @EventListener
    public void onCourseChanged(CourseChangedEvent event) {
        try {
            cacheWarmupService.invalidatePublicCourseCachesLightweight();
            log.info("CourseChangedEvent handled: {} -> invalidated public course JSON caches", event.getChangeType());
        } catch (Exception e) {
            log.warn("Failed to invalidate public course JSON caches", e);
        }
    }
}

