package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.CourseDTO;
import com.touhouqing.grabteacherbackend.model.vo.CourseVO;
import com.touhouqing.grabteacherbackend.model.entity.Course;

import java.util.List;

public interface CourseService {
    
    /**
     * 创建课程
     * @param request 课程请求参数
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 创建的课程
     */
    Course createCourse(CourseDTO request, Long currentUserId, String userType);
    
    /**
     * 更新课程
     * @param id 课程ID
     * @param request 课程请求参数
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 更新后的课程
     */
    Course updateCourse(Long id, CourseDTO request, Long currentUserId, String userType);
    
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
    CourseVO getCourseById(Long id);
    
    /**
     * 获取课程列表（分页） - 公开端建议使用（带缓存）
     */
    Page<CourseVO> getCourseList(int page, int size, String keyword, Long subjectId,
                                 Long teacherId, String status, String courseType);

    /**
     * 获取课程列表（分页） - 管理端使用（直查DB，不缓存）
     */
    Page<CourseVO> getCourseListNoCache(int page, int size, String keyword, Long subjectId,
                                        Long teacherId, String status, String courseType);

    /**
     * 获取教师的课程列表
     * @param teacherId 教师ID
     * @param currentUserId 当前用户ID
     * @param userType 用户类型
     * @return 课程列表
     */
    List<CourseVO> getTeacherCourses(Long teacherId, Long currentUserId, String userType);

    /**
     * 获取活跃状态的课程列表
     * @return 活跃课程列表
     */
    List<CourseVO> getActiveCourses();

    /**
     * 获取活跃状态的课程（限制条数）
     */
    List<CourseVO> getActiveCoursesLimited(Integer limit);

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

    /**
     * 获取精选课程列表（分页）
     * @param page 页码
     * @param size 每页大小
     * @param subjectId 科目ID
     * @return 分页精选课程列表
     */
    Page<CourseVO> getFeaturedCourses(int page, int size, Long subjectId);

    /**
     * 获取所有精选课程列表（不分页，用于首页滚动展示）
     * @return 所有精选课程列表
     */
    List<CourseVO> getAllFeaturedCourses();

    // 管理端：直查 DB 的精选课程分页
    Page<CourseVO> getFeaturedCoursesNoCache(int page, int size, Long subjectId);

    /**
     * 设置课程为精选课程
     * @param courseId 课程ID
     * @param featured 是否精选
     */
    void setCourseAsFeatured(Long courseId, boolean featured);

    /**
     * 批量设置精选课程
     * @param courseIds 课程ID列表
     * @param featured 是否精选
     */
    void batchSetFeaturedCourses(List<Long> courseIds, boolean featured);

    /**
     * 获取所有精选课程ID列表（公开/缓存用）
     */
    List<Long> getFeaturedCourseIds();

    /**
     * 获取所有精选课程ID列表（管理端强一致，直查 DB）
     */
    List<Long> getFeaturedCourseIdsNoCache();
}
