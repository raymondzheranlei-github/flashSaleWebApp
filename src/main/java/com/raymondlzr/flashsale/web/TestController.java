package com.raymondlzr.flashsale.web;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Controller
public class TestController {

    @ResponseBody
    @RequestMapping("hello")
    public String hello(){
        String result;
        // Resource name can use any meaningful string
        // SphU.entry("HelloResource") must be same as the name in seckillsFlow() rule2.setResource("HelloResource");
        // "HelloResource" here is like an id
        try (Entry entry = SphU.entry("HelloResource")){
            // protected service
            result  = "Hello Sentinel";
            return result;
        }catch (BlockException ex) {
            // QPS is over threshold
            // The query request is stopped due to rate limits
            log.error(ex.toString());
            result = "Server is busy. Please try again later";
            return  result;
        }
    }

    /**
     *  define the rule of rate limits
     *  1.create the list of rules of rate limits
     *  2.create single rule of rate limits
     *  3.put the rate limit rule into the list
     *  4.load the rules
     *  @PostConstruct Spring calls methods annotated with @PostConstruct only once,
     *  just after the initialization of bean properties.
     *  Keep in mind that these methods will run even if there is nothing to initialize.
     *  The method annotated with @PostConstruct can have any access level but it can't be static.
     */
    @PostConstruct
    public void seckillsFlow(){
        //1.create the list of rules of rate limits
        List<FlowRule> rules = new ArrayList<>();
        //2.create single rule of rate limits
        FlowRule rule = new FlowRule();
        //define resource, tell which resource will sentinel work for
        rule.setResource("seckills");
        //define the rule of rate limit as QPS
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        //define approved QPS 1/sec
        rule.setCount(1);

        FlowRule rule2 = new FlowRule();
        rule2.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule2.setCount(2);
        rule2.setResource("HelloResource");
        //3.put the rate limit rule into the list
        rules.add(rule);
        rules.add(rule2);
        //4.load the rules
        FlowRuleManager.loadRules(rules);
    }
}