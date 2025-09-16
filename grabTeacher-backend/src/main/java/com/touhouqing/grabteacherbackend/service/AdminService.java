package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.entity.User;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.vo.AdminStudentDetailVO;
import com.touhouqing.grabteacherbackend.model.vo.AdminTeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import com.touhouqing.grabteacherbackend.model.dto.AdminProfileUpdateDTO;

import java.util.List;
import java.util.Map;

public interface AdminService {

    /**
     * 获取系统统计信息
     */
    Map<String, Object> getSystemStatistics();

    /**
     * 获取用户列表
     */
    List<User> getUserList(int page, int size, String userType, String keyword);

    /**
     * 更新用户状态
     */
    void updateUserStatus(Long userId, String status);

    // ===================== 学生管理方法 =====================

    /**
     * 分页获取学生列表
     */
    Page<Student> getStudentList(int page, int size, String keyword);

    /**
     * 根据ID获取学生信息
     */
    Student getStudentById(Long studentId);

    /**
     * 根据ID获取学生详情（管理员端）
     */
    AdminStudentDetailVO getStudentDetailById(Long studentId);

    /**
     * 添加学生
     */
    Student addStudent(StudentInfoDTO request);

    /**
     * 更新学生信息
     */
    Student updateStudent(Long studentId, StudentInfoDTO request);

    /**
     * 更新学生信息（包含操作员ID）
     */
    Student updateStudent(Long studentId, StudentInfoDTO request, Long operatorId);

    /**
     * 删除学生
     */
    void deleteStudent(Long studentId);

    // ===================== 教师管理方法 =====================

    /**
     * 分页获取教师列表
     */
    Page<Teacher> getTeacherList(int page, int size, String keyword, String subject, String gender, Boolean isVerified);

    /**
     * 根据ID获取教师信息
     */
    Teacher getTeacherById(Long teacherId);

    /**
     * 根据ID获取教师详情（管理员端）
     */
    AdminTeacherDetailVO getTeacherDetailById(Long teacherId);

    /**
     * 添加教师
     */
    Teacher addTeacher(TeacherInfoDTO request);

    /**
     * 更新教师信息
     */
    Teacher updateTeacher(Long teacherId, TeacherInfoDTO request);

    /**
     * 删除教师
     */
    void deleteTeacher(Long teacherId);

    /**
     * 审核教师
     */
    void verifyTeacher(Long teacherId, Boolean isVerified);

    /**
     * 设置教师为精选教师
     */
    void setTeacherFeatured(Long teacherId, Boolean isFeatured);

    /**
     * 获取教师科目列表
     */
    List<Long> getTeacherSubjects(Long teacherId);

    /**
     * 批量获取多个教师的科目ID映射
     */
    java.util.Map<Long, java.util.List<Long>> getSubjectsByTeacherIds(java.util.List<Long> teacherIds);

    // ============== 管理员资料 ==============
    /** 获取当前管理员资料（根据当前登录用户） */
    Map<String, Object> getCurrentAdminProfile(Long currentUserId);

    /** 更新当前管理员资料（可改姓名/邮箱/号码，并覆盖头像、二维码） */
    void updateCurrentAdminProfile(Long currentUserId, AdminProfileUpdateDTO dto);

    /**
     * 获取学生感兴趣的科目列表
     */
    List<Long> getStudentSubjects(Long studentId);

    /**
     * 批量更新某教师名下的一对一课程的时薪与本月课时，并记录管理员调整明细
     */
    void updateOneOnOneCourseMetrics(Long teacherId,
                                     java.util.List<com.touhouqing.grabteacherbackend.model.dto.AdminCourseMetricsUpdateDTO> items,
                                     Long operatorId);
}
