package com.raymondlzr.flashsale.service;

import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.po.SeckillActivity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class SeckillOverSellService {
    @Autowired
    private SeckillActivityDao seckillActivityDao;

    public String  processSeckill(long activityId) {
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(activityId);
        long availableStock = seckillActivity.getAvailableStock();
        String result;
        if (availableStock > 0) {
            result = "Congratualtions! You have purchased successfully";
            System.out.println(result);
            availableStock = availableStock - 1;
            seckillActivity.setAvailableStock(new Integer("" + availableStock));
            seckillActivityDao.updateSeckillActivity(seckillActivity);
        } else {
            result = "Sorry，items are out of stock now. Please try next time";
            System.out.println(result);
        }
        return result;
    }
}
