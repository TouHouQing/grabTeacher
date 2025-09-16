package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;
import java.math.BigDecimal;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {

    /**
     * 根据教师ID查询课程列表
     */
    @Select("SELECT * FROM courses WHERE teacher_id = #{teacherId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 批量查询多个教师的活跃课程
     */
    List<Course> findActiveByTeacherIds(@Param("teacherIds") List<Long> teacherIds);

    /**
     * 根据科目ID查询课程列表
     */
    @Select("SELECT * FROM courses WHERE subject_id = #{subjectId} AND is_deleted = 0 AND status = 'active' ORDER BY created_at DESC")
    List<Course> findBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 根据科目ID查询所有课程列表（包括非活跃状态）
     */
    @Select("SELECT * FROM courses WHERE subject_id = #{subjectId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findAllBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 查询指定教师的课程数量
     */
    @Select("SELECT COUNT(*) FROM courses WHERE teacher_id = #{teacherId} AND is_deleted = 0")
    int countByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 查询活跃状态的课程列表
     */
    @Select("SELECT * FROM courses WHERE status = 'active' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findActiveCourses();

    /**
     * 根据教师ID查询活跃状态的课程列表
     */
    @Select("SELECT * FROM courses WHERE teacher_id = #{teacherId} AND status = 'active' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findActiveByTeacherId(@Param("teacherId") Long teacherId);

    /**
     * 查询有活跃课程的教师ID列表
     */
    @Select("SELECT DISTINCT teacher_id FROM courses WHERE status = 'active' AND is_deleted = 0 ORDER BY teacher_id")
    List<Long> findActiveTeacherIds();

    /**
     * 查询精选课程列表
     */
    @Select("SELECT * FROM courses WHERE is_featured = 1 AND status = 'active' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findFeaturedCourses();

    /**
     * 根据科目ID查询精选课程列表
     */
    @Select("SELECT * FROM courses WHERE is_featured = 1 AND subject_id = #{subjectId} AND status = 'active' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findFeaturedCoursesBySubjectId(@Param("subjectId") Long subjectId);

    /**
     * 获取所有精选课程的ID列表
     */
    @Select("SELECT id FROM courses WHERE is_featured = 1 AND is_deleted = 0 ORDER BY created_at DESC")
    List<Long> findFeaturedCourseIds();

    /**
     * 报名人数 +1（带状态置满判断）
     * 当 person_limit 不为空且 enrollment_count+1 >= person_limit 时，将 status 置为 'full'
     * 仅对 is_deleted = 0 的记录生效
     */
    @Update("UPDATE courses SET enrollment_count = enrollment_count + 1, status = CASE WHEN person_limit IS NOT NULL AND enrollment_count + 1 >= person_limit THEN 'full' ELSE status END WHERE id = #{courseId} AND is_deleted = 0 AND (person_limit IS NULL OR enrollment_count < person_limit)")
    int incrementEnrollmentAndSetFullIfNeeded(@Param("courseId") Long courseId);

    @Update("UPDATE courses SET current_hours = COALESCE(current_hours, 0) + #{hours} WHERE id = #{courseId} AND is_deleted = 0 AND course_type = 'one_on_one'")
    int incrementCourseCurrentHours(@Param("courseId") Long courseId, @Param("hours") BigDecimal hours);

    @Update("UPDATE courses SET last_hours = COALESCE(current_hours, 0), current_hours = 0 WHERE is_deleted = 0 AND course_type = 'one_on_one'")
    int resetMonthlyHoursOneOnOne();

    @Select("SELECT COALESCE(SUM(current_hours), 0) FROM courses WHERE is_deleted = 0 AND course_type = 'one_on_one' AND teacher_id = #{teacherId}")
    BigDecimal sumCurrentHoursByTeacher(@Param("teacherId") Long teacherId);

    @Select("SELECT COALESCE(SUM(last_hours), 0) FROM courses WHERE is_deleted = 0 AND course_type = 'one_on_one' AND teacher_id = #{teacherId}")
    BigDecimal sumLastHoursByTeacher(@Param("teacherId") Long teacherId);

    /**
     *   /
     */
    java.util.List<java.util.Map<String, Object>> sumHoursByTeacherIds(@org.apache.ibatis.annotations.Param("teacherIds") java.util.List<Long> teacherIds);

}
