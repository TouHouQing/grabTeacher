package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "按日历设置教师可上课时间 请求DTO")
public class DailyAvailabilitySetDTO {
    @NotNull(message = "教师ID不能为空")
    private Long teacherId;

    @Valid
    @Schema(description = "若某日未在列表中，表示该日默认不可上课（或按周模板回退，视后端策略）")
    private List<DailyTimeSlotDTO> items;

    @Schema(description = "是否覆盖同日期已有设置（true=覆盖重建，false=逐条合并覆盖）", example = "true")
    @Builder.Default
    private Boolean overwrite = true;
}

