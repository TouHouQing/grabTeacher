package com.touhouqing.grabteacherbackend.job;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.mapper.HourDetailMapper;
import com.touhouqing.grabteacherbackend.model.entity.HourDetail;
import com.touhouqing.grabteacherbackend.model.entity.Schedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 课程安排清理服务
 * 负责定时清理过期的课程状态
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleCleanupJob {

    private final ScheduleMapper scheduleMapper;
    private final TeacherMapper teacherMapper;
    private final HourDetailMapper hourDetailMapper;

    /**
     * 每天午夜00:00执行，将所有过期的进行中课程状态更新为已完成
     * cron表达式: 秒 分 时 日 月 周
     */
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void completeExpiredProgessSchedules() {
        log.info("开始执行定时任务：更新过期的进行中课程状态为已完成");

        try {
            // 获取当前日期和时间
            LocalDate today = LocalDate.now();
            LocalTime now = LocalTime.now();
            
            // 使用优化的查询方法，直接获取过期的进行中课程
            List<Schedule> expiredSchedules = scheduleMapper.findExpiredProgressingSchedules(today, now);
            
            if (expiredSchedules.isEmpty()) {
                log.info("没有找到过期的进行中课程安排");
                return;
            }

            log.info("找到{}个过期的进行中课程安排，准备批量更新状态", expiredSchedules.size());
            
            // 批量更新课程状态为已完成，并累加教师本月课时
            int updatedCount = 0;
            for (Schedule schedule : expiredSchedules) {
                LambdaUpdateWrapper<Schedule> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(Schedule::getId, schedule.getId())
                           .set(Schedule::getStatus, "completed");
                
                int result = scheduleMapper.update(null, updateWrapper);
                if (result > 0) {
                    updatedCount++;
                    // 计算课时：基于开始和结束时间差，支持 1.0、1.5、2.0 小时等
                    java.time.Duration duration = java.time.Duration.between(schedule.getStartTime(), schedule.getEndTime());
                    java.math.BigDecimal hours = new java.math.BigDecimal(duration.toMinutes())
                            .divide(new java.math.BigDecimal(60), 2, java.math.RoundingMode.HALF_UP);
                    if (hours.compareTo(java.math.BigDecimal.ZERO) > 0) {
                        // 查询教师以获取用户ID与姓名
                        com.touhouqing.grabteacherbackend.model.entity.Teacher teacher = teacherMapper.selectById(schedule.getTeacherId());
                        Long teacherUserId = teacher != null ? teacher.getUserId() : null;
                        String teacherName = teacher != null ? teacher.getRealName() : null;
                        // 记录教师课时明细
                        HourDetail detail = HourDetail.builder()
                                .userId(teacherUserId)
                                .name(teacherName)
                                .hours(hours)
                                .hoursBefore(null)
                                .hoursAfter(null)
                                .transactionType(1)
                                .reason("课程完成自动结算")
                                .bookingId(schedule.getId())
                                .operatorId(null)
                                .createdAt(java.time.LocalDateTime.now())
                                .build();
                        hourDetailMapper.insert(detail);
                        // 累加教师本月课时
                        teacherMapper.incrementCurrentHours(schedule.getTeacherId(), hours);
                    }
                    log.debug("课程安排ID: {} 已更新为已完成状态，原定时间: {} {}-{}", 
                            schedule.getId(), 
                            schedule.getScheduledDate(),
                            schedule.getStartTime(),
                            schedule.getEndTime());
                }
            }
            
            log.info("定时任务执行完成：成功更新{}个过期课程状态为已完成", updatedCount);
                    
        } catch (Exception e) {
            log.error("执行定时任务时发生异常：更新过期课程状态失败", e);
            throw e; // 重新抛出异常，确保事务回滚
        }
    }

    /**
     * 每月1号 00:00 执行，current_hours -> last_hours，并清空 current_hours
     */
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void resetTeacherMonthlyHours() {
        log.info("开始执行定时任务：月初重置教师课时（current->last，并清空current）");
        try {
            int affected = teacherMapper.resetMonthlyHours();
            log.info("月初重置完成，共影响教师记录数：{}", affected);
        } catch (Exception e) {
            log.error("月初重置教师课时失败", e);
            throw e;
        }
    }
}
