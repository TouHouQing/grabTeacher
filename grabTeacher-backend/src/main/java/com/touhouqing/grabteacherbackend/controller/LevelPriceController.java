package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.LevelPrice;
import com.touhouqing.grabteacherbackend.service.LevelPriceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/admin/level-prices")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "级别价格管理", description = "教师级别价格管理（仅管理员）")
@SecurityRequirement(name = "Bearer Authentication")
public class LevelPriceController {

    @Autowired
    private LevelPriceService levelPriceService;

    @Operation(summary = "列表", description = "获取全部级别与价格")
    @GetMapping
    public ResponseEntity<CommonResult<List<LevelPrice>>> listAll() {
        List<LevelPrice> list = levelPriceService.listAll();
        return ResponseEntity.ok(CommonResult.success("获取成功", list));
    }

    public static class UpdatePriceRequest {
        @NotNull(message = "价格不能为空")
        @Min(value = 0, message = "价格不能为负")
        public BigDecimal price;
    }

    @Operation(summary = "更新价格", description = "仅允许修改价格")
    @PutMapping("/{id}/price")
    public ResponseEntity<CommonResult<LevelPrice>> updatePrice(@PathVariable Long id, @RequestBody UpdatePriceRequest req) {
        try {
            LevelPrice updated = levelPriceService.updatePrice(id, req.price);
            return ResponseEntity.ok(CommonResult.success("更新成功", updated));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(CommonResult.error("服务器异常"));
        }
    }
}

