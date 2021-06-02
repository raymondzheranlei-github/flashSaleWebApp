package com.raymondlzr.flashsale.web;

import com.raymondlzr.flashsale.service.SeckillActivityService;
import com.raymondlzr.flashsale.service.SeckillOverSellService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SeckillOverSellController {

    @Autowired
    private SeckillOverSellService seckillOverSellService;



    /**
     * process buying request in naive way
     * @param seckillActivityId
     * @return
     */
//    @ResponseBody
//    @RequestMapping("/seckill/{seckillActivityId}")
    public String  seckil(@PathVariable long seckillActivityId){
        return seckillOverSellService.processSeckill(seckillActivityId);
    }
    @Autowired
    private SeckillActivityService seckillActivityService;

    /**
     * use lua script to process buying request
     * @param seckillActivityId
     * @return
     */
    @ResponseBody
    @RequestMapping("/seckill/{seckillActivityId}")
    public String seckillCommodity(@PathVariable long seckillActivityId) {
        boolean stockValidateResult = seckillActivityService.seckillStockValidator(seckillActivityId);
        return stockValidateResult ? "Congratulation! You purchased the item successfully" :
                "Item is out of stock. Please come next time.";
    }
}
