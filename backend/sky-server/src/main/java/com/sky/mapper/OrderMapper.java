package com.sky.mapper;

import com.sky.entity.Orders;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    /**
     * 保存订单
     * @param orders
     */
    void insert(Orders orders);

    /**
     * 根据订单号查询订单
     * @param orderNumber
     * @return
     */
    @Select("select * from orders where number = #{orderNumber}")
    Orders getByNumber(String orderNumber);

    /**
     * 修改订单信息（动态）
     * @param orders
     */
    void update(Orders orders);

    /**
     * 根据用户id和订单状态查询订单
     * @param userId
     * @param status
     * @return
     */
    @Select("select * from orders where user_id = #{userId} and status = #{status}")
    List<Orders> getByUserIdAndStatus(Long userId, Integer status);

    /**
     * 查询超时未支付的订单（待付款且下单时间早于给定时间）
     * @param status 订单状态（待付款）
     * @param orderTime 下单时间临界点
     * @return
     */
    @Select("select * from orders where status = #{status} and order_time < #{orderTime}")
    List<Orders> getByStatusAndOrderTimeBefore(Integer status, LocalDateTime orderTime);

    /**
     * 查询当前用户的历史订单（可按状态筛选，status 为 null 时返回全部）
     * @param userId 用户id
     * @param status 订单状态（可空）
     * @return
     */
    @Select("select * from orders where user_id = #{userId} and (#{status} is null or status = #{status}) order by order_time desc")
    List<Orders> getHistory(Long userId, Integer status);
}