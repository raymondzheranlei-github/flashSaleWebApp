package com.raymondlzr.flashsale;

import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.mappers.SeckillActivityMapper;
import com.raymondlzr.flashsale.db.po.SeckillActivity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

@SpringBootTest
public class DaoTest {

    @Autowired
    private SeckillActivityMapper seckillActivityMapper;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Test
    void SeckillActivityTest() {
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName("Test");
        seckillActivity.setCommodityId(999L);
        seckillActivity.setTotalStock(100L);
        seckillActivity.setSeckillPrice(new BigDecimal(99));
        seckillActivity.setActivityStatus(16);
        seckillActivity.setOldPrice(new BigDecimal(99));
        seckillActivity.setAvailableStock(100);
        seckillActivity.setLockStock(0L);
        seckillActivityMapper.insert(seckillActivity);
        System.out.println("====>>>>" + seckillActivityMapper.selectByPrimaryKey(1L));
    }

    @Test
    void setSeckillActivityQuery(){
        List<SeckillActivity> seckillActivitys = seckillActivityDao.querySeckillActivitysByStatus(0);
        System.out.println(seckillActivitys.size());
        seckillActivitys.stream().forEach(seckillActivity -> System.out.println(seckillActivity.toString()));
    }

}
