package com.raymondlzr.flashsale.mq;

import com.alibaba.fastjson.JSON;
import com.raymondlzr.flashsale.db.dao.OrderDao;
import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.po.Order;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * process completed payment message
 * deduct stock
 */
@Slf4j
@Component
@RocketMQMessageListener(topic = "pay_done", consumerGroup = "pay_done_group")
public class PayDoneConsumer implements RocketMQListener<MessageExt> {
    @Autowired
    private OrderDao orderDao;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Override
    public void onMessage(MessageExt messageExt) {
        String message = new String(messageExt.getBody(), StandardCharsets.UTF_8);
        log.info("Received creating order message：" + message);
        Order order = JSON.parseObject(message, Order.class);
        seckillActivityDao.deductStock(order.getSeckillActivityId());
    }
}
