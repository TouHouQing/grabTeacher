package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.MessageQueryDTO;
import com.touhouqing.grabteacherbackend.model.entity.Message;
import com.touhouqing.grabteacherbackend.model.vo.MessageVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息Mapper接口
 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {
    
    /**
     * 分页查询消息列表
     * 
     * @param page 分页对象
     * @param queryDTO 查询条件
     * @return 消息列表
     */
    IPage<MessageVO> selectMessagePage(Page<MessageVO> page, @Param("query") MessageQueryDTO queryDTO);
    
    /**
     * 根据目标类型查询激活的消息列表
     * 
     * @param targetTypes 目标类型列表
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return 消息列表
     */
    List<MessageVO> selectActiveMessagesByTargetTypes(@Param("targetTypes") List<Message.TargetType> targetTypes, 
                                                     @Param("pageNum") Integer pageNum, 
                                                     @Param("pageSize") Integer pageSize);
    
    /**
     * 根据目标类型统计激活的消息数量
     * 
     * @param targetTypes 目标类型列表
     * @return 消息数量
     */
    Long countActiveMessagesByTargetTypes(@Param("targetTypes") List<Message.TargetType> targetTypes);
    
    /**
     * 根据ID查询消息VO
     * 
     * @param id 消息ID
     * @return 消息VO
     */
    MessageVO selectMessageVOById(@Param("id") Long id);
}
