package com.sky.task;

import com.sky.service.OrderService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时任务类，处理订单超时未支付等业务
 */
@Component
@Slf4j
public class OrderTask {

    @Autowired
    private OrderService orderService;

    /**
     * 每分钟执行一次，处理下单超过15分钟仍未支付的订单，自动将其取消
     */
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        log.info("定时任务：开始扫描超时未支付订单");
        orderService.cancelOrderByTimeout();
    }
}