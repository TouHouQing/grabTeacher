package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
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

    @Select("SELECT cs.*, ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.teacher_id = #{teacherId} AND cs.scheduled_date BETWEEN #{startDate} AND #{endDate} \n" +
            "AND ce.is_deleted = 0 AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    List<CourseSchedule> findByTeacherIdAndDateRange(@Param("teacherId") Long teacherId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);

    @Select("SELECT cs.*, ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.student_id = #{studentId} AND cs.scheduled_date BETWEEN #{startDate} AND #{endDate} \n" +
            "AND ce.is_deleted = 0 AND cs.is_deleted = 0 ORDER BY cs.scheduled_date ASC, cs.start_time ASC")
    List<CourseSchedule> findByStudentIdAndDateRange(@Param("studentId") Long studentId,
                                                     @Param("startDate") LocalDate startDate,
                                                     @Param("endDate") LocalDate endDate);
    
    @Select("SELECT cs.*, ce.teacher_id AS teacherId, ce.student_id AS studentId, ce.course_id AS courseId, ce.booking_request_id AS bookingRequestId, cs.enrollment_id AS enrollmentId FROM course_schedules cs \n" +
            "JOIN course_enrollments ce ON cs.enrollment_id = ce.id \n" +
            "WHERE ce.student_id = #{studentId} \n" +
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
}


