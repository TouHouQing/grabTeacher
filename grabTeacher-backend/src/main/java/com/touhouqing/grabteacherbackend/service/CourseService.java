package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.CourseRequest;
import com.touhouqing.grabteacherbackend.dto.CourseResponse;
import com.touhouqing.grabteacherbackend.entity.Course;

import java.util.List;

public interface CourseService {
    
    /**
     * 创建课程
     * @param request 课程请求参数
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 创建的课程
     */
    Course createCourse(CourseRequest request, Long currentUserId, String userType);
    
    /**
     * 更新课程
     * @param id 课程ID
     * @param request 课程请求参数
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 更新后的课程
     */
    Course updateCourse(Long id, CourseRequest request, Long currentUserId, String userType);
    
    /**
     * 删除课程（软删除）
     * @param id 课程ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     */
    void deleteCourse(Long id, Long currentUserId, String userType);
    
    /**
     * 根据ID获取课程详情
     * @param id 课程ID
     * @return 课程响应数据
     */
    CourseResponse getCourseById(Long id);
    
    /**
     * 获取课程列表（分页）
     * @param page 页码
     * @param size 每页大小
     * @param keyword 搜索关键词
     * @param subjectId 科目ID
     * @param teacherId 教师ID
     * @param status 课程状态
     * @param courseType 课程类型
     * @return 分页课程列表
     */
    Page<CourseResponse> getCourseList(int page, int size, String keyword, Long subjectId,
                                      Long teacherId, String status, String courseType, String grade);
    
    /**
     * 获取教师的课程列表
     * @param teacherId 教师ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 课程列表
     */
    List<CourseResponse> getTeacherCourses(Long teacherId, Long currentUserId, String userType);
    
    /**
     * 获取活跃状态的课程列表
     * @return 活跃课程列表
     */
    List<CourseResponse> getActiveCourses();
    
    /**
     * 更新课程状态
     * @param id 课程ID
     * @param status 新状态
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     */
    void updateCourseStatus(Long id, String status, Long currentUserId, String userType);
    
    /**
     * 检查用户是否有权限操作课程
     * @param courseId 课程ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 是否有权限
     */
    boolean hasPermissionToManageCourse(Long courseId, Long currentUserId, String userType);
}
