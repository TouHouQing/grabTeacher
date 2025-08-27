package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.CourseEvaluation;
import com.touhouqing.grabteacherbackend.model.vo.CourseEvaluationVO;
import com.touhouqing.grabteacherbackend.service.CourseEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/course-evaluations")
@Tag(name = "管理员-课程评价", description = "管理员增删改查课程评价")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCourseEvaluationController {

    @Autowired
    private CourseEvaluationService courseEvaluationService;

    @Operation(summary = "分页列表")
    @GetMapping
    public ResponseEntity<CommonResult<Map<String, Object>>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(required = false) String teacherName,
            @RequestParam(required = false) String studentName,
            @RequestParam(required = false) String courseName
    ) {
        Page<CourseEvaluationVO> voPage = courseEvaluationService.pageAdmin(page, size, teacherId, studentId, courseId, minRating, teacherName, studentName, courseName);
        Map<String, Object> data = new HashMap<>();
        data.put("records", voPage.getRecords());
        data.put("total", voPage.getTotal());
        data.put("current", voPage.getCurrent());
        data.put("size", voPage.getSize());
        data.put("pages", voPage.getPages());
        return ResponseEntity.ok(CommonResult.success("获取成功", data));
    }

    @Operation(summary = "创建")
    @PostMapping
    public ResponseEntity<CommonResult<CourseEvaluation>> create(@Valid @RequestBody CourseEvaluationCreateDTO dto) {
        CourseEvaluation created = courseEvaluationService.createByAdmin(dto);
        return ResponseEntity.ok(CommonResult.success("创建成功", created));
    }

    @Operation(summary = "更新")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResult<CourseEvaluation>> update(@PathVariable Long id, @Valid @RequestBody CourseEvaluationUpdateDTO dto) {
        dto.setId(id);
        CourseEvaluation updated = courseEvaluationService.updateByAdmin(dto);
        return ResponseEntity.ok(CommonResult.success("更新成功", updated));
    }

    @Operation(summary = "删除")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResult<Void>> delete(@PathVariable Long id) {
        courseEvaluationService.deleteByAdmin(id);
        return ResponseEntity.ok(CommonResult.success("删除成功", null));
    }

    @Operation(summary = "切换精选状态")
    @PatchMapping("/{id}/featured")
    public ResponseEntity<CommonResult<CourseEvaluation>> toggleFeatured(@PathVariable Long id, @RequestParam boolean isFeatured) {
        CourseEvaluation updated = courseEvaluationService.toggleFeatured(id, isFeatured);
        return ResponseEntity.ok(CommonResult.success("操作成功", updated));
    }
}


