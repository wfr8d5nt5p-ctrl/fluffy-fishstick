package com.sky.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 进店点单：店铺基本信息 + 该店完整菜单（按分类分组，含菜品与套餐）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreMenuVO implements Serializable {

    //店铺基本信息
    private StoreVO store;

    //该店菜单（按分类分组）
    private List<CategoryMenu> menus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryMenu implements Serializable {
        private Long id;
        private String name;
        //该分类下的菜品
        private List<MenuItem> dishes;
        //该分类下的套餐
        private List<MenuItem> setmeals;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MenuItem implements Serializable {
        private Long id;
        //1菜品 2套餐
        private Integer type;
        private String name;
        private BigDecimal price;
        private String image;
        private String description;
        //0停售 1起售
        private Integer status;
        //口味（菜品才有）
        private List<String> flavors;
    }
}