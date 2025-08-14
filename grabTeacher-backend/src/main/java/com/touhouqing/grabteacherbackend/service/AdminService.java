package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.entity.User;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import com.touhouqing.grabteacherbackend.entity.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.entity.dto.TeacherInfoRequest;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

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
    Page<Student> getStudentList(int page, int size, String keyword, String gradeLevel);
    
    /**
     * 根据ID获取学生信息
     */
    Student getStudentById(Long studentId);
    
    /**
     * 添加学生
     */
    Student addStudent(StudentInfoRequest request);
    
    /**
     * 更新学生信息
     */
    Student updateStudent(Long studentId, StudentInfoRequest request);
    
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
     * 添加教师
     */
    Teacher addTeacher(TeacherInfoRequest request);
    
    /**
     * 更新教师信息
     */
    Teacher updateTeacher(Long teacherId, TeacherInfoRequest request);
    
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
     * 获取学生感兴趣的科目列表
     */
    List<Long> getStudentSubjects(Long studentId);
}