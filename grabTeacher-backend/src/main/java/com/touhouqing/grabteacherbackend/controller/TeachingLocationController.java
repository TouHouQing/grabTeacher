package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.TeachingLocationDTO;
import com.touhouqing.grabteacherbackend.model.entity.TeachingLocation;
import com.touhouqing.grabteacherbackend.service.TeachingLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/teaching-locations")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "授课地点管理", description = "授课地点管理相关接口（仅管理员可访问）")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class TeachingLocationController {

    private final TeachingLocationService teachingLocationService;

    @Operation(summary = "创建授课地点", description = "创建新的授课地点")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = TeachingLocation.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    public ResponseEntity<CommonResult<TeachingLocation>> create(@Valid @RequestBody TeachingLocationDTO request) {
        try {
            TeachingLocation entity = teachingLocationService.create(request);
            return ResponseEntity.ok(CommonResult.success("创建授课地点成功", entity));
        } catch (RuntimeException e) {
            log.warn("创建授课地点失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("创建授课地点异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("创建失败，请稍后重试"));
        }
    }

    @Operation(summary = "更新授课地点", description = "根据ID更新授课地点信息")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResult<TeachingLocation>> update(
            @Parameter(description = "授课地点ID", required = true) @PathVariable Long id,
            @Valid @RequestBody TeachingLocationDTO request) {
        try {
            TeachingLocation entity = teachingLocationService.update(id, request);
            return ResponseEntity.ok(CommonResult.success("更新授课地点成功", entity));
        } catch (RuntimeException e) {
            log.warn("更新授课地点失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("更新授课地点异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败，请稍后重试"));
        }
    }

    @Operation(summary = "删除授课地点", description = "根据ID删除授课地点（硬删除）")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResult<Void>> delete(@Parameter(description = "授课地点ID", required = true) @PathVariable Long id) {
        try {
            teachingLocationService.delete(id);
            return ResponseEntity.ok(CommonResult.success("删除授课地点成功", null));
        } catch (RuntimeException e) {
            log.warn("删除授课地点失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("删除授课地点异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("删除失败，请稍后重试"));
        }
    }

    @Operation(summary = "获取单个授课地点", description = "根据ID获取授课地点详细信息")
    @GetMapping("/{id}")
    public ResponseEntity<CommonResult<TeachingLocation>> getById(@PathVariable Long id) {
        try {
            TeachingLocation entity = teachingLocationService.getById(id);
            if (entity == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(CommonResult.error("授课地点不存在"));
            }
            return ResponseEntity.ok(CommonResult.success("获取授课地点成功", entity));
        } catch (Exception e) {
            log.error("获取授课地点异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    @Operation(summary = "获取授课地点列表", description = "分页获取授课地点列表，支持关键词搜索和状态筛选")
    @GetMapping
    public ResponseEntity<CommonResult<Map<String, Object>>> list(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词（匹配名称/地址）") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否激活") @RequestParam(required = false) Boolean isActive) {
        try {
            Page<TeachingLocation> p = teachingLocationService.list(page, size, keyword, isActive);
            Map<String, Object> resp = new HashMap<>();
            resp.put("locations", p.getRecords());
            resp.put("total", p.getTotal());
            resp.put("current", p.getCurrent());
            resp.put("size", p.getSize());
            resp.put("pages", p.getPages());
            return ResponseEntity.ok(CommonResult.success("获取授课地点列表成功", resp));
        } catch (Exception e) {
            log.error("获取授课地点列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败，请稍后重试"));
        }
    }

    @Operation(summary = "更新授课地点状态", description = "启用或禁用授课地点")
    @PatchMapping("/{id}/status")
    public ResponseEntity<CommonResult<Void>> updateStatus(
            @Parameter(description = "授课地点ID", required = true) @PathVariable Long id,
            @Parameter(description = "是否激活", required = true) @RequestParam Boolean isActive) {
        try {
            teachingLocationService.updateStatus(id, isActive);
            return ResponseEntity.ok(CommonResult.success("更新授课地点状态成功", null));
        } catch (RuntimeException e) {
            log.warn("更新授课地点状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            log.error("更新授课地点状态异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败，请稍后重试"));
        }
    }
}

