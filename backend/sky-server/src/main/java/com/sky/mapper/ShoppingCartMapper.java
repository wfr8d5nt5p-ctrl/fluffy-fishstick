package com.sky.mapper;

import com.sky.entity.ShoppingCart;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface ShoppingCartMapper {

    /**
     * 按条件查询购物车记录（可组合：用户id、菜品id、套餐id、口味）
     * @param shoppingCart
     * @return
     */
    List<ShoppingCart> list(ShoppingCart shoppingCart);

    /**
     * 新增购物车记录
     * @param shoppingCart
     */
    void insert(ShoppingCart shoppingCart);

    /**
     * 修改购物车中某条记录的数量
     * @param shoppingCart
     */
    void updateNumberById(ShoppingCart shoppingCart);

    /**
     * 根据id删除购物车记录
     * @param id
     */
    void deleteById(Long id);

    /**
     * 清空某用户的购物车
     * @param userId
     */
    void deleteByUserId(Long userId);
}