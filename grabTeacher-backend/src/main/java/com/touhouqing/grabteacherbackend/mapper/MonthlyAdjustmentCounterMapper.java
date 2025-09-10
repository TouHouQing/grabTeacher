package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.MonthlyAdjustmentCounter;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface MonthlyAdjustmentCounterMapper extends BaseMapper<MonthlyAdjustmentCounter> {

    @Select("SELECT * FROM monthly_adjustment_counters WHERE actor_type=#{actorType} AND actor_id=#{actorId} AND enrollment_id=#{enrollmentId} AND month_key=#{monthKey} AND is_deleted=0 LIMIT 1")
    MonthlyAdjustmentCounter findOne(@Param("actorType") String actorType,
                                     @Param("actorId") Long actorId,
                                     @Param("enrollmentId") Long enrollmentId,
                                     @Param("monthKey") String monthKey);

    @Update("UPDATE monthly_adjustment_counters SET used_times = used_times + 1 WHERE id = #{id}")
    int incrementUsed(@Param("id") Long id);

    @Update("UPDATE monthly_adjustment_counters SET used_times = CASE WHEN used_times>0 THEN used_times-1 ELSE 0 END WHERE id = #{id}")
    int decrementUsed(@Param("id") Long id);
}

