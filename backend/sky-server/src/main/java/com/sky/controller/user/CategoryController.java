package com.sky.controller.user;

import com.sky.entity.Category;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

/**
 * C端分类查询接口
 */
@RestController("userCategoryController")
@RequestMapping("/user/category")
@Api(tags = "C端分类相关接口")
@Slf4j
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    /**
     * 查询分类列表
     * @param type 类型: 1菜品分类 2套餐分类
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("查询分类列表")
    public Result<List<Category>> list(Integer type) {
        log.info("查询C端分类列表，type={}", type);
        List<Category> list = categoryService.list(type);
        return Result.success(list);
    }
}