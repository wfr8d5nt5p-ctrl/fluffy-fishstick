package com.sky.service;

import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.Orders;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;

import java.util.List;

public interface OrderService {

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO);

    /**
     * 用户支付（微信小程序支付）
     * @param ordersPaymentDTO
     * @return
     */
    OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception;

    /**
     * 支付成功回调处理（修改订单状态、记录结账时间）
     * @param orderNumber 订单号
     */
    void paySuccess(String orderNumber);

    /**
     * 超时未支付自动取消订单（定时任务调用）
     */
    void cancelOrderByTimeout();

    /**
     * 查询当前用户的历史订单（可按状态筛选，status 为 null 返回全部）
     * @param status 订单状态（可空）
     * @return 订单列表，按下单时间倒序
     */
    List<Orders> getHistory(Integer status);
}