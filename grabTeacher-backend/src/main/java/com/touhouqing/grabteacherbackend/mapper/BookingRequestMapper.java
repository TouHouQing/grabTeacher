package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BookingRequestMapper extends BaseMapper<BookingRequest> {
    
    /**
     * 根据学生ID查询预约申请列表
     */
    @Select("SELECT * FROM booking_requests WHERE student_id = #{studentId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 根据教师ID查询预约申请列表
     */
    @Select("SELECT * FROM booking_requests WHERE teacher_id = #{teacherId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据状态查询预约申请列表
     */
    @Select("SELECT * FROM booking_requests WHERE status = #{status} AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByStatus(@Param("status") String status);
    
    /**
     * 查询指定学生的待处理预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE student_id = #{studentId} AND status = 'pending' AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findPendingByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 查询指定教师的待处理预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE teacher_id = #{teacherId} AND status = 'pending' AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findPendingByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 查询指定时间范围内的预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE " +
            "((booking_type = 'single' AND requested_date BETWEEN #{startDate} AND #{endDate}) OR " +
            "(booking_type = 'recurring' AND start_date <= #{endDate} AND end_date >= #{startDate})) " +
            "AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByDateRange(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    /**
     * 查询指定教师在指定时间范围内的预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE teacher_id = #{teacherId} AND " +
            "((booking_type = 'single' AND requested_date BETWEEN #{startDate} AND #{endDate}) OR " +
            "(booking_type = 'recurring' AND start_date <= #{endDate} AND end_date >= #{startDate})) " +
            "AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByTeacherIdAndDateRange(@Param("teacherId") Long teacherId, 
                                                    @Param("startDate") LocalDate startDate, 
                                                    @Param("endDate") LocalDate endDate);
    
    /**
     * 查询指定学生的试听课申请
     */
    @Select("SELECT * FROM booking_requests WHERE student_id = #{studentId} AND is_trial = 1 AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findTrialByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 统计指定学生的预约申请数量
     */
    @Select("SELECT COUNT(*) FROM booking_requests WHERE student_id = #{studentId} AND is_deleted = 0")
    int countByStudentId(@Param("studentId") Long studentId);
    
    /**
     * 统计指定教师的预约申请数量
     */
    @Select("SELECT COUNT(*) FROM booking_requests WHERE teacher_id = #{teacherId} AND is_deleted = 0")
    int countByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 统计指定状态的预约申请数量
     */
    @Select("SELECT COUNT(*) FROM booking_requests WHERE status = #{status} AND is_deleted = 0")
    int countByStatus(@Param("status") String status);
    
    /**
     * 查询最近的预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE is_deleted = 0 ORDER BY created_at DESC LIMIT #{limit}")
    List<BookingRequest> findRecentBookings(@Param("limit") int limit);
    
    /**
     * 查询指定时间之后创建的预约申请
     */
    @Select("SELECT * FROM booking_requests WHERE created_at >= #{dateTime} AND is_deleted = 0 ORDER BY created_at DESC")
    List<BookingRequest> findByCreatedAtAfter(@Param("dateTime") LocalDateTime dateTime);
}
