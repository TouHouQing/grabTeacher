package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.ApiResponseDTO;
import com.touhouqing.grabteacherbackend.dto.StudyAbroadCountryRequestDTO;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadCountry;
import com.touhouqing.grabteacherbackend.service.StudyAbroadCountryService;
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
@Tag(name = "留学国家管理")
public class StudyAbroadCountryController {

    private final StudyAbroadCountryService countryService;

    // 管理端
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/study-abroad/countries")
    @Operation(summary = "创建国家")
    public ResponseEntity<ApiResponseDTO<StudyAbroadCountry>> create(@Valid @RequestBody StudyAbroadCountryRequestDTO request) {
        try {
            return ResponseEntity.ok(ApiResponseDTO.success("创建成功", countryService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("创建失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/study-abroad/countries/{id}")
    @Operation(summary = "更新国家")
    public ResponseEntity<ApiResponseDTO<StudyAbroadCountry>> update(@PathVariable Long id, @Valid @RequestBody StudyAbroadCountryRequestDTO request) {
        try {
            return ResponseEntity.ok(ApiResponseDTO.success("更新成功", countryService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/study-abroad/countries/{id}")
    @Operation(summary = "删除国家")
    public ResponseEntity<ApiResponseDTO<Void>> delete(@PathVariable Long id) {
        try {
            countryService.delete(id);
            return ResponseEntity.ok(ApiResponseDTO.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("删除失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/countries/{id}/status")
    @Operation(summary = "更新国家状态")
    public ResponseEntity<ApiResponseDTO<Void>> updateStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            countryService.updateStatus(id, isActive);
            return ResponseEntity.ok(ApiResponseDTO.success("更新状态成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponseDTO.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/study-abroad/countries")
    @Operation(summary = "国家分页列表")
    public ResponseEntity<ApiResponseDTO<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive
    ) {
        try {
            Page<StudyAbroadCountry> p = countryService.list(page, size, keyword, isActive);
            Map<String, Object> data = new HashMap<>();
            data.put("records", p.getRecords());
            data.put("total", p.getTotal());
            data.put("current", p.getCurrent());
            data.put("size", p.getSize());
            data.put("pages", p.getPages());
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("获取失败"));
        }
    }

    // 公开端
    @GetMapping("/public/study-abroad/countries")
    @Operation(summary = "获取所有启用国家（公开）")
    public ResponseEntity<ApiResponseDTO<List<StudyAbroadCountry>>> listActive() {
        try {
            return ResponseEntity.ok(ApiResponseDTO.success("获取成功", countryService.listActive()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponseDTO.error("获取失败"));
        }
    }
}

