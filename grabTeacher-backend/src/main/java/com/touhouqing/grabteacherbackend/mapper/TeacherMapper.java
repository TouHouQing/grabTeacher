package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    /**
     * 根据用户ID查找教师信息
     */
    @Select("SELECT * FROM teachers WHERE user_id = #{userId} AND is_deleted = 0")
    Teacher findByUserId(@Param("userId") Long userId);

    /**
     * 根据科目查找教师 - 优化查询
     */
    @Select("SELECT DISTINCT t.* FROM teachers t " +
            "INNER JOIN teacher_subjects ts ON t.id = ts.teacher_id " +
            "INNER JOIN subjects s ON ts.subject_id = s.id " +
            "WHERE t.is_deleted = 0 AND t.is_verified = 1 " +
            "AND s.name = #{subject} AND s.is_deleted = 0 " +
            "ORDER BY t.teaching_experience DESC")
    List<Teacher> findTeachersBySubject(@Param("subject") String subject);

    /**
     * 根据科目查找提供1对1课程的教师 - 智能匹配专用
     */
    @Select("SELECT DISTINCT t.* FROM teachers t " +
            "INNER JOIN teacher_subjects ts ON t.id = ts.teacher_id " +
            "INNER JOIN subjects s ON ts.subject_id = s.id " +
            "INNER JOIN courses c ON t.id = c.teacher_id " +
            "WHERE t.is_deleted = 0 AND t.is_verified = 1 " +
            "AND s.name = #{subject} AND s.is_deleted = 0 " +
            "AND c.is_deleted = 0 AND c.status = 'active' AND c.course_type = 'one_on_one' " +
            "ORDER BY t.teaching_experience DESC")
    List<Teacher> findOneOnOneTeachersBySubject(@Param("subject") String subject);

    /**
     * 按科目分页查询教师并支持关键字过滤（SQL端分页）
     */
    @Select({
        "<script>",
        "SELECT DISTINCT t.* FROM teachers t",
        "INNER JOIN teacher_subjects ts ON t.id = ts.teacher_id",
        "INNER JOIN subjects s ON ts.subject_id = s.id",
        "WHERE t.is_deleted = 0 AND t.is_verified = 1",
        "AND s.name = #{subject} AND s.is_deleted = 0",
        "<if test='keyword != null and keyword != \"\"'>",
        "  AND (t.real_name LIKE CONCAT('%', #{keyword}, '%')",
        "       OR t.specialties LIKE CONCAT('%', #{keyword}, '%')",
        "       OR t.introduction LIKE CONCAT('%', #{keyword}, '%'))",
        "</if>",
        "ORDER BY t.id DESC",
        "LIMIT #{size} OFFSET #{offset}",
        "</script>"
    })
    List<Teacher> findTeachersBySubjectPaged(@Param("subject") String subject,
                                             @Param("keyword") String keyword,
                                             @Param("offset") int offset,
                                             @Param("size") int size);


    /**
     * 查询活跃教师ID列表（按教学经验排序）
     */
    @Select("SELECT id FROM teachers WHERE is_deleted = 0 AND is_verified = 1 " +
            "ORDER BY teaching_experience DESC, id DESC LIMIT 50")
    List<Long> findActiveTeacherIds();

    /**
     * 查找所有提供1对1课程的教师 - 智能匹配专用（没有指定科目时）
     */
    @Select("SELECT DISTINCT t.* FROM teachers t " +
            "INNER JOIN courses c ON t.id = c.teacher_id " +
            "WHERE t.is_deleted = 0 AND t.is_verified = 1 " +
            "AND c.is_deleted = 0 AND c.status = 'active' AND c.course_type = 'one_on_one' " +
            "ORDER BY t.teaching_experience DESC")
    List<Teacher> findAllOneOnOneTeachers();

    /**
     * 教师本月课时增量累加
     */
    @Update("UPDATE teachers SET current_hours = COALESCE(current_hours, 0) + #{hours} WHERE id = #{teacherId} AND is_deleted = 0")
    int incrementCurrentHours(@Param("teacherId") Long teacherId, @Param("hours") java.math.BigDecimal hours);

    /**
     * 月初重置：将 current_hours 赋值到 last_hours，并清零 current_hours
     */
    @Update("UPDATE teachers SET last_hours = COALESCE(current_hours, 0), current_hours = 0 WHERE is_deleted = 0")
    int resetMonthlyHours();

    /**
     * 教师本月课时减量（不低于0）
     */
    @Update("UPDATE teachers SET current_hours = CASE WHEN current_hours IS NULL THEN 0 WHEN current_hours - #{hours} < 0 THEN 0 ELSE current_hours - #{hours} END WHERE id = #{teacherId} AND is_deleted = 0")
    int decrementCurrentHours(@Param("teacherId") Long teacherId, @Param("hours") java.math.BigDecimal hours);
}