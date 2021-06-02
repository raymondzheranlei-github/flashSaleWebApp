package com.raymondlzr.flashsale.db.mappers;

import com.raymondlzr.flashsale.db.po.SeckillActivity;

import java.util.List;

public interface SeckillActivityMapper {
    int deleteByPrimaryKey(Long id);

    int insert(SeckillActivity record);

    int insertSelective(SeckillActivity record);

    SeckillActivity selectByPrimaryKey(Long id);

    int updateByPrimaryKeySelective(SeckillActivity record);

    int updateByPrimaryKey(SeckillActivity record);

    List<SeckillActivity> querySeckillActivitysByStatus(int activityStatus);

    int lockStock(Long id);

    int deductStock(Long id);

    void revertStock(Long seckillActivityId);
}