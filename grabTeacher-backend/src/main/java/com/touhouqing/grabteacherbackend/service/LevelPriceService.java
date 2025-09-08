package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.touhouqing.grabteacherbackend.model.entity.LevelPrice;

import java.math.BigDecimal;
import java.util.List;

public interface LevelPriceService extends IService<LevelPrice> {
    List<LevelPrice> listAll();

    LevelPrice updatePrice(Long id, BigDecimal price);
}

