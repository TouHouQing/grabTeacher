package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("level_price")
public class LevelPrice {
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "级别名称", example = "王牌")
    @TableField("name")
    private String name;

    @Schema(description = "该级别价格（每小时多少钱）")
    @TableField("price")
    private BigDecimal price;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;
}

