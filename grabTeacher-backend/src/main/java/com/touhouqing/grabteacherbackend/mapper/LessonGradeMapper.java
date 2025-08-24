package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.entity.LessonGrade;
import com.touhouqing.grabteacherbackend.model.vo.LessonGradeVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface LessonGradeMapper extends BaseMapper<LessonGrade> {

    /**
     * 根据课程安排ID和学生ID查询成绩
     */
    LessonGrade selectByScheduleIdAndStudentId(@Param("scheduleId") Long scheduleId, @Param("studentId") Long studentId);

    /**
     * 分页查询学生成绩（包含详细信息）
     */
    IPage<LessonGradeVO> selectGradesByStudentId(Page<LessonGradeVO> page, 
                                                 @Param("studentId") Long studentId,
                                                 @Param("subjectName") String subjectName,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 分页查询教师录入的成绩（包含详细信息）
     */
    IPage<LessonGradeVO> selectGradesByTeacherId(Page<LessonGradeVO> page,
                                                 @Param("teacherId") Long teacherId,
                                                 @Param("studentName") String studentName,
                                                 @Param("subjectName") String subjectName,
                                                 @Param("startDate") LocalDate startDate,
                                                 @Param("endDate") LocalDate endDate);

    /**
     * 查询学生某科目的所有成绩（用于图表展示）
     */
    List<LessonGradeVO> selectGradesByStudentIdAndSubject(@Param("studentId") Long studentId,
                                                          @Param("subjectName") String subjectName);

    /**
     * 查询学生在指定日期范围内的成绩
     */
    List<LessonGradeVO> selectGradesByStudentIdAndDateRange(@Param("studentId") Long studentId,
                                                            @Param("startDate") LocalDate startDate,
                                                            @Param("endDate") LocalDate endDate);

    /**
     * 统计学生成绩数据
     */
    Long countGradesByStudentId(@Param("studentId") Long studentId);

    /**
     * 统计教师录入成绩数据
     */
    Long countGradesByTeacherId(@Param("teacherId") Long teacherId);
}
