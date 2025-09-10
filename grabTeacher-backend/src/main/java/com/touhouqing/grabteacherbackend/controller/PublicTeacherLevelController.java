package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.TeacherLevel;
import com.touhouqing.grabteacherbackend.service.TeacherLevelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/teacher-levels")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "公开教师级别接口", description = "供所有用户访问的教师级别相关接口")
public class PublicTeacherLevelController {

    @Autowired
    private TeacherLevelService teacherLevelService;

    /**
     * 获取所有教师级别列表（公开接口）
     */
    @Operation(summary = "获取教师级别列表", description = "获取所有可用的教师级别列表")
    @GetMapping
    public ResponseEntity<CommonResult<List<TeacherLevel>>> getTeacherLevels() {
        try {
            List<TeacherLevel> levels = teacherLevelService.listAll();
            return ResponseEntity.ok(CommonResult.success("获取成功", levels));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CommonResult.error("获取教师级别列表失败"));
        }
    }
}
