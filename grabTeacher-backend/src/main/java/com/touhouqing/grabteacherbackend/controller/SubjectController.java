package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.ApiResponse;
import com.touhouqing.grabteacherbackend.dto.SubjectRequest;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/admin/subjects")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "科目管理", description = "科目管理相关接口（仅管理员可访问）")
@SecurityRequirement(name = "Bearer Authentication")
public class SubjectController {

    private static final Logger logger = LoggerFactory.getLogger(SubjectController.class);

    @Autowired
    private SubjectService subjectService;

    /**
     * 创建科目
     */
    @Operation(summary = "创建科目", description = "创建新的科目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "创建成功",
                    content = @Content(schema = @Schema(implementation = Subject.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "未授权"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "权限不足")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<Subject>> createSubject(@Valid @RequestBody SubjectRequest request) {
        try {
            Subject subject = subjectService.createSubject(request);
            return ResponseEntity.ok(ApiResponse.success("创建科目成功", subject));
        } catch (RuntimeException e) {
            logger.warn("创建科目失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("创建科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建失败，请稍后重试"));
        }
    }

    /**
     * 更新科目
     */
    @Operation(summary = "更新科目", description = "根据ID更新科目信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> updateSubject(
            @Parameter(description = "科目ID", required = true) @PathVariable Long id, 
            @Valid @RequestBody SubjectRequest request) {
        try {
            Subject subject = subjectService.updateSubject(id, request);
            return ResponseEntity.ok(ApiResponse.success("更新科目成功", subject));
        } catch (RuntimeException e) {
            logger.warn("更新科目失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败，请稍后重试"));
        }
    }

    /**
     * 删除科目
     */
    @Operation(summary = "删除科目", description = "根据ID删除科目（软删除）")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "删除成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSubject(@Parameter(description = "科目ID", required = true) @PathVariable Long id) {
        try {
            subjectService.deleteSubject(id);
            return ResponseEntity.ok(ApiResponse.success("删除科目成功", null));
        } catch (RuntimeException e) {
            logger.warn("删除科目失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("删除科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除失败，请稍后重试"));
        }
    }

    /**
     * 根据ID获取科目
     */
    @Operation(summary = "获取单个科目", description = "根据ID获取科目详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Subject>> getSubjectById(@Parameter(description = "科目ID", required = true) @PathVariable Long id) {
        try {
            Subject subject = subjectService.getSubjectById(id);
            if (subject == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("科目不存在"));
            }
            return ResponseEntity.ok(ApiResponse.success("获取科目成功", subject));
        } catch (Exception e) {
            logger.error("获取科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取科目列表（分页）
     */
    @Operation(summary = "获取科目列表", description = "分页获取科目列表，支持关键词搜索和状态筛选")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSubjectList(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "是否激活") @RequestParam(required = false) Boolean isActive) {
        try {
            Page<Subject> subjectPage = subjectService.getSubjectList(page, size, keyword, isActive);
            
            Map<String, Object> response = new HashMap<>();
            response.put("subjects", subjectPage.getRecords());
            response.put("total", subjectPage.getTotal());
            response.put("current", subjectPage.getCurrent());
            response.put("size", subjectPage.getSize());
            response.put("pages", subjectPage.getPages());
            
            return ResponseEntity.ok(ApiResponse.success("获取科目列表成功", response));
        } catch (Exception e) {
            logger.error("获取科目列表异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 获取所有激活的科目
     */
    @Operation(summary = "获取所有激活科目", description = "获取所有状态为激活的科目列表")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<Subject>>> getAllActiveSubjects() {
        try {
            List<Subject> subjects = subjectService.getAllActiveSubjects();
            return ResponseEntity.ok(ApiResponse.success("获取激活科目成功", subjects));
        } catch (Exception e) {
            logger.error("获取激活科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 更新科目状态
     */
    @Operation(summary = "更新科目状态", description = "启用或禁用科目")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "更新成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "请求参数错误"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Void>> updateSubjectStatus(
            @Parameter(description = "科目ID", required = true) @PathVariable Long id, 
            @Parameter(description = "是否激活", required = true) @RequestParam Boolean isActive) {
        try {
            subjectService.updateSubjectStatus(id, isActive);
            return ResponseEntity.ok(ApiResponse.success("更新科目状态成功", null));
        } catch (RuntimeException e) {
            logger.warn("更新科目状态失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新科目状态异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新失败，请稍后重试"));
        }
    }
} 