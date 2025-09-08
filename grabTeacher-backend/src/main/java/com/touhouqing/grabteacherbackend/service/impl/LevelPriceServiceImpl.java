package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.touhouqing.grabteacherbackend.mapper.LevelPriceMapper;
import com.touhouqing.grabteacherbackend.model.entity.LevelPrice;
import com.touhouqing.grabteacherbackend.service.LevelPriceService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LevelPriceServiceImpl extends ServiceImpl<LevelPriceMapper, LevelPrice> implements LevelPriceService {
    @Override
    public List<LevelPrice> listAll() {
        return this.lambdaQuery().orderByAsc(LevelPrice::getId).list();
    }

    @Override
    public LevelPrice updatePrice(Long id, BigDecimal price) {
        if (id == null || price == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("价格不能为负数");
        }
        LevelPrice levelPriceEntity = this.getById(id);
        if (levelPriceEntity == null) {
            throw new IllegalArgumentException("级别不存在");
        }
        levelPriceEntity.setPrice(price);
        levelPriceEntity.setUpdateTime(LocalDateTime.now());
        this.updateById(levelPriceEntity);
        return levelPriceEntity;
    }
}

