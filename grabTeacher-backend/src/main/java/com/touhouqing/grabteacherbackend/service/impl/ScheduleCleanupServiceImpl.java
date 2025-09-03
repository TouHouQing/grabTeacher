package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.service.ScheduleCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ScheduleCleanupServiceImpl implements ScheduleCleanupService {
    private final CourseScheduleMapper scheduleMapper;

    /**
     * 手动触发清理任务（用于测试或手动执行）
     * @return 更新的课程数量
     */
    public int manualCleanupExpiredSchedules() {
        log.info("手动触发课程状态清理任务");

        try {
            // 获取当前日期和时间
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();

            // 使用优化的查询方法，直接获取过期的进行中课程
            List<CourseSchedule> expiredSchedules = scheduleMapper.findExpiredScheduled(today, now);

            if (expiredSchedules.isEmpty()) {
                log.info("没有找到过期的进行中课程安排");
                return 0;
            }

            log.info("找到{}个过期的进行中课程安排，准备批量更新状态", expiredSchedules.size());

            // 批量更新课程状态为已完成
            int updatedCount = 0;
            for (CourseSchedule schedule : expiredSchedules) {
                com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<CourseSchedule> updateWrapper = new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<>();
                updateWrapper.eq("id", schedule.getId())
                        .set("schedule_status", "completed");

                int result = scheduleMapper.update(null, updateWrapper);
                if (result > 0) {
                    updatedCount++;
                    log.debug("课程安排ID: {} 已更新为已完成状态，原定时间: {} {}-{}",
                            schedule.getId(),
                            schedule.getScheduledDate(),
                            schedule.getStartTime(),
                            schedule.getEndTime());
                }
            }

            log.info("手动清理任务执行完成：成功更新{}个过期课程状态为已完成", updatedCount);
            return updatedCount;

        } catch (Exception e) {
            log.error("手动执行清理任务失败", e);
            throw new RuntimeException("清理任务执行失败", e);
        }
    }
}
