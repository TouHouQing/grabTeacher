package com.touhouqing.grabteacherbackend.service;

public interface ScheduleCleanupService {

    /**
     * 手动触发清理任务（用于测试或手动执行）
     * @return 更新的课程数量
     */
    int manualCleanupExpiredSchedules();

}
