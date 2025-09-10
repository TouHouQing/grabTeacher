package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.TeacherLevel;
import com.touhouqing.grabteacherbackend.model.vo.TeacherLevelVO;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.service.TeacherLevelService;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin/teacher-levels")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "教师级别管理", description = "教师级别管理（仅管理员）")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherLevelController {

    @Autowired
    private TeacherLevelService teacherLevelService;

    @Autowired
    private TeacherMapper teacherMapper;

    @Operation(summary = "列表", description = "获取全部教师级别")
    @GetMapping
    public ResponseEntity<CommonResult<List<TeacherLevelVO>>> listAll() {
        List<TeacherLevel> list = teacherLevelService.listAll();
        // 统计每个级别的使用数量
        List<TeacherLevelVO> result = new java.util.ArrayList<>(list.size());
        for (TeacherLevel lvl : list) {
            Long cnt = teacherMapper.selectCount(new QueryWrapper<Teacher>()
                    .eq("level", lvl.getName())
                    .eq("is_deleted", false));
            TeacherLevelVO vo = new TeacherLevelVO();
            vo.setId(lvl.getId());
            vo.setName(lvl.getName());
            vo.setIsActive(lvl.getIsActive());
            vo.setSortOrder(lvl.getSortOrder());
            vo.setCreateTime(lvl.getCreateTime());
            vo.setUpdateTime(lvl.getUpdateTime());
            vo.setUsageCount(cnt);
            result.add(vo);
        }
        return ResponseEntity.ok(CommonResult.success("获取成功", result));
    }

    @Operation(summary = "新增", description = "创建新的教师级别")
    @PostMapping
    public ResponseEntity<CommonResult<TeacherLevel>> create(@RequestBody TeacherLevel req) {
        if (req == null || !StringUtils.hasText(req.getName())) {
            return ResponseEntity.badRequest().body(CommonResult.error("级别名称不能为空"));
        }
        TeacherLevel entity = new TeacherLevel();
        entity.setName(req.getName().trim());
        entity.setIsActive(req.getIsActive() != null ? req.getIsActive() : Boolean.TRUE);
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        boolean ok = teacherLevelService.save(entity);
        if (!ok) {
            return ResponseEntity.internalServerError().body(CommonResult.error("创建失败"));
        }
        return ResponseEntity.ok(CommonResult.success("创建成功", entity));
    }

    @Operation(summary = "修改", description = "根据ID修改名称")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResult<TeacherLevel>> update(@PathVariable Long id, @RequestBody TeacherLevel req) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(CommonResult.error("ID不合法"));
        }
        TeacherLevel entity = teacherLevelService.getById(id);
        if (entity == null) {
            return ResponseEntity.badRequest().body(CommonResult.error("级别不存在"));
        }
        if (req != null) {
            if (StringUtils.hasText(req.getName())) {
                entity.setName(req.getName().trim());
            }
            if (req.getIsActive() != null) {
                entity.setIsActive(req.getIsActive());
            }
            if (req.getSortOrder() != null) {
                entity.setSortOrder(req.getSortOrder());
            }
        }
        entity.setUpdateTime(LocalDateTime.now());
        boolean ok = teacherLevelService.updateById(entity);
        if (!ok) {
            return ResponseEntity.internalServerError().body(CommonResult.error("更新失败"));
        }
        return ResponseEntity.ok(CommonResult.success("更新成功", entity));
    }

    @Operation(summary = "删除", description = "根据ID删除级别")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResult<Boolean>> delete(@PathVariable Long id) {
        if (id == null || id <= 0) {
            return ResponseEntity.badRequest().body(CommonResult.error("ID不合法"));
        }
        TeacherLevel level = teacherLevelService.getById(id);
        if (level == null) {
            return ResponseEntity.badRequest().body(CommonResult.error("级别不存在"));
        }
        // 校验是否被教师引用
        Long inUseCount = teacherMapper.selectCount(new QueryWrapper<Teacher>()
                .eq("level", level.getName())
                .eq("is_deleted", false));
        if (inUseCount != null && inUseCount > 0) {
            return ResponseEntity.badRequest().body(CommonResult.error("该级别已被" + inUseCount + "位教师使用，无法删除"));
        }
        boolean ok = teacherLevelService.removeById(id);
        if (!ok) {
            return ResponseEntity.internalServerError().body(CommonResult.error("删除失败"));
        }
        return ResponseEntity.ok(CommonResult.success("删除成功", true));
    }
}


