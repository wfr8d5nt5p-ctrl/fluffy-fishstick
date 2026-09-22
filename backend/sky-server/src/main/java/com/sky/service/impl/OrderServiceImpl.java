package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.context.BaseContext;
import com.sky.dto.OrdersPaymentDTO;
import com.sky.dto.OrdersSubmitDTO;
import com.sky.entity.*;
import com.sky.exception.OrderBusinessException;
import com.sky.mapper.*;
import com.sky.service.OrderService;
import com.sky.utils.WeChatPayUtil;
import com.sky.vo.OrderPaymentVO;
import com.sky.vo.OrderSubmitVO;
import com.sky.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.sky.constant.MessageConstant.*;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private OrderDetailMapper orderDetailMapper;
    @Autowired
    private AddressBookMapper addressBookMapper;
    @Autowired
    private ShoppingCartMapper shoppingCartMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private WeChatPayUtil weChatPayUtil;

    @Autowired
    private WebSocketServer webSocketServer;

    /**
     * 用户下单
     * @param ordersSubmitDTO
     * @return
     */
    @Transactional
    public OrderSubmitVO submitOrder(OrdersSubmitDTO ordersSubmitDTO) {
        // 1. 处理购物车为空的异常
        Long userId = BaseContext.getCurrentId();
        ShoppingCart shoppingCart = new ShoppingCart();
        shoppingCart.setUserId(userId);

        List<ShoppingCart> shoppingCartList = shoppingCartMapper.list(shoppingCart);
        if (shoppingCartList == null || shoppingCartList.size() == 0) {
            throw new OrderBusinessException(SHOPPING_CART_IS_NULL);
        }

        // 2. 校验地址簿
        AddressBook addressBook = addressBookMapper.getById(ordersSubmitDTO.getAddressBookId());
        if (addressBook == null) {
            throw new OrderBusinessException(ADDRESS_BOOK_IS_NULL);
        }

        // 3. 构造订单实体
        Orders orders = new Orders();
        BeanUtils.copyProperties(ordersSubmitDTO, orders);

        orders.setNumber(String.valueOf(System.currentTimeMillis()));  // 订单号
        orders.setStatus(Orders.PENDING_PAYMENT);                      // 待付款
        orders.setUserId(userId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setPayStatus(Orders.UN_PAID);                           // 未支付
        orders.setUserName(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress("省:" + addressBook.getProvinceName()
                + " 市:" + addressBook.getCityName()
                + " 区:" + addressBook.getDistrictName());
        orders.setConsignee(addressBook.getConsignee());

        // 4. 保存订单，拿回自增id
        orderMapper.insert(orders);

        // 5. 构造订单明细（从购物车数据拷贝）
        List<OrderDetail> orderDetailList = new ArrayList<>();
        for (ShoppingCart cart : shoppingCartList) {
            OrderDetail orderDetail = new OrderDetail();
            BeanUtils.copyProperties(cart, orderDetail);
            orderDetail.setOrderId(orders.getId());   // 本次订单id
            orderDetailList.add(orderDetail);
        }
        orderDetailMapper.insertBatch(orderDetailList);

        // 6. 清空购物车
        shoppingCartMapper.deleteByUserId(userId);

        // 7. 封装返回结果
        return OrderSubmitVO.builder()
                .id(orders.getId())
                .orderNumber(orders.getNumber())
                .orderAmount(orders.getAmount())
                .orderTime(orders.getOrderTime())
                .build();
    }

    /**
     * 用户支付（微信小程序支付）
     * @param ordersPaymentDTO
     * @return
     */
    public OrderPaymentVO payment(OrdersPaymentDTO ordersPaymentDTO) throws Exception {
        // 1. 根据订单号查询订单
        Orders orders = orderMapper.getByNumber(ordersPaymentDTO.getOrderNumber());
        if (orders == null) {
            throw new OrderBusinessException(ORDER_NOT_FOUND);
        }

        // 2. 查询下单用户的openid
        User user = userMapper.getById(orders.getUserId());
        String openid = user.getOpenid();

        // 3. 调用微信支付统一下单
        JSONObject jsonObject = weChatPayUtil.pay(orders.getNumber(),
                orders.getAmount(), "苍穹外卖订单", openid);

        System.out.println(jsonObject.toJSONString());
        // 4. 返回前端需要调起支付的参数
        OrderPaymentVO vo = new OrderPaymentVO();
        if (jsonObject.getString("code") == null) {
            vo.setNonceStr(jsonObject.getString("nonceStr"));
            vo.setPaySign(jsonObject.getString("paySign"));
            vo.setTimeStamp(jsonObject.getString("timeStamp"));
            vo.setSignType(jsonObject.getString("signType"));
            vo.setPackageStr(jsonObject.getString("package"));
        }
        return vo;
    }

    /**
     * 支付成功回调：修改订单状态、记录结账时间
     * @param orderNumber 商户订单号
     */
    public void paySuccess(String orderNumber) {
        Orders orders = orderMapper.getByNumber(orderNumber);
        if (orders == null) {
            throw new OrderBusinessException(ORDER_NOT_FOUND);
        }
        Orders updated = new Orders();
        updated.setId(orders.getId());
        updated.setStatus(Orders.TO_BE_CONFIRMED);   // 待接单
        updated.setPayStatus(Orders.PAID);           // 已支付
        updated.setCheckoutTime(LocalDateTime.now());
        orderMapper.update(updated);

        // 支付成功（真正来单）后推送来单提醒
        sendOrderReminder(orders);
    }

    /**
     * 构造来单提醒消息并推送给所有在线的商家端 WebSocket 客户端
     * 消息格式为 JSON：{"type":1, "orderId":订单id, "content":内容}
     */
    private void sendOrderReminder(Orders orders) {
        com.alibaba.fastjson.JSONObject message = new com.alibaba.fastjson.JSONObject();
        message.put("type", 1);                              // 1=来单提醒
        message.put("orderId", orders.getId());
        message.put("content", "订单号：" + orders.getNumber() + " 的新订单，请及时处理！");
        String json = message.toJSONString();
        webSocketServer.sendToAllClient(json);
        log.info("来单提醒已推送：{}", json);
    }

    /**
     * 超时未支付自动取消订单（定时任务调用）
     */
    @Transactional
    public void cancelOrderByTimeout() {
        // 待付款且下单时间超过15分钟的订单
        LocalDateTime time = LocalDateTime.now().minusMinutes(15);
        List<Orders> orderList = orderMapper.getByStatusAndOrderTimeBefore(Orders.PENDING_PAYMENT, time);

        if (orderList == null || orderList.isEmpty()) {
            log.info("定时任务：无超时未支付订单");
            return;
        }

        for (Orders orders : orderList) {
            Orders updated = new Orders();
            updated.setId(orders.getId());
            updated.setStatus(Orders.CANCELLED);                          // 已取消
            updated.setCancelReason("订单超时未支付，系统自动取消");
            updated.setCancelTime(LocalDateTime.now());
            orderMapper.update(updated);
            log.info("定时任务：订单 id={}，订单号={} 因超时未支付已自动取消", orders.getId(), orders.getNumber());
        }
    }

    /**
     * 查询当前用户的历史订单
     * @param status 订单状态（可空，null 返回全部）
     * @return
     */
    public List<Orders> getHistory(Integer status) {
        Long userId = BaseContext.getCurrentId();
        List<Orders> list = orderMapper.getHistory(userId, status);
        return list == null ? new ArrayList<>() : list;
    }
}