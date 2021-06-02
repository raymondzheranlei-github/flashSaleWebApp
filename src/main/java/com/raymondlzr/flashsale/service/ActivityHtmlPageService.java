package com.raymondlzr.flashsale.service;

import com.raymondlzr.flashsale.db.dao.SeckillActivityDao;
import com.raymondlzr.flashsale.db.dao.SeckillCommodityDao;
import com.raymondlzr.flashsale.db.po.SeckillActivity;
import com.raymondlzr.flashsale.db.po.SeckillCommodity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class ActivityHtmlPageService {

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private SeckillActivityDao seckillActivityDao;

    @Autowired
    private SeckillCommodityDao seckillCommodityDao;

    /**
     * Create html web page
     *
     * @throws Exception
     */
    public void createActivityHtml(long seckillActivityId) {

        PrintWriter writer = null;
        try {
            SeckillActivity seckillActivity = seckillActivityDao.querySeckillActivityById(seckillActivityId);
            SeckillCommodity seckillCommodity = seckillCommodityDao.querySeckillCommodityById(seckillActivity.getCommodityId());
            // get data of webpage
            Map<String, Object> resultMap = new HashMap<>();
            resultMap.put("seckillActivity", seckillActivity);
            resultMap.put("seckillCommodity", seckillCommodity);
            resultMap.put("seckillPrice", seckillActivity.getSeckillPrice());
            resultMap.put("oldPrice", seckillActivity.getOldPrice());
            resultMap.put("commodityId", seckillActivity.getCommodityId());
            resultMap.put("commodityName", seckillCommodity.getCommodityName());
            resultMap.put("commodityDesc", seckillCommodity.getCommodityDesc());

            // create thymeleaf context object
            Context context = new Context();
            // put data into context object
            context.setVariables(resultMap);

            // create file object with pathname
            // create file for each seckillActivityId
            File file = new File("src/main/resources/templates/" + "seckill_item_" + seckillActivityId + ".html");
            writer = new PrintWriter(file);
            // execute web page staticize technology
            templateEngine.process("seckill_item", context, writer);
        } catch (Exception e) {
            log.error(e.toString());
            log.error("Failed to staticize web page：" + seckillActivityId);
        } finally {
            //close the file whatever there is error or not
            if (writer != null) {
                writer.close();
            }
        }
    }
}

