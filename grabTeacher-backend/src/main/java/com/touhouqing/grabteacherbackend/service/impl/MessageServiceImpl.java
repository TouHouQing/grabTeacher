package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.exception.BusinessException;
import com.touhouqing.grabteacherbackend.mapper.MessageMapper;
import com.touhouqing.grabteacherbackend.model.dto.MessageCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageQueryDTO;
import com.touhouqing.grabteacherbackend.model.dto.MessageUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.Message;
import com.touhouqing.grabteacherbackend.model.vo.MessageVO;
import com.touhouqing.grabteacherbackend.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 消息服务实现类
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {
    
    private final MessageMapper messageMapper;
    
    @Override
    @Transactional
    public Long createMessage(MessageCreateDTO createDTO, Long adminId, String adminName) {
        Message message = new Message();
        BeanUtils.copyProperties(createDTO, message);
        message.setAdminId(adminId);
        message.setAdminName(adminName);
        message.setIsActive(true);
        message.setCreatedAt(LocalDateTime.now());
        message.setUpdatedAt(LocalDateTime.now());
        
        int result = messageMapper.insert(message);
        if (result <= 0) {
            throw new BusinessException("创建消息失败");
        }
        
        return message.getId();
    }
    
    @Override
    @Transactional
    public Boolean updateMessage(MessageUpdateDTO updateDTO) {
        Message existingMessage = messageMapper.selectById(updateDTO.getId());
        if (existingMessage == null) {
            throw new BusinessException("消息不存在");
        }
        
        Message message = new Message();
        BeanUtils.copyProperties(updateDTO, message);
        message.setUpdatedAt(LocalDateTime.now());
        
        int result = messageMapper.updateById(message);
        return result > 0;
    }
    
    @Override
    @Transactional
    public Boolean deleteMessage(Long id) {
        Message existingMessage = messageMapper.selectById(id);
        if (existingMessage == null) {
            throw new BusinessException("消息不存在");
        }
        
        int result = messageMapper.deleteById(id);
        return result > 0;
    }
    
    @Override
    public MessageVO getMessageById(Long id) {
        MessageVO messageVO = messageMapper.selectMessageVOById(id);
        if (messageVO == null) {
            throw new BusinessException("消息不存在");
        }
        return messageVO;
    }
    
    @Override
    public IPage<MessageVO> getMessagePage(MessageQueryDTO queryDTO) {
        Page<MessageVO> page = new Page<>(queryDTO.getPageNum(), queryDTO.getPageSize());
        return messageMapper.selectMessagePage(page, queryDTO);
    }
    
    @Override
    public IPage<MessageVO> getStudentMessages(Integer pageNum, Integer pageSize) {
        List<Message.TargetType> targetTypes = Arrays.asList(
            Message.TargetType.STUDENT, 
            Message.TargetType.ALL
        );
        
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        List<MessageVO> messages = messageMapper.selectActiveMessagesByTargetTypes(targetTypes, offset, pageSize);
        Long total = messageMapper.countActiveMessagesByTargetTypes(targetTypes);
        
        Page<MessageVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(messages);
        
        return page;
    }
    
    @Override
    public IPage<MessageVO> getTeacherMessages(Integer pageNum, Integer pageSize) {
        List<Message.TargetType> targetTypes = Arrays.asList(
            Message.TargetType.TEACHER, 
            Message.TargetType.ALL
        );
        
        // 计算偏移量
        int offset = (pageNum - 1) * pageSize;
        
        List<MessageVO> messages = messageMapper.selectActiveMessagesByTargetTypes(targetTypes, offset, pageSize);
        Long total = messageMapper.countActiveMessagesByTargetTypes(targetTypes);
        
        Page<MessageVO> page = new Page<>(pageNum, pageSize, total);
        page.setRecords(messages);
        
        return page;
    }
    
    @Override
    @Transactional
    public Boolean toggleMessageStatus(Long id) {
        Message existingMessage = messageMapper.selectById(id);
        if (existingMessage == null) {
            throw new BusinessException("消息不存在");
        }
        
        Message message = new Message();
        message.setId(id);
        message.setIsActive(!existingMessage.getIsActive());
        message.setUpdatedAt(LocalDateTime.now());
        
        int result = messageMapper.updateById(message);
        return result > 0;
    }
}
