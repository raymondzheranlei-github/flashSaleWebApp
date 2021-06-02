package com.raymondlzr.flashsale.db.dao;

import com.raymondlzr.flashsale.db.po.Order;

public interface OrderDao {

    void insertOrder(Order order);

    Order queryOrder(String orderNo);

    void updateOrder(Order order);
}
