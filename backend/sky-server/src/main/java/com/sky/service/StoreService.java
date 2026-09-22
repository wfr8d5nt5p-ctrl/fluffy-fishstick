package com.sky.service;

import com.sky.vo.StoreMenuVO;
import com.sky.vo.StoreVO;

import java.util.List;

public interface StoreService {

    /**
     * 查询营业中的商家列表
     * @param keyword 关键词，可空
     * @param category 主营分类，可空
     * @return
     */
    List<StoreVO> listStores(String keyword, String category);

    /**
     * 查询店铺详情及完整菜单（进店点单）
     * @param storeId
     * @return
     */
    StoreMenuVO getStoreMenu(Long storeId);
}