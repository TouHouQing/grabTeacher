package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.entity.dto.StudyAbroadProgramRequest;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadProgram;
import com.touhouqing.grabteacherbackend.service.StudyAbroadProgramService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "留学项目管理")
public class StudyAbroadProgramController {

    private final StudyAbroadProgramService programService;

    // 管理端
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/study-abroad/programs")
    @Operation(summary = "创建项目")
    public ResponseEntity<ApiResponse<StudyAbroadProgram>> create(@Valid @RequestBody StudyAbroadProgramRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("创建成功", programService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("创建失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/study-abroad/programs/{id}")
    @Operation(summary = "更新项目")
    public ResponseEntity<ApiResponse<StudyAbroadProgram>> update(@PathVariable Long id, @Valid @RequestBody StudyAbroadProgramRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新成功", programService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/study-abroad/programs/{id}")
    @Operation(summary = "删除项目")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            programService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("删除失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/programs/{id}/status")
    @Operation(summary = "更新项目状态")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            programService.updateStatus(id, isActive);
            return ResponseEntity.ok(ApiResponse.success("更新状态成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/programs/{id}/flags")
    @Operation(summary = "更新项目热门标记")
    public ResponseEntity<ApiResponse<Void>> updateFlags(@PathVariable Long id,
                                                         @RequestParam(required = false) Boolean isHot) {
        try {
            programService.updateFlags(id, isHot, null);
            return ResponseEntity.ok(ApiResponse.success("更新标记成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/study-abroad/programs")
    @Operation(summary = "项目分页列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Boolean isHot
    ) {
        try {
            Page<StudyAbroadProgram> p = programService.list(page, size, keyword, isActive, countryId, stageId, isHot, null);
            Map<String, Object> data = new HashMap<>();
            data.put("records", p.getRecords());
            data.put("total", p.getTotal());
            data.put("current", p.getCurrent());
            data.put("size", p.getSize());
            data.put("pages", p.getPages());
            return ResponseEntity.ok(ApiResponse.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("获取失败"));
        }
    }

    // 公开端
    @GetMapping("/public/study-abroad/programs")
    @Operation(summary = "获取启用的项目（可按国家/阶段过滤，公开）")
    public ResponseEntity<ApiResponse<List<StudyAbroadProgram>>> listActive(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stageId
    ) {
        try {
            return ResponseEntity.ok(ApiResponse.success("获取成功", programService.listActive(limit, countryId, stageId)));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("获取失败"));
        }
    }
}

