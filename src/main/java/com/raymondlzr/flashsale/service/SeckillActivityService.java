package com.raymondlzr.flashsale.service;

import com.alibaba.fastjson.JSON;
import com.raymondlzr.flashsale.db.dao.OrderDao;
import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.dao.SeckillCommodityDao;
import com.raymondlzr.flashsale.db.po.Order;
import com.raymondlzr.flashsale.db.po.SeckillActivity;
import com.raymondlzr.flashsale.db.po.SeckillCommodity;
import com.raymondlzr.flashsale.mq.RocketMQService;
import com.raymondlzr.flashsale.util.RedisService;
import com.raymondlzr.flashsale.util.SnowFlake;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;

@Slf4j
@Service
public class SeckillActivityService {

    @Autowired
    private RedisService redisService;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private RocketMQService rocketMQService;

    @Autowired
    SeckillCommodityDao seckillCommodityDao;

    @Autowired
    OrderDao orderDao;

    /**
     * datacenterId;
     * machineId;
     * If it's in distributed environment, get machineId from each assigned machine
     * If it's in a single local host, set it as 1
     */
    private SnowFlake snowFlake = new SnowFlake(1, 1);

    /**
     * Create Order
     *
     * @param seckillActivityId
     * @param userId
     * @return
     * @throws Exception
     */
    public Order createOrder(long seckillActivityId, long userId) throws Exception {
        /*
         * 1.Create Order
         */
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        Order order = new Order();
        //Use Snowflake Algorithm to generate Order ID
        order.setOrderNo(String.valueOf(snowFlake.nextId()));
        order.setSeckillActivityId(seckillActivity.getId());
        order.setUserId(userId);
        order.setOrderAmount(seckillActivity.getSeckillPrice().longValue());
        /*
         *2.Send the message of creating order
         */
        rocketMQService.sendMessage("seckill_order", JSON.toJSONString(order));

        /*
         * 3.Send message of payment status
         * open source RocketMQ support delayed message，but not in the precision of seconds.
         * 18-level delayed message，this is broker side configuration：
         * messageDelayLevel=1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
         * Level = 3 means 10s ，which means after 10s the system will check the payment status，if not paid, it will be
           considered as overdue order
         */

        rocketMQService.sendDelayMessage("pay_check", JSON.toJSONString(order), 3);

        return order;
    }

    /**
     * Check whehter there is still available items
     *
     * @param activityId Item ID
     * @return
     */
    public boolean seckillStockValidator(long activityId) {
        String key = "stock:" + activityId;
        return redisService.stockDeductValidator(key);
    }


    /**
     * put the information of flash sale activity into redis
     * @param seckillActivityId
     */
    public void pushSeckillInfoToRedis(long seckillActivityId) {
        //get seckillActivity info by using seckillActivityId in database
        SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        //JSON.toJSONString converts the seckillActivity object to JSONString; parseObject will convert it back
        redisService.setValue("seckillActivity:" + seckillActivityId, JSON.toJSONString(seckillActivity));

        SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        redisService.setValue("seckillCommodity:" + seckillActivity.getCommodityId(), JSON.toJSONString(seckillCommodity));
    }

    /**
     * process order payment
     *
     * @param orderNo
     */
    public void payOrderProcess(String orderNo) throws Exception {
        log.info("Order was placed,  order number：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        if (order == null) {
            log.error("order does not exist：" + orderNo);
            return;
        } else if(order.getOrderStatus() != 1 ) {
            log.error("order is unavailable：" + orderNo);
            return;
        }
        order.setPayTime(new Date());
        order.setOrderStatus(2);
        orderDao.updateOrder(order);
        rocketMQService.sendMessage("pay_done", JSON.toJSONString(order));
    }
}
