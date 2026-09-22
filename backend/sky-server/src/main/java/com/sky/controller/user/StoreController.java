package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.StoreService;
import com.sky.vo.StoreMenuVO;
import com.sky.vo.StoreVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端商家（店铺）相关接口
 */
@RestController("userStoreController")
@RequestMapping("/user/store")
@Api(tags = "C端商家相关接口")
@Slf4j
public class StoreController {

    @Autowired
    private StoreService storeService;

    /**
     * 查询附近好店/商家列表（支持搜索与主营分类筛选）
     * @param keyword 关键词，可空
     * @param category 主营分类，可空
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询商家列表")
    public Result<List<StoreVO>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String category) {
        log.info("C端查询商家列表，keyword={}, category={}", keyword, category);
        return Result.success(storeService.listStores(keyword, category));
    }

    /**
     * 查询店铺详情及菜单（进店点单）
     * @param id 商家id
     * @return
     */
    @GetMapping("/{id}/menu")
    @ApiOperation("查询店铺详情及菜单")
    public Result<StoreMenuVO> menu(@PathVariable Long id) {
        log.info("C端查询店铺菜单，storeId={}", id);
        return Result.success(storeService.getStoreMenu(id));
    }
}