package com.raymondlzr.flashsale.db.dao;

import com.raymondlzr.flashsale.db.mappers.SeckillCommodityMapper;
import com.raymondlzr.flashsale.db.po.SeckillCommodity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class SeckillCommodityDaoImpl implements SeckillCommodityDao {

    @Resource
    private SeckillCommodityMapper seckillCommodityMapper;

    @Override
    public SeckillCommodity querySeckillCommodityById(long commodityId) {
        return seckillCommodityMapper.selectByPrimaryKey(commodityId);
    }
}
