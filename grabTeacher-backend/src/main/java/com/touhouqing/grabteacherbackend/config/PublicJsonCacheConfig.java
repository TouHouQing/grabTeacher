package com.touhouqing.grabteacherbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 公开端 JSON 文本缓存 TTL 配置（集中管理 & 可配置）
 */
@Component
public class PublicJsonCacheConfig {

    @Value("${public.cache.ttl.grade-names:PT30M}")
    private Duration gradeNamesTtl;

    @Value("${public.cache.ttl.active-subjects:PT30M}")
    private Duration activeSubjectsTtl;

    @Value("${public.cache.ttl.programs-active:PT10M}")
    private Duration programsActiveTtl;

    @Value("${public.cache.ttl.featured-teachers:PT20M}")
    private Duration featuredTeachersTtl;

    public Duration getGradeNamesTtl() { return gradeNamesTtl; }
    public Duration getActiveSubjectsTtl() { return activeSubjectsTtl; }
    public Duration getProgramsActiveTtl() { return programsActiveTtl; }
    public Duration getFeaturedTeachersTtl() { return featuredTeachersTtl; }
}

