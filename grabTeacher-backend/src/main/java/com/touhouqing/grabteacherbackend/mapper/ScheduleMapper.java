package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.Schedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface ScheduleMapper extends BaseMapper<Schedule> {
    
    /**
     * 根据教师ID查询课程安排列表
     */
    @Select("SELECT * FROM schedules WHERE teacher_id = #{teacherId} AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据学生ID查询课程安排列表
     */
    @Select("SELECT * FROM schedules WHERE student_id = #{studentId} AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据课程ID查询课程安排列表
     */
    @Select("SELECT * FROM schedules WHERE course_id = #{courseId} AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findByCourseId(@Param("courseId") Long courseId);
    
    /**
     * 根据预约申请ID查询课程安排列表
     */
    @Select("SELECT * FROM schedules WHERE booking_request_id = #{bookingRequestId} AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findByBookingRequestId(@Param("bookingRequestId") Long bookingRequestId);
    
    /**
     * 查询指定教师在指定日期的课程安排
     */
    @Select("SELECT * FROM schedules WHERE teacher_id = #{teacherId} AND scheduled_date = #{date} AND is_deleted = 0 ORDER BY start_time ASC")
    List<Schedule> findByTeacherIdAndDate(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);
    
    /**
     * 查询指定学生在指定日期的课程安排
     */
    @Select("SELECT * FROM schedules WHERE student_id = #{studentId} AND scheduled_date = #{date} AND is_deleted = 0 ORDER BY start_time ASC")
    List<Schedule> findByStudentIdAndDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);
    
    /**
     * 查询指定教师在指定时间范围内的课程安排
     */
    @Select("SELECT * FROM schedules WHERE teacher_id = #{teacherId} AND scheduled_date BETWEEN #{startDate} AND #{endDate} AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findByTeacherIdAndDateRange(@Param("teacherId") Long teacherId, 
                                              @Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    /**
     * 查询指定学生在指定时间范围内的课程安排
     */
    @Select("SELECT * FROM schedules WHERE student_id = #{studentId} AND scheduled_date BETWEEN #{startDate} AND #{endDate} AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findByStudentIdAndDateRange(@Param("studentId") Long studentId, 
                                              @Param("startDate") LocalDate startDate, 
                                              @Param("endDate") LocalDate endDate);
    
    /**
     * 检查教师在指定时间是否有冲突的课程安排
     */
    @Select("SELECT COUNT(*) FROM schedules WHERE teacher_id = #{teacherId} AND scheduled_date = #{date} AND " +
            "((start_time <= #{startTime} AND end_time > #{startTime}) OR " +
            "(start_time < #{endTime} AND end_time >= #{endTime}) OR " +
            "(start_time >= #{startTime} AND end_time <= #{endTime})) AND " +
            "status != 'cancelled' AND is_deleted = 0")
    int countConflictingSchedules(@Param("teacherId") Long teacherId, 
                                 @Param("date") LocalDate date, 
                                 @Param("startTime") LocalTime startTime, 
                                 @Param("endTime") LocalTime endTime);
    
    /**
     * 检查学生在指定时间是否有冲突的课程安排
     */
    @Select("SELECT COUNT(*) FROM schedules WHERE student_id = #{studentId} AND scheduled_date = #{date} AND " +
            "((start_time <= #{startTime} AND end_time > #{startTime}) OR " +
            "(start_time < #{endTime} AND end_time >= #{endTime}) OR " +
            "(start_time >= #{startTime} AND end_time <= #{endTime})) AND " +
            "status != 'cancelled' AND is_deleted = 0")
    int countStudentConflictingSchedules(@Param("studentId") Long studentId, 
                                        @Param("date") LocalDate date, 
                                        @Param("startTime") LocalTime startTime, 
                                        @Param("endTime") LocalTime endTime);
    
    /**
     * 查询指定状态的课程安排
     */
    @Select("SELECT * FROM schedules WHERE status = #{status} AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findByStatus(@Param("status") String status);
    
    /**
     * 查询即将到来的课程安排（今天及以后）
     */
    @Select("SELECT * FROM schedules WHERE scheduled_date >= #{date} AND status = 'progressing' AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findUpcomingSchedules(@Param("date") LocalDate date);
    
    /**
     * 查询指定教师即将到来的课程安排
     */
    @Select("SELECT * FROM schedules WHERE teacher_id = #{teacherId} AND scheduled_date >= #{date} AND status = 'progressing' AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findUpcomingSchedulesByTeacherId(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);
    
    /**
     * 查询指定学生即将到来的课程安排
     */
    @Select("SELECT * FROM schedules WHERE student_id = #{studentId} AND scheduled_date >= #{date} AND status = 'progressing' AND is_deleted = 0 ORDER BY scheduled_date ASC, start_time ASC")
    List<Schedule> findUpcomingSchedulesByStudentId(@Param("studentId") Long studentId, @Param("date") LocalDate date);
    
    /**
     * 查询试听课安排
     */
    @Select("SELECT * FROM schedules WHERE is_trial = 1 AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findTrialSchedules();
    
    /**
     * 查询指定学生的试听课安排
     */
    @Select("SELECT * FROM schedules WHERE student_id = #{studentId} AND is_trial = 1 AND is_deleted = 0 ORDER BY scheduled_date DESC, start_time DESC")
    List<Schedule> findTrialSchedulesByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 统计指定教师的课程安排数量
     */
    @Select("SELECT COUNT(*) FROM schedules WHERE teacher_id = #{teacherId} AND is_deleted = 0")
    int countByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 统计指定学生的课程安排数量
     */
    @Select("SELECT COUNT(*) FROM schedules WHERE student_id = #{studentId} AND is_deleted = 0")
    int countByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 查询最近的课程安排
     */
    @Select("SELECT * FROM schedules WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<Schedule> findRecentSchedules(@Param("limit") int limit);
}
