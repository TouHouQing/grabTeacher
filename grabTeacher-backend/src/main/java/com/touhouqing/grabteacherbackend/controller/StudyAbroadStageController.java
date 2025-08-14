package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.entity.dto.StudyAbroadStageRequest;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadStage;
import com.touhouqing.grabteacherbackend.service.StudyAbroadStageService;
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
@Tag(name = "留学阶段管理")
public class StudyAbroadStageController {

    private final StudyAbroadStageService stageService;

    // 管理端
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/study-abroad/stages")
    @Operation(summary = "创建阶段")
    public ResponseEntity<ApiResponse<StudyAbroadStage>> create(@Valid @RequestBody StudyAbroadStageRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("创建成功", stageService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("创建失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/study-abroad/stages/{id}")
    @Operation(summary = "更新阶段")
    public ResponseEntity<ApiResponse<StudyAbroadStage>> update(@PathVariable Long id, @Valid @RequestBody StudyAbroadStageRequest request) {
        try {
            return ResponseEntity.ok(ApiResponse.success("更新成功", stageService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/study-abroad/stages/{id}")
    @Operation(summary = "删除阶段")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        try {
            stageService.delete(id);
            return ResponseEntity.ok(ApiResponse.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("删除失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/stages/{id}/status")
    @Operation(summary = "更新阶段状态")
    public ResponseEntity<ApiResponse<Void>> updateStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            stageService.updateStatus(id, isActive);
            return ResponseEntity.ok(ApiResponse.success("更新状态成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/study-abroad/stages")
    @Operation(summary = "阶段分页列表")
    public ResponseEntity<ApiResponse<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        try {
            Page<StudyAbroadStage> p = stageService.list(page, size, keyword, isActive);
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
    @GetMapping("/public/study-abroad/stages")
    @Operation(summary = "获取所有启用阶段（公开）")
    public ResponseEntity<ApiResponse<List<StudyAbroadStage>>> listActive() {
        try {
            return ResponseEntity.ok(ApiResponse.success("获取成功", stageService.listActive()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error("获取失败"));
        }
    }
}

