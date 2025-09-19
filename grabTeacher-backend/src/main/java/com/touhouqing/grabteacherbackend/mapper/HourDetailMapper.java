package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.HourDetail;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface HourDetailMapper extends BaseMapper<HourDetail> {
    @Select("SELECT * FROM hour_details WHERE user_id = #{userId} ORDER BY id DESC LIMIT 1")
    HourDetail findLastByUserId(@Param("userId") Long userId);
}


