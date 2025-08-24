package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeQueryDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.LessonGrade;
import com.touhouqing.grabteacherbackend.model.vo.LessonGradeVO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.LessonGradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/lesson-grades")
@RequiredArgsConstructor
@Tag(name = "课程成绩管理", description = "课程成绩相关接口")
public class LessonGradeController {

    private final LessonGradeService lessonGradeService;

    @PostMapping
    @Operation(summary = "录入课程成绩", description = "教师为学生录入课程成绩")
    public CommonResult<LessonGrade> createGrade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody LessonGradeCreateDTO request) {
        
        log.info("录入课程成绩请求: userId={}, scheduleId={}", 
                userPrincipal.getId(), request.getScheduleId());
        
        LessonGrade grade = lessonGradeService.createGrade(userPrincipal.getId(), request);
        return CommonResult.success(grade);
    }

    @PutMapping
    @Operation(summary = "更新课程成绩", description = "教师更新已录入的课程成绩")
    public CommonResult<LessonGrade> updateGrade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody LessonGradeUpdateDTO request) {
        
        log.info("更新课程成绩请求: userId={}, gradeId={}", 
                userPrincipal.getId(), request.getId());
        
        LessonGrade grade = lessonGradeService.updateGrade(userPrincipal.getId(), request);
        return CommonResult.success(grade);
    }

    @DeleteMapping("/{gradeId}")
    @Operation(summary = "删除课程成绩", description = "教师删除已录入的课程成绩")
    public CommonResult<Void> deleteGrade(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "成绩记录ID") @PathVariable Long gradeId) {
        
        log.info("删除课程成绩请求: userId={}, gradeId={}", userPrincipal.getId(), gradeId);
        
        lessonGradeService.deleteGrade(userPrincipal.getId(), gradeId);
        return CommonResult.success(null);
    }

    @GetMapping("/schedule/{scheduleId}/student/{studentId}")
    @Operation(summary = "查询特定课程的成绩", description = "根据课程安排ID和学生ID查询成绩")
    public CommonResult<LessonGrade> getGradeByScheduleAndStudent(
            @Parameter(description = "课程安排ID") @PathVariable Long scheduleId,
            @Parameter(description = "学生ID") @PathVariable Long studentId) {
        
        log.info("查询特定课程成绩: scheduleId={}, studentId={}", scheduleId, studentId);
        
        LessonGrade grade = lessonGradeService.getGradeByScheduleAndStudent(scheduleId, studentId);
        return CommonResult.success(grade);
    }

    @GetMapping("/teacher/my-grades")
    @Operation(summary = "查询教师录入的成绩", description = "分页查询当前教师录入的所有成绩")
    public CommonResult<IPage<LessonGradeVO>> getMyGrades(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid LessonGradeQueryDTO request) {
        
        log.info("查询教师成绩列表: userId={}", userPrincipal.getId());
        
        IPage<LessonGradeVO> grades = lessonGradeService.getGradesByTeacher(userPrincipal.getId(), request);
        return CommonResult.success(grades);
    }

    @GetMapping("/student/{studentId}")
    @Operation(summary = "查询学生成绩", description = "分页查询指定学生的成绩")
    public CommonResult<IPage<LessonGradeVO>> getStudentGrades(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Valid LessonGradeQueryDTO request) {
        
        log.info("查询学生成绩列表: studentId={}", studentId);
        
        request.setStudentId(studentId);
        IPage<LessonGradeVO> grades = lessonGradeService.getGradesByStudent(request);
        return CommonResult.success(grades);
    }

    @GetMapping("/student/{studentId}/subject/{subjectName}/chart-data")
    @Operation(summary = "获取学生某科目成绩图表数据", description = "用于ECharts图表展示")
    public CommonResult<List<LessonGradeVO>> getStudentSubjectChartData(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Parameter(description = "科目名称") @PathVariable String subjectName) {
        
        log.info("获取学生科目成绩图表数据: studentId={}, subjectName={}", studentId, subjectName);
        
        List<LessonGradeVO> grades = lessonGradeService.getGradesByStudentAndSubject(studentId, subjectName);
        return CommonResult.success(grades);
    }

    @GetMapping("/student/{studentId}/date-range")
    @Operation(summary = "获取学生指定时间范围的成绩", description = "查询学生在指定日期范围内的成绩")
    public CommonResult<List<LessonGradeVO>> getStudentGradesByDateRange(
            @Parameter(description = "学生ID") @PathVariable Long studentId,
            @Parameter(description = "开始日期 (yyyy-MM-dd)") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)") @RequestParam(required = false) String endDate) {
        
        log.info("获取学生时间范围成绩: studentId={}, startDate={}, endDate={}", 
                studentId, startDate, endDate);
        
        List<LessonGradeVO> grades = lessonGradeService.getGradesByStudentAndDateRange(studentId, startDate, endDate);
        return CommonResult.success(grades);
    }

    @GetMapping("/statistics")
    @Operation(summary = "获取成绩统计", description = "获取成绩相关统计信息")
    public CommonResult<Map<String, Object>> getStatistics(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "学生ID（可选）") @RequestParam(required = false) Long studentId) {
        
        log.info("获取成绩统计: userId={}, studentId={}", userPrincipal.getId(), studentId);
        
        Long count = lessonGradeService.getGradeCount(studentId, null);
        return CommonResult.success(Map.of("totalGrades", count));
    }

    @GetMapping("/check-permission/{scheduleId}")
    @Operation(summary = "检查录入权限", description = "检查当前教师是否有权限录入指定课程的成绩")
    public CommonResult<Map<String, Boolean>> checkGradePermission(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "课程安排ID") @PathVariable Long scheduleId) {
        
        log.info("检查录入权限: userId={}, scheduleId={}", userPrincipal.getId(), scheduleId);
        
        boolean hasPermission = lessonGradeService.hasPermissionToGrade(userPrincipal.getId(), scheduleId);
        return CommonResult.success(Map.of("hasPermission", hasPermission));
    }
}
