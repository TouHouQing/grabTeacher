package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponseDTO;
import com.touhouqing.grabteacherbackend.dto.GradeResponseDTO;
import com.touhouqing.grabteacherbackend.service.GradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公开年级接口控制器
 * 提供不需要认证的年级相关接口
 */
@Slf4j
@RestController
@RequestMapping("/api/public/grades")
@RequiredArgsConstructor
@Tag(name = "公开年级接口", description = "提供公开访问的年级相关接口")
public class PublicGradeController {

    private final GradeService gradeService;

    /**
     * 获取所有年级列表（公开接口）
     */
    @Operation(summary = "获取所有年级列表", description = "获取系统中所有年级信息，无需认证")
    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<GradeResponseDTO>>> getAllGrades() {
        try {
            List<GradeResponseDTO> grades = gradeService.getAllGrades();
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", grades));
        } catch (Exception e) {
            log.error("获取年级列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }

    /**
     * 获取年级名称列表（公开接口）
     */
    @Operation(summary = "获取年级名称列表", description = "获取所有年级的名称列表，用于下拉选择")
    @GetMapping("/names")
    public ResponseEntity<ApiResponseDTO<List<String>>> getGradeNames() {
        try {
            List<GradeResponseDTO> grades = gradeService.getAllGrades();
            List<String> gradeNames = grades.stream()
                    .map(GradeResponseDTO::getGradeName)
                    .toList();
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", gradeNames));
        } catch (Exception e) {
            log.error("获取年级名称列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败"));
        }
    }
}
