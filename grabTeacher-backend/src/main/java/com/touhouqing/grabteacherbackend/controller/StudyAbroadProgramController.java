package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.StudyAbroadProgramDTO;
import com.touhouqing.grabteacherbackend.model.entity.StudyAbroadProgram;
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
import org.springframework.http.MediaType;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.touhouqing.grabteacherbackend.cache.AbroadProgramsLocalCache;
import com.touhouqing.grabteacherbackend.config.PublicJsonCacheConfig;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "留学项目管理")
public class StudyAbroadProgramController {

    private final StudyAbroadProgramService programService;

    // L1/L2 JSON 缓存依赖（公开端）
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final AbroadProgramsLocalCache abroadProgramsLocalCache;
    private final PublicJsonCacheConfig publicJsonCacheConfig;

    // 单飞锁，避免并发回源击穿
    private final java.util.concurrent.ConcurrentHashMap<String, Object> keyLocks = new java.util.concurrent.ConcurrentHashMap<>();

    // 管理端
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PostMapping("/admin/study-abroad/programs")
    @Operation(summary = "创建项目")
    public ResponseEntity<CommonResult<StudyAbroadProgram>> create(@Valid @RequestBody StudyAbroadProgramDTO request) {
        try {
            return ResponseEntity.ok(CommonResult.success("创建成功", programService.create(request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("创建失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PutMapping("/admin/study-abroad/programs/{id}")
    @Operation(summary = "更新项目")
    public ResponseEntity<CommonResult<StudyAbroadProgram>> update(@PathVariable Long id, @Valid @RequestBody StudyAbroadProgramDTO request) {
        try {
            return ResponseEntity.ok(CommonResult.success("更新成功", programService.update(id, request)));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @DeleteMapping("/admin/study-abroad/programs/{id}")
    @Operation(summary = "删除项目")
    public ResponseEntity<CommonResult<Void>> delete(@PathVariable Long id) {
        try {
            programService.delete(id);
            return ResponseEntity.ok(CommonResult.success("删除成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("删除失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/programs/{id}/status")
    @Operation(summary = "更新项目状态")
    public ResponseEntity<CommonResult<Void>> updateStatus(@PathVariable Long id, @RequestParam Boolean isActive) {
        try {
            programService.updateStatus(id, isActive);
            return ResponseEntity.ok(CommonResult.success("更新状态成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @PatchMapping("/admin/study-abroad/programs/{id}/flags")
    @Operation(summary = "更新项目热门标记")
    public ResponseEntity<CommonResult<Void>> updateFlags(@PathVariable Long id,
                                                          @RequestParam(required = false) Boolean isHot) {
        try {
            programService.updateFlags(id, isHot, null);
            return ResponseEntity.ok(CommonResult.success("更新标记成功", null));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("更新失败"));
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @GetMapping("/admin/study-abroad/programs")
    @Operation(summary = "项目分页列表")
    public ResponseEntity<CommonResult<Map<String, Object>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Boolean isHot
    ) {
        try {
            Page<StudyAbroadProgram> p = programService.listNoCache(page, size, keyword, isActive, countryId, stageId, isHot, null);
            Map<String, Object> data = new HashMap<>();
            data.put("records", p.getRecords());
            data.put("total", p.getTotal());
            data.put("current", p.getCurrent());
            data.put("size", p.getSize());
            data.put("pages", p.getPages());
            return ResponseEntity.ok(CommonResult.success("获取成功", data));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }

    // 公开端 - JSON L1+L2 缓存
    @GetMapping(value = "/public/study-abroad/programs", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取启用的项目（可按国家/阶段过滤，公开）")
    public ResponseEntity<?> listActive(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stageId
    ) {
        try {
            int lim = (limit == null || limit <= 0) ? 20 : limit; // 默认 20
            String cacheKey = buildProgramsCacheKey(lim, countryId, stageId);

            String json = abroadProgramsLocalCache.get(cacheKey);
            if (json != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }

            // 单飞锁，避免并发回源击穿
            Object lock = keyLocks.computeIfAbsent(cacheKey, k -> new Object());
            synchronized (lock) {
                // 双检 L1
                json = abroadProgramsLocalCache.get(cacheKey);
                if (json != null) {
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    abroadProgramsLocalCache.put(cacheKey, json);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                List<StudyAbroadProgram> data = programService.listActive(lim, countryId, stageId);
                json = objectMapper.writeValueAsString(CommonResult.success("获取成功", data));
                abroadProgramsLocalCache.put(cacheKey, json);
                stringRedisTemplate.opsForValue().set(cacheKey, json, publicJsonCacheConfig.getProgramsActiveTtl());

                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }

    // 公开端 - 分页 JSON L1+L2 缓存（与管理端风格一致）
    @GetMapping(value = "/public/study-abroad/programs/page", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "获取启用的项目（公开分页接口）")
    public ResponseEntity<?> listActivePaged(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long countryId,
            @RequestParam(required = false) Long stageId
    ) {
        try {
            String kCountry = countryId != null ? countryId.toString() : "all";
            String kStage = stageId != null ? stageId.toString() : "all";
            String cacheKey = String.format("public:abroad:programs:active:paged:json:page:%d:size:%d:country:%s:stage:%s", page, size, kCountry, kStage);

            String json = abroadProgramsLocalCache.get(cacheKey);
            if (json != null) {
                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }

            Object lock = keyLocks.computeIfAbsent(cacheKey, k -> new Object());
            synchronized (lock) {
                json = abroadProgramsLocalCache.get(cacheKey);
                if (json != null) {
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                json = stringRedisTemplate.opsForValue().get(cacheKey);
                if (json != null) {
                    abroadProgramsLocalCache.put(cacheKey, json);
                    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
                }

                Page<StudyAbroadProgram> p = programService.listNoCache(page, size, null, true, countryId, stageId, null, null);
                Map<String, Object> data = new HashMap<>();
                data.put("records", p.getRecords());
                data.put("total", p.getTotal());
                data.put("current", p.getCurrent());
                data.put("size", p.getSize());
                data.put("pages", p.getPages());

                json = objectMapper.writeValueAsString(CommonResult.success("获取成功", data));
                abroadProgramsLocalCache.put(cacheKey, json);
                stringRedisTemplate.opsForValue().set(cacheKey, json, publicJsonCacheConfig.getProgramsActiveTtl());

                return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(json);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(CommonResult.error("获取失败"));
        }
    }


    private String buildProgramsCacheKey(int limit, Long countryId, Long stageId) {
        StringBuilder sb = new StringBuilder("public:abroad:programs:active:json");
        sb.append(":limit:").append(limit);
        if (countryId != null) sb.append(":country:").append(countryId);
        if (stageId != null) sb.append(":stage:").append(stageId);
        return sb.toString();
    }
}

