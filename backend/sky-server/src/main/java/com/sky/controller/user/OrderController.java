package com.sky.controller.user;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.result.Result;
import com.sky.service.OrderService;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * C端订单相关接口
 */
@RestController("userOrderController")
@RequestMapping("/user/order")
@Api(tags = "C端订单接口")
@Slf4j
public class OrderController {

    @Autowired
    private OrderService orderService;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @PostMapping("/submit")
    @ApiOperation("用户下单")
    public Result<OrderSubmitVO> submit(@RequestBody OrdersSubmitDTO ordersSubmitDTO) {
        log.info("用户下单：{}", ordersSubmitDTO);
        return Result.success(orderService.submitOrder(ordersSubmitDTO));
    }

    /**
     * 用户支付
     * @param ordersPaymentDTO
     * @return
     */
    @PostMapping("/payment")
    @ApiOperation("用户支付")
    public Result<OrderPaymentVO> payment(@RequestBody OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        log.info("用户支付：{}", ordersPaymentDTO);
        return Result.success(orderService.payment(ordersPaymentDTO));
    }

    /**
     * 支付成功回调：微信支付成功后通知后端，true调用处更新订单状态并推送来单提醒
     * 注：正式环境应在此校验微信回调签名并解析回调XML，本例为简化触发入口
     * @param body 请求体，含 orderNumber（商户订单号）
     * @return
     */
    @PostMapping("/notify")
    @ApiOperation("支付成功回调")
    public Result<String> notifyPayment(@RequestBody Map<String, String> body) {
        String orderNumber = body.get("orderNumber");
        log.info("支付成功回调，订单号：{}", orderNumber);
        orderService.paySuccess(orderNumber);
        return Result.success();
    }

    /**
     * 查询当前用户的历史订单（可按状态筛选）
     * @param status 订单状态，可空
     * @return
     */
    @GetMapping("/history")
    @ApiOperation("查询我的历史订单")
    public Result<List<Orders>> history(@RequestParam(value = "status", required = false) Integer status) {
        log.info("查询我的历史订单：status={}", status);
        return Result.success(orderService.getHistory(status));
    }
}