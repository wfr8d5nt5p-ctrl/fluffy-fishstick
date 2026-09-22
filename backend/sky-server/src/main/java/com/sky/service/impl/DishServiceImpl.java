package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealDishMapper;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜品业务层
 */
@Service
@Slf4j
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        dishPageQueryDTO.setStoreId(BaseContext.getCurrentStoreId());
        PageHelper.startPage(dishPageQueryDTO.getPage(), dishPageQueryDTO.getPageSize());
        // 下一条sql进行分页，自动拼接limit关键字
        Page<DishVO> page = dishMapper.pageQuery(dishPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 菜品批量删除
     * @param ids
     */
    public void deleteBatch(List<Long> ids) {
        // 判断菜品是否在售中，在售中的菜品不能删除
        Integer count = dishMapper.countByStatus(ids, StatusConstant.ENABLE);
        if (count != null && count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
        }

        // 判断菜品是否被套餐关联，被关联的菜品不能删除
        count = setmealDishMapper.countByDishIds(ids);
        if (count != null && count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }

        // 删除菜品
        dishMapper.deleteByIds(ids, BaseContext.getCurrentStoreId());
        // 删除菜品关联的口味数据
        dishFlavorMapper.deleteByDishIds(ids);
    }

    /**
     * 根据id查询菜品及口味（编辑回显）
     * @param id
     * @return
     */
    public DishVO getByIdWithFlavor(Long id) {
        // 1. 查询菜品基础信息
        Dish dish = dishMapper.getById(id);
        // 2. 查询该菜品的口味
        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(id);
        // 3. 组装成DishVO
        DishVO dishVO = new DishVO();
        BeanUtils.copyProperties(dish, dishVO);
        dishVO.setFlavors(flavors);
        return dishVO;
    }

    /**
     * 修改菜品
     * @param dishDTO
     */
    public void updateWithFlavor(DishDTO dishDTO) {
        // 1. DTO转实体
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setStoreId(BaseContext.getCurrentStoreId());
        // 2. 更新菜品主表（公共字段update_time/update_user由切面自动填充）
        dishMapper.update(dish);
        // 3. 删除该菜品原有口味
        dishFlavorMapper.deleteByDishId(dish.getId());
        // 4. 写入新的口味，并补充dishId
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(dish.getId()));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 新增菜品
     * @param dishDTO
     */
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        Dish dish = new Dish();
        BeanUtils.copyProperties(dishDTO, dish);
        dish.setStoreId(BaseContext.getCurrentStoreId());
        dishMapper.save(dish);
        Long dishId = dish.getId();
        List<DishFlavor> flavors = dishDTO.getFlavors();
        if (flavors != null && flavors.size() > 0) {
            flavors.forEach(flavor -> flavor.setDishId(dishId));
            dishFlavorMapper.insertBatch(flavors);
        }
    }

    /**
     * 菜品起售停售
     * @param status
     * @param id
     */
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder().id(id).status(status).storeId(BaseContext.getCurrentStoreId()).build();
        dishMapper.update(dish);
    }

    /**
     * 管理端：根据分类id查询该分类全部菜品（含口味，新增套餐选择菜品弹窗使用）
     * @param categoryId
     * @return
     */
    public List<DishVO> listWithFlavorByCategory(Long categoryId) {
        List<Dish> dishList = dishMapper.listByCategoryAll(categoryId, BaseContext.getCurrentStoreId());
        List<DishVO> dishVOList = new ArrayList<>();
        if (dishList != null && dishList.size() > 0) {
            for (Dish dish : dishList) {
                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(dish, dishVO);
                List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish.getId());
                dishVO.setFlavors(flavors);
                dishVOList.add(dishVO);
            }
        }
        return dishVOList;
    }

    /**
     * 根据分类id查询在售菜品（含口味），带Redis缓存
     * 缓存key为 categoryId，缓存名dish，Spring Cache自动完成"查缓存->命中返回/未命中查库->写缓存"
     * @param categoryId
     * @return
     */
    @Cacheable(cacheNames = "dish", key = "#categoryId")
    public List<DishVO> listByCategory(Long categoryId) {
        // 这里只写查数据库的逻辑，缓存由Spring Cache注解自动处理
        List<Dish> dishList = dishMapper.listByCategoryId(categoryId);
        List<DishVO> dishVOList = new ArrayList<>();
        if (dishList != null && dishList.size() > 0) {
            for (Dish dish : dishList) {
                DishVO dishVO = new DishVO();
                BeanUtils.copyProperties(dish, dishVO);
                List<DishFlavor> flavors = dishFlavorMapper.getByDishId(dish.getId());
                dishVO.setFlavors(flavors);
                dishVOList.add(dishVO);
            }
        }
        return dishVOList;
    }
}