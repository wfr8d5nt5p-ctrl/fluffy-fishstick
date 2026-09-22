package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户统计返回VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserReportVO implements Serializable {

    private String dateList;      // 日期列表，如 2022-05-01,2022-05-02,...
    private String totalUserList; // 累计用户列表，如 100,150,210,...
    private String newUserList;   // 新增用户列表，如 10,20,30,...
}