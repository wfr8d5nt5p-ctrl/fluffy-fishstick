package com.sky.service.impl;

import com.sky.context.BaseContext;
import com.sky.mapper.ReportMapper;
import com.sky.service.ReportService;
import com.sky.vo.OrderReportVO;
import com.sky.vo.SalesTop10ReportVO;
import com.sky.vo.TurnoverReportVO;
import com.sky.vo.UserReportVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportServiceImpl implements ReportService {

    /** 订单状态：5=已完成 */
    private static final Integer ORDER_COMPLETED = 5;
    /** 订单状态：6=已取消 */
    private static final Integer ORDER_CANCELLED = 6;
    /** 有效订单状态：2=待接单,3=待送达,4=派送中,5=已完成 */
    private static final List<Integer> VALID_ORDER_STATUS = List.of(2, 3, 4, 5);

    @Autowired
    private ReportMapper reportMapper;

    @Override
    public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
        // 1. 计算出 begin~end 每一天的日期列表
        List<LocalDate> dateList = new ArrayList<>();
        if (!begin.isAfter(end)) {
            LocalDate cur = begin;
            while (!cur.isAfter(end)) {
                dateList.add(cur);
                cur = cur.plusDays(1);
            }
        }

        // 2. 按天统计营业额（已完成订单的实收金额之和）
        LocalDate today = LocalDate.now();
        List<Double> turnoverList = new ArrayList<>();
        for (LocalDate date : dateList) {
            // 结束时间：若统计的是今天，则截止到当前时刻，否则到当天最后一刻
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = date.isEqual(today)
                    ? LocalDateTime.now()
                    : LocalDateTime.of(date, LocalTime.MAX);

            Double turnover = reportMapper.sumAmountByStatusAndDate(ORDER_COMPLETED, beginTime, endTime, BaseContext.getCurrentStoreId());
            turnoverList.add(turnover == null ? 0.0 : turnover);
        }

        // 3. 拼成逗号分隔字符串
        String dateStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        String turnoverStr = turnoverList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        log.info("营业额统计，日期列表长度：" + dateList.size());
        return TurnoverReportVO.builder()
                .dateList(dateStr)
                .turnoverList(turnoverStr)
                .build();
    }

    @Override
    public UserReportVO getUserStatistics(LocalDate begin, LocalDate end) {
        // 1. 计算 begin~end 每一天的日期列表
        List<LocalDate> dateList = new ArrayList<>();
        if (!begin.isAfter(end)) {
            LocalDate cur = begin;
            while (!cur.isAfter(end)) {
                dateList.add(cur);
                cur = cur.plusDays(1);
            }
        }

        // 2. 按天统计新增用户数和累计用户数
        //    平台管理员(storeId 为 null)看全平台注册用户；商家看本店下过单的独立用户(方案A)
        Long storeId = BaseContext.getCurrentStoreId();
        LocalDate today = LocalDate.now();
        List<Integer> newUserList = new ArrayList<>();
        List<Integer> totalUserList = new ArrayList<>();
        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = date.isEqual(today)
                    ? LocalDateTime.now()
                    : LocalDateTime.of(date, LocalTime.MAX);

            Integer newUser;
            Integer totalUser;
            if (storeId == null) {
                // 平台全局：新增用户=当天注册，累计用户=截止当前注册总数
                newUser = reportMapper.countNewUser(beginTime, endTime);
                totalUser = reportMapper.countTotalUser(endTime);
            } else {
                // 本店口径：新增用户=当天在本店下首单的独立用户，累计用户=截止当前在本店下过单的独立用户
                newUser = reportMapper.countNewUserInStore(beginTime, endTime, storeId);
                totalUser = reportMapper.countTotalUserInStore(endTime, storeId);
            }
            newUserList.add(newUser == null ? 0 : newUser);
            totalUserList.add(totalUser == null ? 0 : totalUser);
        }

        // 3. 拼成逗号分隔字符串
        String dateStr = dateList.stream()
                .map(LocalDate::toString)
                .collect(Collectors.joining(","));
        String newUserStr = newUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        String totalUserStr = totalUserList.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));

        log.info("用户统计，日期列表长度：" + dateList.size());
        return UserReportVO.builder()
                .dateList(dateStr)
                .newUserList(newUserStr)
                .totalUserList(totalUserStr)
                .build();
    }

    @Override
    public OrderReportVO getOrderStatistics(LocalDate begin, LocalDate end) {
        // 1. 计算出 begin~end 每一天的日期列表
        List<LocalDate> dateList = new ArrayList<>();
        if (!begin.isAfter(end)) {
            LocalDate cur = begin;
            while (!cur.isAfter(end)) {
                dateList.add(cur);
                cur = cur.plusDays(1);
            }
        }

        // 2. 按天统计：订单总数 / 有效订单数 / 取消订单数 / 已完成订单数
        LocalDate today = LocalDate.now();
        List<Integer> orderCountList = new ArrayList<>();
        List<Integer> validOrderCountList = new ArrayList<>();
        List<Integer> cancelOrderCountList = new ArrayList<>();
        List<Integer> completedOrderCountList = new ArrayList<>();

        for (LocalDate date : dateList) {
            LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
            LocalDateTime endTime = date.isEqual(today)
                    ? LocalDateTime.now()
                    : LocalDateTime.of(date, LocalTime.MAX);

            Integer orderCount = reportMapper.countAllOrderByDate(beginTime, endTime, BaseContext.getCurrentStoreId());
            // 有效订单数 = 各"有效状态"订单数之和
            int valid = 0;
            for (Integer status : VALID_ORDER_STATUS) {
                Integer c = reportMapper.countOrderByStatusAndDate(status, beginTime, endTime, BaseContext.getCurrentStoreId());
                valid += (c == null ? 0 : c);
            }
            Integer validCount = valid;
            Integer cancelCount = reportMapper.countOrderByStatusAndDate(ORDER_CANCELLED, beginTime, endTime, BaseContext.getCurrentStoreId());
            Integer completedCount = reportMapper.countOrderByStatusAndDate(ORDER_COMPLETED, beginTime, endTime, BaseContext.getCurrentStoreId());

            orderCountList.add(orderCount == null ? 0 : orderCount);
            validOrderCountList.add(validCount == null ? 0 : validCount);
            cancelOrderCountList.add(cancelCount == null ? 0 : cancelCount);
            completedOrderCountList.add(completedCount == null ? 0 : completedCount);
        }

        // 3. 汇总：订单总数 + 有效订单总数 + 完成率 + 有效率
        Integer totalOrderCount = orderCountList.stream().mapToInt(Integer::intValue).sum();
        Integer validOrderCount = validOrderCountList.stream().mapToInt(Integer::intValue).sum();

        // 订单完成率 = 已完成订单总数 / 订单总数
        Integer totalCompletedCount = completedOrderCountList.stream().mapToInt(Integer::intValue).sum();
        Double orderCompletionRate = totalOrderCount == 0 ? 0.0
                : (double) totalCompletedCount / totalOrderCount;
        // 订单有效率 = 有效订单总数 / 订单总数
        Double validOrderRate = totalOrderCount == 0 ? 0.0
                : (double) validOrderCount / totalOrderCount;

        // 4. 拼成逗号分隔字符串
        String dateStr = dateList.stream().map(LocalDate::toString).collect(Collectors.joining(","));
        String orderCountStr = orderCountList.stream().map(String::valueOf).collect(Collectors.joining(","));
        String validCountStr = validOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(","));
        String cancelCountStr = cancelOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(","));
        String completedCountStr = completedOrderCountList.stream().map(String::valueOf).collect(Collectors.joining(","));

        log.info("订单统计，日期列表长度：" + dateList.size());
        return OrderReportVO.builder()
                .dateList(dateStr)
                .orderCountList(orderCountStr)
                .validOrderCountList(validCountStr)
                .cancelOrderCountList(cancelCountStr)
                .completedOrderCountList(completedCountStr)
                .totalOrderCount(totalOrderCount)
                .validOrderCount(validOrderCount)
                .orderCompletionRate(orderCompletionRate)
                .validOrderRate(validOrderRate)
                .build();
    }

    @Override
    public List<SalesTop10ReportVO> getSalesTop10(LocalDate begin, LocalDate end) {
        LocalDateTime beginTime = LocalDateTime.of(begin, LocalTime.MIN);
        LocalDateTime endTime = end.equals(LocalDate.now())
                ? LocalDateTime.now()
                : LocalDateTime.of(end, LocalTime.MAX);
        return reportMapper.getSalesTop10(beginTime, endTime, BaseContext.getCurrentStoreId());
    }

    /** 统计口径为 null 时兜底为 0 */
    private Integer nullSafe(Integer value, LocalDateTime... ignored) {
        return value == null ? 0 : value;
    }
}