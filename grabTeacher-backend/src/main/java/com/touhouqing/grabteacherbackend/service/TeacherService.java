package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.vo.TeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherListVO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherMatchDTO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherMatchVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherProfileVO;
import com.touhouqing.grabteacherbackend.model.vo.TeacherScheduleVO;
import com.touhouqing.grabteacherbackend.model.vo.ClassRecordVO;

import com.touhouqing.grabteacherbackend.model.entity.Teacher;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

public interface TeacherService {

    /**
     * 根据用户ID获取教师信息
     */
    Teacher getTeacherByUserId(Long userId);

    /**
     * 根据ID获取教师信息
     */
    Teacher getTeacherById(Long teacherId);

    /**
     * 根据ID获取教师详情（包含用户信息、科目信息）
     */
    TeacherDetailVO getTeacherDetailById(Long teacherId);

    /**
     * 根据用户ID获取教师详细信息（包含科目信息）
     */
    TeacherProfileVO getTeacherProfileByUserId(Long userId);

    /**
     * 获取教师列表
     */
    List<Teacher> getTeacherList(int page, int size, String subject, String keyword);

    /**
     * 获取教师列表（包含科目信息）
     */
    List<TeacherListVO> getTeacherListWithSubjects(int page, int size, String subject, String keyword);

    /**
     * 统计教师总数（用于分页）
     */
    long countTeachers(String subject, String keyword);

    /**
     * 获取精选教师列表（天下名师页面使用）
     */
    List<TeacherListVO> getFeaturedTeachers(int page, int size, String subject, String keyword);

    /**
     * 获取教师列表（包含科目信息，支持精确姓名）
     */
    default java.util.List<TeacherListVO> getTeacherListWithSubjects(int page, int size, String subject, String keyword, String realNameExact) {
        return getTeacherListWithSubjects(page, size, subject, keyword);
    }

    /**
     * 统计教师总数（支持精确姓名）
     */
    default long countTeachers(String subject, String keyword, String realNameExact) {
        return countTeachers(subject, keyword);
    }

    /**
     * 统计精选教师总数（支持筛选）
     */
    long countFeaturedTeachers(String subject, String keyword);

    /**
     * 更新教师信息
     */
    Teacher updateTeacherInfo(Long userId, TeacherInfoDTO request);

    /**
     * 匹配教师
     */
    List<TeacherMatchVO> matchTeachers(TeacherMatchDTO request);


    /**
     * 获取教师的公开课表（供学生查看）
     * @param teacherId 教师ID
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 教师课表信息
     */
    TeacherScheduleVO getTeacherPublicSchedule(Long teacherId, LocalDate startDate, LocalDate endDate);


    /**
     * 获取教师控制台统计数据
     * @param userId 教师用户ID
     * @return 统计数据
     */
    Map<String, Object> getTeacherStatistics(Long userId);

    /**
     * 获取教师课时详情统计
     * @param userId 教师用户ID
     * @return 课时详情统计
     */
    Map<String, Object> getHourDetailsSummary(Long userId);

    /**
     * 获取教师上课记录
     * @param userId 教师用户ID
     * @param page 页码
     * @param size 每页大小
     * @param year 年份筛选
     * @param month 月份筛选
     * @param studentName 学生姓名筛选
     * @param courseName 课程名称筛选
     * @return 上课记录分页数据
     */
    Page<ClassRecordVO> getClassRecords(Long userId, int page, int size, Integer year, Integer month, String studentName, String courseName);
}