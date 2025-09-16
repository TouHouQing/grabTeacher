package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.model.vo.ClassRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Mapper
public interface CourseScheduleMapper extends BaseMapper<CourseSchedule> {

    @Select("SELECT COUNT(*) FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date = #{date} AND ce.is_deleted = 0 AND cs.is_deleted = 0 AND \n" +
            "((cs.start_time <= #{startTime} AND cs.end_time > #{startTime}) OR \n" +
            " (cs.start_time < #{endTime} AND cs.end_time >= #{endTime}) OR \n" +
            " (cs.start_time >= #{startTime} AND cs.end_time <= #{endTime})) AND cs.schedule_status != 'cancelled'")
    int countTeacherConflicts(@Param("teacherId") Long teacherId,
                              @Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    @Select("SELECT COUNT(*) FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.student_id = #{studentId} AND cs.scheduled_date = #{date} AND ce.is_deleted = 0 AND cs.is_deleted = 0 AND \n" +
            "((cs.start_time <= #{startTime} AND cs.end_time > #{startTime}) OR \n" +
            " (cs.start_time < #{endTime} AND cs.end_time >= #{endTime}) OR \n" +
            " (cs.start_time >= #{startTime} AND cs.end_time <= #{endTime})) AND cs.schedule_status != 'cancelled'")
    int countStudentConflicts(@Param("studentId") Long studentId,
                              @Param("date") LocalDate date,
                              @Param("startTime") LocalTime startTime,
                              @Param("endTime") LocalTime endTime);

    @Select("SELECT cs.*, " +
            "ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId, " +
            "ce.duration_minutes AS durationMinutes, ce.is_trial AS trial, ce.enrollment_type AS courseType, ce.grade AS grade, " +
            "s.real_name AS studentName, t.real_name AS teacherName, " +
            "c.title AS courseTitle, sub.name AS subjectName " +
            "FROM course_schedules cs " +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id " +
            "JOIN students s ON ce.student_id = s.id " +
            "JOIN teachers t ON ce.teacher_id = t.id " +
            "LEFT JOIN courses c ON ce.course_id = c.id " +
            "LEFT JOIN subjects sub ON c.subject_id = sub.id " +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date BETWEEN #{startDate} AND #{endDate} " +
            "AND ce.is_deleted = 0 AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    List<CourseSchedule> findByTeacherIdAndDateRange(@Param("teacherId") Long teacherId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Select("SELECT cs.*, " +
            "ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId, " +
            "ce.duration_minutes AS durationMinutes, ce.is_trial AS trial, ce.enrollment_type AS courseType, ce.grade AS grade, " +
            "s.real_name AS studentName, t.real_name AS teacherName, " +
            "c.title AS courseTitle, sub.name AS subjectName " +
            "FROM course_schedules cs " +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id " +
            "JOIN students s ON ce.student_id = s.id " +
            "JOIN teachers t ON ce.teacher_id = t.id " +
            "LEFT JOIN courses c ON ce.course_id = c.id " +
            "LEFT JOIN subjects sub ON c.subject_id = sub.id " +
            "WHERE ce.student_id = #{studentId} AND cs.scheduled_date BETWEEN #{startDate} AND #{endDate} " +
            "AND ce.is_deleted = 0 AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    List<CourseSchedule> findByStudentIdAndDateRange(@Param("studentId") Long studentId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
    
    @Select("SELECT cs.*, " +
            "ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId, " +
            "ce.duration_minutes AS durationMinutes, ce.is_trial AS trial, ce.enrollment_type AS courseType, ce.grade AS grade, " +
            "s.real_name AS studentName, t.real_name AS teacherName, " +
            "c.title AS courseTitle, sub.name AS subjectName " +
            "FROM course_schedules cs " +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id " +
            "JOIN students s ON ce.student_id = s.id " +
            "JOIN teachers t ON ce.teacher_id = t.id " +
            "LEFT JOIN courses c ON ce.course_id = c.id " +
            "LEFT JOIN subjects sub ON c.subject_id = sub.id " +
            "WHERE ce.student_id = #{studentId} " +
            "AND ce.is_deleted = 0 AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    List<CourseSchedule> findByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT cs.* FROM course_schedules cs \n" +
            "WHERE cs.schedule_status = 'scheduled' AND cs.is_deleted = 0 AND \n" +
            "(cs.scheduled_date < #{currentDate} OR (cs.scheduled_date = #{currentDate} AND cs.end_time < #{currentTime}))")
    List<CourseSchedule> findExpiredScheduled(@Param("currentDate") LocalDate currentDate,
                                              @Param("currentTime") LocalTime currentTime);

    @Select("SELECT cs.*, ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId FROM course_schedules cs JOIN course_enrollments ce ON cs.enrollment_id = ce.id WHERE cs.id = #{id} AND cs.is_deleted = 0")
    CourseSchedule findById(@Param("id") Long id);

    @Select("SELECT COUNT(*) FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date >= #{date} AND cs.schedule_status = 'scheduled' \n" +
            "AND cs.is_deleted = 0 AND ce.is_deleted = 0")
    int countUpcomingByTeacherId(@Param("teacherId") Long teacherId, @Param("date") LocalDate date);

    @Select("SELECT cs.*, ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId FROM course_schedules cs JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.booking_request_id = #{bookingRequestId} AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    java.util.List<CourseSchedule> findByBookingRequestId(@Param("bookingRequestId") Long bookingRequestId);

    /**
     * 检查试听课预约是否会影响基础2小时区间的可用性
     * 如果某个基础2小时区间内有试听课预约，则该区间不能用于正式课预约
     */
    @Select("SELECT COUNT(*) FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "JOIN booking_requests br ON ce.booking_request_id = br.id \n" +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date = #{date} AND ce.is_deleted = 0 AND cs.is_deleted = 0 AND br.is_deleted = 0 \n" +
            "AND br.is_trial = 1 AND cs.schedule_status != 'cancelled' \n" +
            "AND ((cs.start_time >= #{baseStartTime} AND cs.start_time < #{baseEndTime}) OR \n" +
            "     (cs.end_time > #{baseStartTime} AND cs.end_time <= #{baseEndTime}) OR \n" +
            "     (cs.start_time <= #{baseStartTime} AND cs.end_time >= #{baseEndTime}))")
    int countTrialConflictsInBaseSlot(@Param("teacherId") Long teacherId,
                                      @Param("date") LocalDate date,
                                      @Param("baseStartTime") LocalTime baseStartTime,
                                      @Param("baseEndTime") LocalTime baseEndTime);

    /**
     * 检查试听课时间段是否可用（不检查试听课之间的冲突）
     * 试听课之间可以共存，但试听课会影响正式课的预约
     */
    @Select("SELECT COUNT(*) FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date = #{date} AND ce.is_deleted = 0 AND cs.is_deleted = 0 AND \n" +
            "((cs.start_time <= #{startTime} AND cs.end_time > #{startTime}) OR \n" +
            " (cs.start_time < #{endTime} AND cs.end_time >= #{endTime}) OR \n" +
            " (cs.start_time >= #{startTime} AND cs.end_time <= #{endTime})) AND cs.schedule_status != 'cancelled'")
    int countTrialTimeConflicts(@Param("teacherId") Long teacherId,
                                @Param("date") LocalDate date,
                                @Param("startTime") LocalTime startTime,
                                @Param("endTime") LocalTime endTime);

    /**
     * 查询教师上课记录
     */
    @Select("SELECT " +
            "cs.id as scheduleId, " +
            "cs.scheduled_date as scheduledDate, " +
            "cs.start_time as startTime, " +
            "cs.end_time as endTime, " +
            "cs.session_number as sessionNumber, " +
            "cs.teacher_notes as teacherNotes, " +
            "cs.student_feedback as studentFeedback, " +
            "ce.duration_minutes as durationMinutes, " +
            "ce.is_trial as isTrial, " +
            "ce.enrollment_type as courseType, " +
            "s.id as studentId, " +
            "s.real_name as studentName, " +
            "t.real_name as teacherName, " +
            "COALESCE(c.title, '一对一课程') as courseName, " +
            "CASE WHEN ce.is_trial = 1 OR ce.enrollment_type <> 'one_on_one' THEN NULL ELSE c.teacher_hourly_rate END as teacherHourlyRate " +
            "FROM course_schedules cs " +
            "INNER JOIN course_enrollments ce ON cs.enrollment_id = ce.id " +
            "INNER JOIN students s ON ce.student_id = s.id " +
            "INNER JOIN teachers t ON ce.teacher_id = t.id " +
            "LEFT JOIN courses c ON ce.course_id = c.id " +
            "WHERE cs.schedule_status = 'completed' " +
            "AND ce.teacher_id = #{teacherId} " +
            "AND cs.is_deleted = 0 " +
            "AND ce.is_deleted = 0 " +
            "AND (#{year} IS NULL OR YEAR(cs.scheduled_date) = #{year}) " +
            "AND (#{month} IS NULL OR MONTH(cs.scheduled_date) = #{month}) " +
            "AND (#{studentName} IS NULL OR #{studentName} = '' OR s.real_name LIKE CONCAT('%', #{studentName}, '%')) " +
            "AND (#{courseName} IS NULL OR #{courseName} = '' OR c.title LIKE CONCAT('%', #{courseName}, '%') OR '一对一课程' LIKE CONCAT('%', #{courseName}, '%')) " +
            "ORDER BY cs.scheduled_date DESC, cs.start_time DESC")
    List<ClassRecordVO> selectClassRecords(@Param("teacherId") Long teacherId,
                                          @Param("year") Integer year,
                                          @Param("month") Integer month,
                                          @Param("studentName") String studentName,
                                          @Param("courseName") String courseName);
}


