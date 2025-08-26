package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationCreateDTO;
import com.touhouqing.grabteacherbackend.model.entity.CourseEvaluation;
import com.touhouqing.grabteacherbackend.service.ICourseEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 课程评价表 前端控制器
 * </p>
 *
 * @author TouHouQing
 * @since 2025-08-26
 */
@RestController
@RequestMapping("/api/student/course-evaluations")
@Tag(name = "学生端-课程评价", description = "学生端创建课程评价")
@PreAuthorize("hasRole('STUDENT')")
public class CourseEvaluationController {

    @Autowired
    private ICourseEvaluationService courseEvaluationService;

    @Operation(summary = "创建课程评价")
    @PostMapping
    public ResponseEntity<CommonResult<CourseEvaluation>> create(@Valid @RequestBody CourseEvaluationCreateDTO dto) {
        CourseEvaluation created = courseEvaluationService.createByStudent(dto);
        return ResponseEntity.ok(CommonResult.success("评价创建成功", created));
    }

    @Operation(summary = "检查学生是否已评价某课程")
    @GetMapping("/check/{studentId}/{courseId}")
    public ResponseEntity<CommonResult<Boolean>> checkEvaluationStatus(
            @PathVariable Long studentId,
            @PathVariable Long courseId) {
        boolean hasEvaluated = courseEvaluationService.hasStudentEvaluatedCourse(studentId, courseId);
        return ResponseEntity.ok(CommonResult.success("查询成功", hasEvaluated));
    }
}
