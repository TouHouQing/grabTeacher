package com.touhouqing.grabteacherbackend.model.dto;

import com.touhouqing.grabteacherbackend.model.entity.Message;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 消息创建DTO
 */
@Data
public class MessageCreateDTO {
    
    /**
     * 消息标题
     */
    @NotBlank(message = "消息标题不能为空")
    @Size(max = 255, message = "消息标题长度不能超过255个字符")
    private String title;
    
    /**
     * 消息内容
     */
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容长度不能超过5000个字符")
    private String content;
    
    /**
     * 目标用户类型
     */
    @NotNull(message = "目标用户类型不能为空")
    private Message.TargetType targetType;
}
