package com.touhouqing.grabteacherbackend.listener;

import com.touhouqing.grabteacherbackend.event.ProgramChangedEvent;
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
public class AbroadProgramsCacheInvalidationListener {

    private final CacheWarmupService cacheWarmupService;

    @Async
    @EventListener
    public void onProgramChanged(ProgramChangedEvent event) {
        try {
            // 精准删除：携带 countryId/stageId 时按参数化 key 清理；否则清理常用热点键
            cacheWarmupService.invalidatePublicAbroadProgramsJson(20, event.getCountryId(), event.getStageId());
            log.info("ProgramChangedEvent handled: {} -> invalidated abroad programs keys, countryId={}, stageId={}",
                    event.getChangeType(), event.getCountryId(), event.getStageId());
        } catch (Exception e) {
            log.warn("Failed to invalidate abroad programs json cache", e);
        }
    }
}

