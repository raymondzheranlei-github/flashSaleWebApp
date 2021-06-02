package com.raymondlzr.flashsale.mq;

import com.alibaba.fastjson.JSON;
import com.raymondlzr.flashsale.db.dao.OrderDao;
import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.po.Order;
import com.raymondlzr.flashsale.util.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
@RocketMQMessageListener(topic = "seckill_order", consumerGroup = "seckill_order_group")
public class OrderConsumer implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    RedisService redisService;

    @Override
    @Transactional
    public void onMessage (MessageExt messageExt) {
        //1.analyze message of creating order request
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("The request of creating order received：" + message);
        Order order = JSON.parseObject(message, Order.class);
        order.setCreateTime(new Date());
        //2.Deduct stock
        boolean lockStockResult = seckillActivityDao.lockStock(order.getSeckillActivityId());
        if (lockStockResult) {
            //Order status 0:Out of Stock，invalid order 1: order generated, waiting for payment
            order.setOrderStatus(1);
            // add the user to the limited member
            redisService.addLimitMember(order.getSeckillActivityId(), order.getUserId());
        } else {
            order.setOrderStatus(0);
        }
        //3.insert order to Dao
        orderDao.insertOrder(order);
    }
}
