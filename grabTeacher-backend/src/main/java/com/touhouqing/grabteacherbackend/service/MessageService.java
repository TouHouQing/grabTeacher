package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.touhouqing.grabteacherbackend.model.dto.MessageCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageQueryDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageUpdateDTO;
import com.touhouqing.grabteacherbackend.model.vo.MessageVO;

/**
 * 消息服务接口
 */
public interface MessageService {
    
    /**
     * 创建消息
     * 
     * @param createDTO 消息创建DTO
     * @param adminId 管理员ID
     * @param adminName 管理员姓名
     * @return 消息ID
     */
    Long createMessage(MessageCreateDTO createDTO, Long adminId, String adminName);
    
    /**
     * 更新消息
     * 
     * @param updateDTO 消息更新DTO
     * @return 是否成功
     */
    Boolean updateMessage(MessageUpdateDTO updateDTO);
    
    /**
     * 删除消息
     * 
     * @param id 消息ID
     * @return 是否成功
     */
    Boolean deleteMessage(Long id);
    
    /**
     * 根据ID查询消息
     * 
     * @param id 消息ID
     * @return 消息VO
     */
    MessageVO getMessageById(Long id);
    
    /**
     * 分页查询消息列表（管理员用）
     * 
     * @param queryDTO 查询条件
     * @return 消息分页列表
     */
    IPage<MessageVO> getMessagePage(MessageQueryDTO queryDTO);
    
    /**
     * 查询学生可见的消息列表
     * 
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 消息列表
     */
    IPage<MessageVO> getStudentMessages(Integer pageNum, Integer pageSize);
    
    /**
     * 查询教师可见的消息列表
     * 
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 消息列表
     */
    IPage<MessageVO> getTeacherMessages(Integer pageNum, Integer pageSize);
    
    /**
     * 切换消息激活状态
     * 
     * @param id 消息ID
     * @return 是否成功
     */
    Boolean toggleMessageStatus(Long id);
}
