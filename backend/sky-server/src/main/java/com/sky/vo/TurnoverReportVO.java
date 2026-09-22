package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 营业额统计返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TurnoverReportVO implements Serializable {

    private String dateList;      // 日期列表，如 2022-05-01,2022-05-02,...
    private String turnoverList;  // 营业额列表，如 1200.0,1800.5,...
}