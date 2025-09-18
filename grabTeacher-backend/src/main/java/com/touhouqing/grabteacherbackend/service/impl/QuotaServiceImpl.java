package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.mapper.CourseEnrollmentMapper;
import com.touhouqing.grabteacherbackend.mapper.MonthlyAdjustmentCounterMapper;
import com.touhouqing.grabteacherbackend.model.entity.CourseEnrollment;
import com.touhouqing.grabteacherbackend.model.entity.MonthlyAdjustmentCounter;
import com.touhouqing.grabteacherbackend.service.QuotaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuotaServiceImpl implements QuotaService {

    private final MonthlyAdjustmentCounterMapper counterMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;

    private static final ZoneId BJ = ZoneId.of("Asia/Shanghai");

    private String monthKeyNow() {
        ZonedDateTime now = ZonedDateTime.now(BJ);
        int y = now.getYear();
        int m = now.getMonthValue();
        return String.format("%04d-%02d", y, m);
    }

    /**
     * 计算当月允许次数：min(3, max(1, floor(total_sessions * 0.2)))
     * 说明：
     * - 以报名(enrollment)的总课次数为基数；
     * - 最小1次；
     * - 取3次与20%中更小者；
     * - 为避免误差，采用向下取整。
     */
    private int calcAllowedTimes(String actorType, Long enrollmentId) {
        try {
            CourseEnrollment ce = courseEnrollmentMapper.selectById(enrollmentId);
            Integer total = (ce != null) ? ce.getTotalSessions() : null;
            int byRatio = (total == null || total <= 0) ? 1 : (int) Math.floor(total * 0.2);
            if (byRatio < 1) byRatio = 1;
            int allowed = Math.min(3, byRatio);
            return allowed;
        } catch (Exception e) {
            // 降级：若读取失败，回退为3次，保证主流程不被阻断
            log.warn("calcAllowedTimes degraded, fallback to 3. enrollmentId={}", enrollmentId, e);
            return 3;
        }
    }

    private MonthlyAdjustmentCounter getOrCreate(String actorType, Long actorId, Long enrollmentId, String monthKey) {
        MonthlyAdjustmentCounter one = counterMapper.findOne(actorType, actorId, enrollmentId, monthKey);
        if (one != null) return one;
        MonthlyAdjustmentCounter fresh = MonthlyAdjustmentCounter.builder()
                .actorType(actorType)
                .actorId(actorId)
                .enrollmentId(enrollmentId)
                .monthKey(monthKey)
                .allowedTimes(calcAllowedTimes(actorType, enrollmentId))
                .usedTimes(0)
                .build();
        counterMapper.insert(fresh);
        return fresh;
    }

    @Override
    public boolean consumeOnApply(String actorType, Long actorId, Long enrollmentId) {
        try {
            String monthKey = monthKeyNow();
            MonthlyAdjustmentCounter c = getOrCreate(actorType, actorId, enrollmentId, monthKey);
            counterMapper.incrementUsed(c.getId());
            // 重新查询以准确判断是否超额
            MonthlyAdjustmentCounter after = counterMapper.findOne(actorType, actorId, enrollmentId, monthKey);
            boolean over = after.getUsedTimes() != null && after.getAllowedTimes() != null && after.getUsedTimes() > after.getAllowedTimes();
            log.info("Quota consume: actorType={}, actorId={}, enrollmentId={}, month={}, used={}/{}, over={}",
                    actorType, actorId, enrollmentId, monthKey, after.getUsedTimes(), after.getAllowedTimes(), over);
            return over;
        } catch (Exception e) {
            // 表不存在或数据库异常时，降级为“不过额且不阻断流程”
            log.warn("Quota consume degraded: monthly_adjustment_counters unavailable, bypassing quota check. actorType={}, actorId={}, enrollmentId={}",
                    actorType, actorId, enrollmentId, e);
            return false;
        }
    }

    @Override
    public void rollbackOnReject(String actorType, Long actorId, Long enrollmentId) {
        try {
            String monthKey = monthKeyNow();
            MonthlyAdjustmentCounter c = counterMapper.findOne(actorType, actorId, enrollmentId, monthKey);
            if (c != null) {
                counterMapper.decrementUsed(c.getId());
                log.info("Quota rollback: actorType={}, actorId={}, enrollmentId={}, month={}", actorType, actorId, enrollmentId, monthKey);
            }
        } catch (Exception e) {
            // 降级：仅记录日志，不影响主流程
            log.warn("Quota rollback degraded: monthly_adjustment_counters unavailable. actorType={}, actorId={}, enrollmentId={}",
                    actorType, actorId, enrollmentId, e);
        }
    }

    @Override
    public boolean isOverQuota(String actorType, Long actorId, Long enrollmentId) {
        try {
            String monthKey = monthKeyNow();
            MonthlyAdjustmentCounter c = getOrCreate(actorType, actorId, enrollmentId, monthKey);
            return c.getUsedTimes() != null && c.getAllowedTimes() != null && c.getUsedTimes() > c.getAllowedTimes();
        } catch (Exception e) {
            // 降级：无法读取配额时，默认不过额
            log.warn("Quota read degraded: monthly_adjustment_counters unavailable. actorType={}, actorId={}, enrollmentId={}",
                    actorType, actorId, enrollmentId, e);
            return false;
        }
    }
}

