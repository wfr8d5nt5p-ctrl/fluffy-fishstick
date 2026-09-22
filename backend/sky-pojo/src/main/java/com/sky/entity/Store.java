package com.sky.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 商家（店铺）信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Store implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    //店铺名称
    private String name;

    //主营分类（如：快餐、甜品、饮品等）
    private String category;

    //评分
    private Double rating;

    //月售
    private Integer monthlySales;

    //预计配送时长（分钟）
    private Integer deliveryTime;

    //距离（公里）
    private Double distanceKm;

    //店铺图片
    private String photo;

    //起送价
    private BigDecimal minPrice;

    //配送费
    private BigDecimal deliverFee;

    //店铺地址
    private String address;

    //经度
    private Double longitude;

    //纬度
    private Double latitude;

    //状态 1营业 0打烊
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private Long createUser;

    private Long updateUser;
}