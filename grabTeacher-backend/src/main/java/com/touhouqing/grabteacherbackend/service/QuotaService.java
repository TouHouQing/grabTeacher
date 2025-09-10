package com.touhouqing.grabteacherbackend.service;

public interface QuotaService {

    /**
     * 提交申请时按（actor,enrollment,month）消耗一次并返回是否超额（用于前端提示/后续审批扣费）。
     * 返回true表示超出本月该课程配额，需要按规则扣费/补偿（在审批通过时执行）。
     */
    boolean consumeOnApply(String actorType, Long actorId, Long enrollmentId);

    /**
     * 审批被驳回时回滚一次消耗。
     */
    void rollbackOnReject(String actorType, Long actorId, Long enrollmentId);

    /**
     * 读取当前（月-课程-角色）配额与已用，判断本次是否超额（不改变计数），供审批阶段复核。
     */
    boolean isOverQuota(String actorType, Long actorId, Long enrollmentId);
}

