package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
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
     * 根据年级查找教师 - 优化查询
     */
    @Select("SELECT DISTINCT t.* FROM teachers t " +
            "INNER JOIN courses c ON t.id = c.teacher_id " +
            "INNER JOIN course_grades cg ON c.id = cg.course_id " +
            "WHERE t.is_deleted = 0 AND t.is_verified = 1 " +
            "AND cg.grade = #{grade} AND c.is_deleted = 0 " +
            "ORDER BY t.teaching_experience DESC")
    List<Teacher> findTeachersByGrade(@Param("grade") String grade);

    /**
     * 根据科目和年级查找教师 - 优化查询
     */
    @Select("SELECT DISTINCT t.* FROM teachers t " +
            "INNER JOIN teacher_subjects ts ON t.id = ts.teacher_id " +
            "INNER JOIN subjects s ON ts.subject_id = s.id " +
            "INNER JOIN courses c ON t.id = c.teacher_id " +
            "INNER JOIN course_grades cg ON c.id = cg.course_id " +
            "WHERE t.is_deleted = 0 AND t.is_verified = 1 " +
            "AND s.name = #{subject} AND s.is_deleted = 0 " +
            "AND cg.grade = #{grade} AND c.is_deleted = 0 " +
            "ORDER BY t.teaching_experience DESC")
    List<Teacher> findTeachersBySubjectAndGrade(@Param("subject") String subject, @Param("grade") String grade);
}