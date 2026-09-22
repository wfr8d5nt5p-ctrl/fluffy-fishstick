package com.sky.controller.user;

import com.sky.result.Result;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C端套餐查询接口
 */
@RestController("userSetmealController")
@RequestMapping("/user/setmeal")
@Api(tags = "C端套餐相关接口")
@Slf4j
public class SetmealController {

    @Autowired
    private SetmealService setmealService;

    /**
     * 根据分类id查询套餐
     * @param categoryId 分类id
     * @return
     */
    @GetMapping("/list")
    @Cacheable(cacheNames = "setmealCache", key = "#categoryId")
    @ApiOperation("根据分类id查询套餐")
    public Result<List<SetmealVO>> list(Long categoryId) {
        log.info("C端查询套餐列表，categoryId={}", categoryId);
        List<SetmealVO> list = setmealService.listByCategory(categoryId);
        return Result.success(list);
    }
}