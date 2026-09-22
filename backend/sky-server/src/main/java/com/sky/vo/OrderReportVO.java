package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 订单统计报告 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderReportVO implements Serializable {

    /** 日期列表 */
    private String dateList;

    /** 每日订单数 */
    private String orderCountList;

    /** 每日有效订单数 */
    private String validOrderCountList;

    /** 每日取消订单数 */
    private String cancelOrderCountList;

    /** 每日已完成订单数 */
    private String completedOrderCountList;

    /** 订单总数 */
    private Integer totalOrderCount;

    /** 有效订单总数 */
    private Integer validOrderCount;

    /** 订单完成率 */
    private Double orderCompletionRate;

    /** 订单有效率 */
    private Double validOrderRate;
}