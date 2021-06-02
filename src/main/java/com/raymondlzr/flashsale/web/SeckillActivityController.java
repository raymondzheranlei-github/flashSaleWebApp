package com.raymondlzr.flashsale.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.fastjson.JSON;
import com.raymondlzr.flashsale.db.dao.OrderDao;
import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.dao.SeckillCommodityDao;
import com.raymondlzr.flashsale.db.po.Order;
import com.raymondlzr.flashsale.db.po.SeckillActivity;
import com.raymondlzr.flashsale.db.po.SeckillCommodity;
import com.raymondlzr.flashsale.service.SeckillActivityService;
import com.raymondlzr.flashsale.util.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
public class SeckillActivityController {

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    @Autowired
    SeckillActivityService seckillActivityService;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    RedisService redisService;


    @RequestMapping("/addSeckillActivity")
    public String addSeckillActivity() {
        return "add_activity";
    }

    @RequestMapping("/addSeckillActivityAction")
    public String addSeckillActivityAction(
            @RequestParam("name") String name,
            @RequestParam("commodityId") long commodityId,
            @RequestParam("seckillPrice") BigDecimal seckillPrice,
            @RequestParam("oldPrice") BigDecimal oldPrice,
            @RequestParam("seckillNumber") long seckillNumber,
            @RequestParam("startTime") String startTime,
            @RequestParam("endTime") String endTime,
            Map<String, Object> resultMap
    ) throws ParseException {
        startTime = startTime.substring(0, 10) +  startTime.substring(11);
        endTime = endTime.substring(0, 10) +  endTime.substring(11);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-ddhh:mm");
        SeckillActivity seckillActivity = new SeckillActivity();
        seckillActivity.setName(name);
        seckillActivity.setCommodityId(commodityId);
        seckillActivity.setSeckillPrice(seckillPrice);
        seckillActivity.setOldPrice(oldPrice);
        seckillActivity.setTotalStock(seckillNumber);
        seckillActivity.setAvailableStock(new Integer("" + seckillNumber));
        seckillActivity.setLockStock(0L);
        seckillActivity.setActivityStatus(1);
        seckillActivity.setStartTime(format.parse(startTime));
        seckillActivity.setEndTime(format.parse(endTime));
        seckillActivityDao.inertSeckillActivity(seckillActivity);
        resultMap.put("seckillActivity", seckillActivity);
        return "add_success";
    }

    @RequestMapping("/seckills")
    public String activityList(Map<String, Object> resultMap) {
        try (Entry entry = SphU.entry("seckills")) {
            //protected resource
            List<SeckillActivity> seckillActivities = seckillActivityDao.querySeckillActivitysByStatus(1);
            resultMap.put("seckillActivities", seckillActivities);
            return "seckill_activity";
        } catch (BlockException ex) {
            //if the QPS is over the threshold, direct to wait.html
            log.error("The list of searching flash sale activity has been rate limited"+ex.toString());
            return "wait";
        }
    }


    @RequestMapping("/item/{seckillActivityId}")
    public String itemPage(Map<String, Object> resultMap, @PathVariable long seckillActivityId) {
        SeckillActivity seckillActivity;
        SeckillCommodity seckillCommodity;
        //by using seckillActivity+id as key, get seckillActivityInfo from Redis
        String seckillActivityInfo = redisService.getValue("seckillActivity:" + seckillActivityId);
        if (StringUtils.isNotEmpty(seckillActivityInfo)) {
            log.info("redis cached data:" + seckillActivityInfo);
            //parseObject is the open sourced by alibaba
            //convert the json to designated object, here convert it to SeckillActivity object
            seckillActivity = JSON.parseObject(seckillActivityInfo, SeckillActivity.class);
        } else {
            //if info not in redis, query in database
            seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
        }
        //same logic, get the information of the item
        String seckillCommodityInfo = redisService.getValue("seckillCommodity:" + seckillActivity.getCommodityId());
        if (StringUtils.isNotEmpty(seckillCommodityInfo)) {
            log.info("redis cached data:" + seckillCommodityInfo);
            seckillCommodity = JSON.parseObject(seckillActivityInfo, SeckillCommodity.class);
        } else {
            seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
        }

        resultMap.put("seckillActivity", seckillActivity);
        resultMap.put("seckillCommodity", seckillCommodity);
        resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
        resultMap.put("oldPrice", seckillActivity.getOldPrice());
        resultMap.put("commodityId", seckillActivity.getCommodityId());
        resultMap.put("commodityName", seckillCommodity.getCommodityName());
        resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());
        return "seckill_item";
    }

    /**
     * Process flash sale purchase request
     * @param userId
     * @param seckillActivityId
     * @return
     */
    @RequestMapping("/seckill/buy/{userId}/{seckillActivityId}")
    public ModelAndView seckillCommodity(@PathVariable long userId, @PathVariable long seckillActivityId) {
        boolean stockValidateResult = false;

        ModelAndView modelAndView = new ModelAndView();
        try {
            /*
             * check whether the user is in the list of purchased userId
             */
            if (redisService.isInLimitMember(seckillActivityId, userId)) {
                modelAndView.addObject("resultInfo", "Sorry, you cannot purchase again");
                modelAndView.setViewName("seckill_result");
                return modelAndView;
            }
            /*
             * check whether it's available to join in the flash sale
             */
            //Use Lua Scripting to check whether there is available stock
            stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
            if (stockValidateResult) {
                //the way to get the created order
                Order order = seckillActivityService.createOrder(seckillActivityId, userId);
                modelAndView.addObject("resultInfo",
                        "Getting the item successfully. Order is creating，orderID：" + order.getOrderNo());
                modelAndView.addObject("orderNo",order.getOrderNo());
            } else {
                modelAndView.addObject("resultInfo","Sorry, the item is out of stock");
            }
        } catch (Exception e) {
            log.error("System is down" + e.toString());
            modelAndView.addObject("resultInfo","Server is busy. Please try again");
        }
        modelAndView.setViewName("seckill_result");
        return modelAndView;
    }

    /**
     * order Query
     * @param orderNo
     * @return
     */
    @RequestMapping("/seckill/orderQuery/{orderNo}")
    public ModelAndView orderQuery(@PathVariable String orderNo) {
        log.info("order number：" + orderNo);
        Order order = orderDao.queryOrder(orderNo);
        ModelAndView modelAndView = new ModelAndView();

        if (order != null) {
            modelAndView.setViewName("order");
            modelAndView.addObject("order", order);
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(order.getSeckillActivityId());
            modelAndView.addObject("seckillActivity", seckillActivity);
        } else {
            modelAndView.setViewName("order_wait");
        }
        return modelAndView;
    }

    /**
     * payment
     * @return
     */
    @RequestMapping("/seckill/payOrder/{orderNo}")
    public String payOrder(@PathVariable String orderNo) throws Exception {
        seckillActivityService.payOrderProcess(orderNo);
        //After paying successfully, page will jump to orderQuery page
        return "redirect:/seckill/orderQuery/" + orderNo;
    }

    /**
     * get the current time from backend
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/getSystemTime")
    public String getSystemTime() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//set date format
        String date = df.format(new Date());// new Date() to get current system time
        return date;
    }
}
