package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.CourseVO;
import com.touhouqing.grabteacherbackend.model.entity.User;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.vo.AdminStudentDetailVO;
import com.touhouqing.grabteacherbackend.model.vo.AdminTeacherDetailVO;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.dto.TeacherInfoDTO;
import com.touhouqing.grabteacherbackend.service.AdminService;
import com.touhouqing.grabteacherbackend.service.CourseService;
import com.touhouqing.grabteacherbackend.service.MessageService;
import com.touhouqing.grabteacherbackend.service.ScheduleCleanupService;
import jakarta.validation.Valid;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.touhouqing.grabteacherbackend.model.dto.AdminProfileUpdateDTO;
import com.touhouqing.grabteacherbackend.mapper.BalanceTransactionMapper;
import com.touhouqing.grabteacherbackend.model.entity.BalanceTransaction;
import com.touhouqing.grabteacherbackend.model.dto.MessageCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageUpdateDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageQueryDTO;
import com.touhouqing.grabteacherbackend.model.vo.MessageVO;
import com.touhouqing.grabteacherbackend.model.entity.Message;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import com.touhouqing.grabteacherbackend.model.entity.HourDetail;
import com.touhouqing.grabteacherbackend.mapper.HourDetailMapper;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "管理员功能", description = "管理员专用功能接口")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminService adminService;

    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;


    @Autowired
    private CourseService courseService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private ScheduleCleanupService scheduleCleanupService;

    @Autowired
    private HourDetailMapper hourDetailMapper;

    /**
     * 获取系统统计信息
     */
    @Operation(summary = "获取系统统计信息", description = "获取用户数量、教师数量等统计数据")
    @GetMapping("/statistics")
    public ResponseEntity<CommonResult<Map<String, Object>>> getStatistics() {
        try {
            Map<String, Object> statistics = adminService.getSystemStatistics();
            return ResponseEntity.ok(CommonResult.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取统计信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取用户列表
     */
    @GetMapping("/users")
    public ResponseEntity<CommonResult<List<User>>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String keyword) {
        try {
            List<User> users = adminService.getUserList(page, size, userType, keyword);
            return ResponseEntity.ok(CommonResult.success("获取成功", users));
        } catch (Exception e) {
            logger.error("获取用户列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 更新用户状态
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<CommonResult<Object>> updateUserStatus(
            @PathVariable Long userId,
            @RequestParam String status) {
        try {
            adminService.updateUserStatus(userId, status);
            return ResponseEntity.ok(CommonResult.success("更新成功", null));
        } catch (RuntimeException e) {
            logger.warn("更新用户状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新用户状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    // ===================== 学生管理接口 =====================

    /**
     * 获取学生列表（分页查询）
     */
    @Operation(summary = "获取学生列表", description = "分页查询学生信息，支持按姓名搜索")
    @GetMapping("/students")
    public ResponseEntity<CommonResult<Page<Student>>> getStudentList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword
) {
        try {
            Page<Student> students = adminService.getStudentList(page, size, keyword);
            return ResponseEntity.ok(CommonResult.success("获取成功", students));
        } catch (Exception e) {
            logger.error("获取学生列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 根据ID获取学生详细信息
     */
    @Operation(summary = "获取学生详细信息", description = "根据学生ID获取详细信息")
    @GetMapping("/students/{studentId}")
    public ResponseEntity<CommonResult<AdminStudentDetailVO>> getStudentById(@PathVariable Long studentId) {
        try {
            AdminStudentDetailVO student = adminService.getStudentDetailById(studentId);
            if (student == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(CommonResult.success("获取成功", student));
        } catch (Exception e) {
            logger.error("获取学生详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 添加学生
     */
    @Operation(summary = "添加学生", description = "管理员添加新学生")
    @PostMapping("/students")
    public ResponseEntity<CommonResult<Student>> addStudent(@RequestBody StudentInfoDTO request) {
        try {
            Student student = adminService.addStudent(request);
            return ResponseEntity.ok(CommonResult.success("添加成功", student));
        } catch (RuntimeException e) {
            logger.warn("添加学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("添加失败"));
        }
    }

    /**
     * 编辑学生信息
     */
    @Operation(summary = "编辑学生信息", description = "管理员编辑学生信息")
    @PutMapping("/students/{studentId}")
    public ResponseEntity<CommonResult<Student>> updateStudent(
            @PathVariable Long studentId,
            @RequestBody StudentInfoDTO request,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Student student = adminService.updateStudent(studentId, request, userPrincipal.getId());
            return ResponseEntity.ok(CommonResult.success("更新成功", student));
        } catch (RuntimeException e) {
            logger.warn("更新学生信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    /**
     * 删除学生
     */
    @Operation(summary = "删除学生", description = "管理员删除学生（逻辑删除）")
    @DeleteMapping("/students/{studentId}")
    public ResponseEntity<CommonResult<Object>> deleteStudent(@PathVariable Long studentId) {
        try {
            adminService.deleteStudent(studentId);
            return ResponseEntity.ok(CommonResult.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除学生失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除学生异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("删除失败"));
        }
    }

    // ===================== 教师管理接口 =====================

    /**
     * 获取教师列表（分页查询）
     */
    @Operation(summary = "获取教师列表", description = "分页查询教师信息，支持按姓名、科目搜索")
    @GetMapping("/teachers")
    public ResponseEntity<CommonResult<Page<Teacher>>> getTeacherList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Boolean isVerified) {
        try {
            Page<Teacher> teachers = adminService.getTeacherList(page, size, keyword, subject, gender, isVerified);
            return ResponseEntity.ok(CommonResult.success("获取成功", teachers));
        } catch (Exception e) {
            logger.error("获取教师列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 根据ID获取教师详细信息
     */
    @Operation(summary = "获取教师详细信息", description = "根据教师ID获取详细信息")
    @GetMapping("/teachers/{teacherId}")
    public ResponseEntity<CommonResult<AdminTeacherDetailVO>> getTeacherById(@PathVariable Long teacherId) {
        try {
            AdminTeacherDetailVO teacher = adminService.getTeacherDetailById(teacherId);
            if (teacher == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(CommonResult.success("获取成功", teacher));
        } catch (Exception e) {
            logger.error("获取教师详细信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 添加教师
     */
    @Operation(summary = "添加教师", description = "管理员添加新教师")
    @PostMapping("/teachers")
    public ResponseEntity<CommonResult<Teacher>> addTeacher(@Valid @RequestBody TeacherInfoDTO request) {
        try {
            Teacher teacher = adminService.addTeacher(request);
            return ResponseEntity.ok(CommonResult.success("添加成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("添加教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("添加教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("添加失败"));
        }
    }

    /**
     * 编辑教师信息
     */
    @Operation(summary = "编辑教师信息", description = "管理员编辑教师信息")
    @PutMapping("/teachers/{teacherId}")
    public ResponseEntity<CommonResult<Teacher>> updateTeacher(
            @PathVariable Long teacherId,
            @Valid @RequestBody TeacherInfoDTO request) {
        try {
            Teacher teacher = adminService.updateTeacher(teacherId, request);
            return ResponseEntity.ok(CommonResult.success("更新成功", teacher));
        } catch (RuntimeException e) {
            logger.warn("更新教师信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新教师信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    /**
     * 删除教师
     */
    @Operation(summary = "删除教师", description = "管理员删除教师（逻辑删除）")
    @DeleteMapping("/teachers/{teacherId}")
    public ResponseEntity<CommonResult<Object>> deleteTeacher(@PathVariable Long teacherId) {
        try {
            adminService.deleteTeacher(teacherId);
            return ResponseEntity.ok(CommonResult.success("删除成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("删除失败"));
        }
    }

    /**
     * 审核教师
     */
    @Operation(summary = "审核教师", description = "管理员审核教师认证状态")
    @PutMapping("/teachers/{teacherId}/verify")
    public ResponseEntity<CommonResult<Object>> verifyTeacher(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean isVerified = requestBody.get("isVerified");
            adminService.verifyTeacher(teacherId, isVerified);
            return ResponseEntity.ok(CommonResult.success("审核成功", null));
        } catch (RuntimeException e) {
            logger.warn("审核教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("审核教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("审核失败"));
        }
    }

    /**
     * 设置教师为精选教师
     */
    @Operation(summary = "设置精选教师", description = "管理员设置教师是否为天下名师")
    @PutMapping("/teachers/{teacherId}/featured")
    public ResponseEntity<CommonResult<Object>> setTeacherFeatured(
            @PathVariable Long teacherId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean isFeatured = requestBody.get("isFeatured");
            adminService.setTeacherFeatured(teacherId, isFeatured);
            String message = isFeatured ? "设置为天下名师成功" : "取消天下名师成功";
            return ResponseEntity.ok(CommonResult.success(message, null));
        } catch (RuntimeException e) {
            logger.warn("设置精选教师失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置精选教师异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("设置失败"));
        }
    }

    /**
     * 获取教师科目列表
     */
    @Operation(summary = "获取教师科目列表", description = "获取指定教师的科目ID列表")
    @GetMapping("/teachers/{teacherId}/subjects")
    public ResponseEntity<CommonResult<List<Long>>> getTeacherSubjects(@PathVariable Long teacherId) {
        try {
            List<Long> subjectIds = adminService.getTeacherSubjects(teacherId);
            return ResponseEntity.ok(CommonResult.success("获取成功", subjectIds));
        } catch (Exception e) {
            logger.error("获取教师科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    @Operation(summary = "批量获取教师科目映射", description = "根据多个教师ID，返回每个教师的科目ID列表映射")
    @PostMapping("/teachers/subjects/batch")
    public ResponseEntity<CommonResult<java.util.Map<Long, java.util.List<Long>>>> getSubjectsByTeacherIds(@RequestBody java.util.List<Long> teacherIds) {
        try {
            java.util.Map<Long, java.util.List<Long>> map = adminService.getSubjectsByTeacherIds(teacherIds);
            return ResponseEntity.ok(CommonResult.success("获取成功", map));
        } catch (Exception e) {
            logger.error("批量获取教师科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取学生感兴趣的科目列表
     */
    @Operation(summary = "获取学生科目列表", description = "获取指定学生感兴趣的科目ID列表")
    @GetMapping("/students/{studentId}/subjects")
    public ResponseEntity<CommonResult<List<Long>>> getStudentSubjects(@PathVariable Long studentId) {
        try {
            List<Long> subjectIds = adminService.getStudentSubjects(studentId);
            return ResponseEntity.ok(CommonResult.success("获取成功", subjectIds));
        } catch (Exception e) {
            logger.error("获取学生科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }


    // ==================== 精选课程管理接口 ====================

    /**
     * 获取精选课程列表
     */
    @Operation(summary = "获取精选课程列表", description = "获取所有精选课程的分页列表")
    @GetMapping("/featured-courses")
    public ResponseEntity<CommonResult<Map<String, Object>>> getFeaturedCourses(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long subjectId
) {
        try {
            // 管理端一致性优先：直查 DB 不走缓存
            Page<CourseVO> coursePage = courseService.getFeaturedCoursesNoCache(page, size, subjectId);

            Map<String, Object> response = new HashMap<>();
            response.put("courses", coursePage.getRecords());
            response.put("total", coursePage.getTotal());
            response.put("current", coursePage.getCurrent());
            response.put("size", coursePage.getSize());
            response.put("pages", coursePage.getPages());

            return ResponseEntity.ok(CommonResult.success("获取精选课程列表成功", response));
        } catch (Exception e) {
            logger.error("获取精选课程列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    // ============== 管理员资料 ==============
    @Operation(summary = "获取当前管理员资料")
    @GetMapping("/profile")
    public ResponseEntity<CommonResult<Map<String, Object>>> getMyProfile(org.springframework.security.core.Authentication authentication) {
        Long userId = ((com.touhouqing.grabteacherbackend.security.UserPrincipal) authentication.getPrincipal()).getId();
        Map<String, Object> profile = adminService.getCurrentAdminProfile(userId);
        return ResponseEntity.ok(CommonResult.success("获取成功", profile));
    }

    @Operation(summary = "更新当前管理员资料（覆盖头像/二维码：新传则删旧）")
    @PutMapping("/profile")
    public ResponseEntity<CommonResult<Object>> updateMyProfile(
            org.springframework.security.core.Authentication authentication,
            @RequestBody AdminProfileUpdateDTO dto) {
        Long userId = ((com.touhouqing.grabteacherbackend.security.UserPrincipal) authentication.getPrincipal()).getId();
        adminService.updateCurrentAdminProfile(userId, dto);
        return ResponseEntity.ok(CommonResult.success("更新成功", null));
    }

    /**
     * 获取所有精选课程ID列表
     */
    @Operation(summary = "获取精选课程ID列表", description = "获取所有精选课程的ID列表")
    @GetMapping("/featured-courses/ids")
    public ResponseEntity<CommonResult<List<Long>>> getFeaturedCourseIds() {
        try {
            // 管理端强一致：直查 DB 不走缓存
            List<Long> courseIds = courseService.getFeaturedCourseIdsNoCache();
            return ResponseEntity.ok(CommonResult.success("获取精选课程ID列表成功", courseIds));
        } catch (Exception e) {
            logger.error("获取精选课程ID列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 设置单个课程为精选课程
     */
    @Operation(summary = "设置精选课程", description = "设置单个课程为精选课程或取消精选")
    @PutMapping("/courses/{courseId}/featured")
    public ResponseEntity<CommonResult<Object>> setCourseAsFeatured(
            @PathVariable Long courseId,
            @RequestBody Map<String, Boolean> requestBody) {
        try {
            Boolean featured = requestBody.get("featured");
            if (featured == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("featured参数不能为空"));
            }

            courseService.setCourseAsFeatured(courseId, featured);
            String action = featured ? "设置为精选课程" : "取消精选课程";
            return ResponseEntity.ok(CommonResult.success(action + "成功", null));
        } catch (RuntimeException e) {
            logger.warn("设置精选课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("设置精选课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("操作失败"));
        }
    }

    /**
     * 批量设置精选课程
     */
    @Operation(summary = "批量设置精选课程", description = "批量设置多个课程为精选课程或取消精选")
    @PutMapping("/courses/batch-featured")
    public ResponseEntity<CommonResult<Object>> batchSetFeaturedCourses(
            @RequestBody Map<String, Object> requestBody) {
        try {
            @SuppressWarnings("unchecked")
            List<Long> courseIds = (List<Long>) requestBody.get("courseIds");
            Boolean featured = (Boolean) requestBody.get("featured");

            if (courseIds == null || courseIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("课程ID列表不能为空"));
            }

            if (featured == null) {
                return ResponseEntity.badRequest()
                        .body(CommonResult.error("featured参数不能为空"));
            }

            courseService.batchSetFeaturedCourses(courseIds, featured);
            String action = featured ? "批量设置为精选课程" : "批量取消精选课程";
            return ResponseEntity.ok(CommonResult.success(action + "成功", null));
        } catch (RuntimeException e) {
            logger.warn("批量设置精选课程失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("批量设置精选课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("操作失败"));
        }
    }

    /**
     * 获取余额交易记录列表
     */
    @Operation(summary = "获取余额交易记录列表", description = "分页查询所有学生的余额变动记录")
    @GetMapping("/balance-transactions")
    public ResponseEntity<CommonResult<Page<BalanceTransaction>>> getBalanceTransactions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String transactionType) {
        try {
            Page<BalanceTransaction> pageRequest = new Page<>(page, size);
            QueryWrapper<BalanceTransaction> queryWrapper = new QueryWrapper<>();
            
            // 按创建时间倒序排列
            queryWrapper.orderByDesc("created_at");
            
            // 按用户ID筛选
            if (userId != null) {
                queryWrapper.eq("user_id", userId);
            }
            
            // 按学生姓名筛选（模糊查询）
            if (name != null && !name.trim().isEmpty()) {
                queryWrapper.like("name", name.trim());
            }
            
            // 按交易类型筛选
            if (transactionType != null && !transactionType.trim().isEmpty()) {
                queryWrapper.eq("transaction_type", transactionType);
            }
            
            Page<BalanceTransaction> result = balanceTransactionMapper.selectPage(pageRequest, queryWrapper);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (Exception e) {
            logger.error("获取余额交易记录异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取教师课时变动记录列表
     */
    @Operation(summary = "获取教师课时明细", description = "分页查询教师课时变动记录")
    @GetMapping("/teacher-hour-details")
    public ResponseEntity<CommonResult<Page<HourDetail>>> getTeacherHourDetails(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer transactionType) {
        try {
            Page<HourDetail> pageRequest = new Page<>(page, size);
            QueryWrapper<HourDetail> qw = new QueryWrapper<>();
            qw.orderByDesc("created_at");
            if (userId != null) {
                qw.eq("user_id", userId);
            }
            if (name != null && !name.trim().isEmpty()) {
                qw.like("name", name.trim());
            }
            if (transactionType != null) {
                qw.eq("transaction_type", transactionType);
            }
            Page<HourDetail> result = hourDetailMapper.selectPage(pageRequest, qw);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (Exception e) {
            logger.error("获取教师课时明细异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    // ===================== 消息管理接口 =====================

    /**
     * 创建消息
     */
    @Operation(summary = "创建消息", description = "管理员创建新消息")
    @PostMapping("/messages")
    public ResponseEntity<CommonResult<Long>> createMessage(
            @Valid @RequestBody MessageCreateDTO createDTO,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Long messageId = messageService.createMessage(createDTO, userPrincipal.getId(), userPrincipal.getUsername());
            return ResponseEntity.ok(CommonResult.success("创建成功", messageId));
        } catch (RuntimeException e) {
            logger.warn("创建消息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("创建失败"));
        }
    }

    /**
     * 更新消息
     */
    @Operation(summary = "更新消息", description = "管理员更新消息信息")
    @PutMapping("/messages/{id}")
    public ResponseEntity<CommonResult<Boolean>> updateMessage(
            @PathVariable Long id,
            @Valid @RequestBody MessageUpdateDTO updateDTO) {
        try {
            updateDTO.setId(id);
            Boolean result = messageService.updateMessage(updateDTO);
            return ResponseEntity.ok(CommonResult.success("更新成功", result));
        } catch (RuntimeException e) {
            logger.warn("更新消息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    /**
     * 删除消息
     */
    @Operation(summary = "删除消息", description = "管理员删除消息")
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<CommonResult<Boolean>> deleteMessage(@PathVariable Long id) {
        try {
            Boolean result = messageService.deleteMessage(id);
            return ResponseEntity.ok(CommonResult.success("删除成功", result));
        } catch (RuntimeException e) {
            logger.warn("删除消息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("删除失败"));
        }
    }

    /**
     * 根据ID获取消息详情
     */
    @Operation(summary = "获取消息详情", description = "根据ID获取消息详细信息")
    @GetMapping("/messages/{id}")
    public ResponseEntity<CommonResult<MessageVO>> getMessageById(@PathVariable Long id) {
        try {
            MessageVO message = messageService.getMessageById(id);
            return ResponseEntity.ok(CommonResult.success("获取成功", message));
        } catch (RuntimeException e) {
            logger.warn("获取消息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 分页查询消息列表
     */
    @Operation(summary = "分页查询消息列表", description = "管理员分页查询消息列表，支持多条件筛选")
    @GetMapping("/messages")
    public ResponseEntity<CommonResult<IPage<MessageVO>>> getMessagePage(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String adminName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            MessageQueryDTO queryDTO = new MessageQueryDTO();
            queryDTO.setTitle(title);
            if (targetType != null && !targetType.isEmpty()) {
                queryDTO.setTargetType(Message.TargetType.valueOf(targetType));
            }
            queryDTO.setAdminName(adminName);
            queryDTO.setIsActive(isActive);
            queryDTO.setPageNum(pageNum);
            queryDTO.setPageSize(pageSize);

            IPage<MessageVO> result = messageService.getMessagePage(queryDTO);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (IllegalArgumentException e) {
            logger.warn("查询参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error("查询参数错误"));
        } catch (Exception e) {
            logger.error("获取消息列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 切换消息激活状态
     */
    @Operation(summary = "切换消息状态", description = "管理员切换消息的激活/停用状态")
    @PutMapping("/messages/{id}/toggle-status")
    public ResponseEntity<CommonResult<Boolean>> toggleMessageStatus(@PathVariable Long id) {
        try {
            Boolean result = messageService.toggleMessageStatus(id);
            return ResponseEntity.ok(CommonResult.success("状态切换成功", result));
        } catch (RuntimeException e) {
            logger.warn("切换消息状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("切换消息状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("操作失败"));
        }
    }

    /**
     * 手动触发过期课程状态清理
     */
    @Operation(summary = "手动清理过期课程", description = "手动触发过期课程状态清理任务，将过期的进行中课程状态更新为已完成")
    @PostMapping("/schedules/cleanup-expired")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CommonResult<String>> manualCleanupExpiredSchedules() {
        try {
            logger.info("管理员手动触发过期课程状态清理任务");
            int updatedCount = scheduleCleanupService.manualCleanupExpiredSchedules();
            String message = String.format("清理任务执行完成，共更新了%d个过期课程状态", updatedCount);
            return ResponseEntity.ok(CommonResult.success(message));
        } catch (Exception e) {
            logger.error("手动清理过期课程异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("清理任务执行失败：" + e.getMessage()));
        }
    }
}