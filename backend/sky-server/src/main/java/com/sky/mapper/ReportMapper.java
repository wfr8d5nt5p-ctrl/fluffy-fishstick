package com.sky.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.sky.vo.SalesTop10ReportVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表统计 Mapper
 */
@Mapper
public interface ReportMapper {

    /**
     * 统计指定状态、指定时间范围内的订单营业额（按店铺隔离，平台管理员 storeId 为 null 时查全部）
     */
    Double sumAmountByStatusAndDate(@Param("status") Integer status,
                                    @Param("begin") LocalDateTime begin,
                                    @Param("end") LocalDateTime end,
                                    @Param("storeId") Long storeId);

    /**
     * 统计指定时间范围内新增的用户数（平台全局：按注册时间）
     */
    @Select("select count(*) from user where create_time between #{begin} and #{end}")
    Integer countNewUser(LocalDateTime begin, LocalDateTime end);

    /**
     * 统计指定时间之前（累计）的用户总数（平台全局：按注册时间）
     */
    @Select("select count(*) from user where create_time < #{end}")
    Integer countTotalUser(LocalDateTime end);

    /**
     * 统计指定时间范围内在本店下首单（成为本店新顾客）的独立用户数（按店铺隔离）
     */
    Integer countNewUserInStore(@Param("begin") LocalDateTime begin,
                                @Param("end") LocalDateTime end,
                                @Param("storeId") Long storeId);

    /**
     * 统计指定时间之前在本店下过单的累计独立用户数（按店铺隔离）
     */
    Integer countTotalUserInStore(@Param("end") LocalDateTime end,
                                  @Param("storeId") Long storeId);

    /**
     * 统计指定状态、指定时间范围内的订单数（按店铺隔离，平台管理员 storeId 为 null 时查全部）
     */
    Integer countOrderByStatusAndDate(@Param("status") Integer status,
                                      @Param("begin") LocalDateTime begin,
                                      @Param("end") LocalDateTime end,
                                      @Param("storeId") Long storeId);

    /**
     * 统计指定时间范围内的订单总数（按店铺隔离，平台管理员 storeId 为 null 时查全部）
     */
    Integer countAllOrderByDate(@Param("begin") LocalDateTime begin,
                                @Param("end") LocalDateTime end,
                                @Param("storeId") Long storeId);

    /**
     * 统计指定时间范围内销量排名 Top10 的菜品（按店铺隔离，平台管理员 storeId 为 null 时查全部）
     */
    List<SalesTop10ReportVO> getSalesTop10(@Param("begin") LocalDateTime begin,
                                           @Param("end") LocalDateTime end,
                                           @Param("storeId") Long storeId);
}