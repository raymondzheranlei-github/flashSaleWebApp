
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

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RocketMQMessageListener(topic = "pay_check", consumerGroup = "pay_check_group")
public class PayStatusCheckListener implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Resource
    private RedisService redisService;

    //deal with overdue order
    @Override
    public void onMessage(MessageExt messageExt) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received payment status:" + message);
        Order order = JSON.parseObject(message, Order.class);
        //1.query order
        Order orderInfo = orderDao.queryOrder(order.getOrderNo());
        //2.check whether the payment is received for this order
        if (orderInfo.getOrderStatus() != 2) {
            //3.If payment is not received, close the order
            log.info("payment is not received, close the order, orderNo：" + orderInfo.getOrderNo());
            orderInfo.setOrderStatus(99);
            orderDao.updateOrder(orderInfo);
            //4.rollback the data in database
            seckillActivityDao.revertStock(order.getSeckillActivityId());
            // rollback the data in redis
            redisService.revertStock("stock:" + order.getSeckillActivityId());
            //5.remove the userId from the purchased list
            redisService.removeLimitMember(order.getSeckillActivityId(), order.getUserId());
        }
    }
}

