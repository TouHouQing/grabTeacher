package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.BalanceTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface BalanceTransactionMapper extends BaseMapper<BalanceTransaction> {

    /**
     * 根据用户ID查询余额变动记录
     */
    @Select("SELECT * FROM balance_transactions WHERE user_id = #{userId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<BalanceTransaction> findByUserId(@Param("userId") Long userId);

    /**
     * 根据预约ID查询余额变动记录
     */
    @Select("SELECT * FROM balance_transactions WHERE booking_id = #{bookingId} AND is_deleted = 0")
    List<BalanceTransaction> findByBookingId(@Param("bookingId") Long bookingId);
}
