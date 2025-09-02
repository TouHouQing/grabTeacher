package com.touhouqing.grabteacherbackend.job;

import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdjustmentTimesResetJob {

    private final UserMapper userMapper;

    // 每月1号00:00执行
    @Scheduled(cron = "0 0 0 1 * ?")
    @Transactional
    public void resetMonthlyAdjustmentTimes() {
        log.info("开始执行定时任务：重置用户本月调课次数为3");
        int affected = userMapper.resetAllAdjustmentTimes(3);
        log.info("重置完成，共影响 {} 个用户", affected);
    }
}


