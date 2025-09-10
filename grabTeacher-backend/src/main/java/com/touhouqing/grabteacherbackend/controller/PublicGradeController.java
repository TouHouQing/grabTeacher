package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.Grade;
import com.touhouqing.grabteacherbackend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/grades")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "公开年级接口", description = "供学生端获取可用年级列表")
@RequiredArgsConstructor
public class PublicGradeController {

    private final GradeService gradeService;

    @Operation(summary = "获取启用的年级列表", description = "返回启用的年级，按sortOrder升序")
    @GetMapping
    public ResponseEntity<CommonResult<List<Grade>>> listActiveGrades() {
        try {
            Page<Grade> p = gradeService.list(1, 1000, null, Boolean.TRUE);
            List<Grade> active = p.getRecords();
            return ResponseEntity.ok(CommonResult.success("获取成功", active));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(CommonResult.error("获取年级列表失败"));
        }
    }
}

