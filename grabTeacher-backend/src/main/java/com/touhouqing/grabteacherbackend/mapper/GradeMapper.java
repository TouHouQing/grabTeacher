package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.Grade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

/**
 * 年级Mapper接口
 */
@Mapper
public interface GradeMapper extends BaseMapper<Grade> {

    /**
     * 根据年级名称查找年级（不包括已删除的）
     */
    @Select("SELECT * FROM grades WHERE grade_name = #{gradeName} AND is_deleted = false")
    Grade findByGradeName(String gradeName);

    /**
     * 根据年级名称查找年级（不包括已删除的，排除指定ID）
     */
    @Select("SELECT * FROM grades WHERE grade_name = #{gradeName} AND is_deleted = false AND id != #{excludeId}")
    Grade findByGradeNameExcludeId(String gradeName, Long excludeId);

    /**
     * 统计使用指定年级的课程数量
     */
    @Select("SELECT COUNT(*) FROM course_grades WHERE grade = #{gradeName}")
    Long countCoursesByGradeName(String gradeName);
}
