package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.RescheduleRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 调课申请Mapper接口
 */
@Mapper
public interface RescheduleRequestMapper extends BaseMapper<RescheduleRequest> {

    /**
     * 根据学生ID获取调课申请列表
     */
    @Select("SELECT * FROM reschedule_requests WHERE applicant_id = #{studentId} AND applicant_type = 'student' AND is_deleted = 0 ORDER BY created_at DESC")
    List<RescheduleRequest> findByStudentId(@Param("studentId") Long studentId);

    /**
     * 根据教师ID获取需要审批的调课申请列表
     */
    @Select("SELECT rr.* FROM reschedule_requests rr " +
            "INNER JOIN schedules s ON rr.schedule_id = s.id " +
            "WHERE s.teacher_id = #{teacherId} AND rr.is_deleted = 0 " +
            "ORDER BY rr.created_at DESC")
    List<RescheduleRequest> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 根据教师ID和状态获取调课申请列表（分页）
     */
    Page<RescheduleRequest> findByTeacherIdWithPage(Page<RescheduleRequest> page,
                                                   @Param("teacherId") Long teacherId,
                                                   @Param("status") String status);

    /**
     * 根据课程安排ID获取调课申请列表
     */
    @Select("SELECT * FROM reschedule_requests WHERE schedule_id = #{scheduleId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<RescheduleRequest> findByScheduleId(@Param("scheduleId") Long scheduleId);

    /**
     * 根据申请人ID和类型获取调课申请列表（分页）
     */
    Page<RescheduleRequest> findByApplicantWithPage(Page<RescheduleRequest> page,
                                                   @Param("applicantId") Long applicantId,
                                                   @Param("applicantType") String applicantType,
                                                   @Param("status") String status);

    /**
     * 统计教师待处理的调课申请数量
     */
    @Select("SELECT COUNT(*) FROM reschedule_requests rr " +
            "INNER JOIN schedules s ON rr.schedule_id = s.id " +
            "WHERE s.teacher_id = #{teacherId} AND rr.status = 'pending' AND rr.is_deleted = 0")
    int countPendingByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 统计学生的调课申请数量
     */
    @Select("SELECT COUNT(*) FROM reschedule_requests " +
            "WHERE applicant_id = #{studentId} AND applicant_type = 'student' " +
            "AND status = #{status} AND is_deleted = 0")
    int countByStudentIdAndStatus(@Param("studentId") Long studentId, @Param("status") String status);
}
