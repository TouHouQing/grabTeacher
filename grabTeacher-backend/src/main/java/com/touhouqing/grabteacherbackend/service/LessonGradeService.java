package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeQueryDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.LessonGrade;
import com.touhouqing.grabteacherbackend.model.vo.LessonGradeVO;

import java.util.List;

public interface LessonGradeService {

    /**
     * 录入课程成绩
     * @param userId 当前用户ID（教师）
     * @param request 成绩录入请求
     * @return 录入的成绩记录
     */
    LessonGrade createGrade(Long userId, LessonGradeCreateDTO request);

    /**
     * 更新课程成绩
     * @param userId 当前用户ID（教师）
     * @param request 成绩更新请求
     * @return 更新后的成绩记录
     */
    LessonGrade updateGrade(Long userId, LessonGradeUpdateDTO request);

    /**
     * 删除课程成绩
     * @param userId 当前用户ID（教师）
     * @param gradeId 成绩记录ID
     */
    void deleteGrade(Long userId, Long gradeId);

    /**
     * 根据课程安排ID和学生ID查询成绩
     * @param scheduleId 课程安排ID
     * @param studentId 学生ID
     * @return 成绩记录
     */
    LessonGrade getGradeByScheduleAndStudent(Long scheduleId, Long studentId);

    /**
     * 分页查询学生成绩
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<LessonGradeVO> getGradesByStudent(LessonGradeQueryDTO request);

    /**
     * 分页查询教师录入的成绩
     * @param userId 教师用户ID
     * @param request 查询条件
     * @return 分页结果
     */
    IPage<LessonGradeVO> getGradesByTeacher(Long userId, LessonGradeQueryDTO request);

    /**
     * 查询学生某科目的所有成绩（用于图表展示）
     * @param studentId 学生ID
     * @param subjectName 科目名称
     * @return 成绩列表
     */
    List<LessonGradeVO> getGradesByStudentAndSubject(Long studentId, String subjectName);

    /**
     * 查询学生在指定日期范围内的成绩
     * @param studentId 学生ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 成绩列表
     */
    List<LessonGradeVO> getGradesByStudentAndDateRange(Long studentId, String startDate, String endDate);

    /**
     * 获取成绩统计信息
     * @param studentId 学生ID（可选）
     * @param teacherId 教师ID（可选）
     * @return 统计信息
     */
    Long getGradeCount(Long studentId, Long teacherId);

    /**
     * 检查教师是否有权限录入该课程成绩
     * @param userId 教师用户ID
     * @param scheduleId 课程安排ID
     * @return 是否有权限
     */
    boolean hasPermissionToGrade(Long userId, Long scheduleId);
}
