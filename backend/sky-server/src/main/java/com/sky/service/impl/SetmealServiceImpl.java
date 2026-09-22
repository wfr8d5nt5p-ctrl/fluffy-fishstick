package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.SetmealDTO;
import com.sky.dto.SetmealPageQueryDTO;
import com.sky.entity.Setmeal;
import com.sky.entity.SetmealDish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.SetmealDishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.result.PageResult;
import com.sky.service.SetmealService;
import com.sky.vo.SetmealVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 套餐业务层
 */
@Service
@Slf4j
public class SetmealServiceImpl implements SetmealService {

    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private SetmealDishMapper setmealDishMapper;

    /**
     * 新增套餐
     * @param setmealDTO
     */
    @Transactional
    public void saveWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStoreId(BaseContext.getCurrentStoreId());
        // 新增套餐主表，返回自增主键
        setmealMapper.save(setmeal);
        Long setmealId = setmeal.getId();
        // 新增套餐与菜品的关联关系，补充setmealId
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(sd -> sd.setSetmealId(setmealId));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐分页查询
     * @param setmealPageQueryDTO
     * @return
     */
    public PageResult pageQuery(SetmealPageQueryDTO setmealPageQueryDTO) {
        setmealPageQueryDTO.setStoreId(BaseContext.getCurrentStoreId());
        PageHelper.startPage(setmealPageQueryDTO.getPage(), setmealPageQueryDTO.getPageSize());
        Page<SetmealVO> page = setmealMapper.pageQuery(setmealPageQueryDTO);
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 批量删除套餐
     * @param ids
     */
    @Transactional
    public void deleteBatch(List<Long> ids) {
        // 判断套餐是否在售中，在售中的套餐不能删除
        Integer count = setmealMapper.countByStatus(ids, StatusConstant.ENABLE);
        if (count != null && count > 0) {
            throw new DeletionNotAllowedException(MessageConstant.SETMEAL_ON_SALE);
        }
        // 删除套餐主表
        setmealMapper.deleteByIds(ids, BaseContext.getCurrentStoreId());
        // 删除套餐与菜品的关联关系
        for (Long id : ids) {
            setmealDishMapper.deleteBySetmealId(id);
        }
    }

    /**
     * 根据id查询套餐及关联菜品（编辑回显）
     * @param id
     * @return
     */
    public SetmealVO getByIdWithDish(Long id) {
        Setmeal setmeal = setmealMapper.getById(id);
        List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(id);
        SetmealVO setmealVO = new SetmealVO();
        BeanUtils.copyProperties(setmeal, setmealVO);
        setmealVO.setSetmealDishes(setmealDishes);
        return setmealVO;
    }

    /**
     * 修改套餐
     * @param setmealDTO
     */
    @Transactional
    public void updateWithDish(SetmealDTO setmealDTO) {
        Setmeal setmeal = new Setmeal();
        BeanUtils.copyProperties(setmealDTO, setmeal);
        setmeal.setStoreId(BaseContext.getCurrentStoreId());
        // 修改套餐主表
        setmealMapper.update(setmeal);
        // 删除原有套餐菜品关系
        setmealDishMapper.deleteBySetmealId(setmealDTO.getId());
        // 重新写入套餐菜品关系
        List<SetmealDish> setmealDishes = setmealDTO.getSetmealDishes();
        if (setmealDishes != null && setmealDishes.size() > 0) {
            setmealDishes.forEach(sd -> sd.setSetmealId(setmealDTO.getId()));
            setmealDishMapper.insertBatch(setmealDishes);
        }
    }

    /**
     * 套餐起售停售
     * @param status
     * @param id
     */
    public void updateStatus(Integer status, Long id) {
        Setmeal setmeal = Setmeal.builder().id(id).status(status).storeId(BaseContext.getCurrentStoreId()).build();
        setmealMapper.update(setmeal);
    }

    /**
     * 用户端：根据分类id查询在售套餐（含关联菜品）
     * @param categoryId
     * @return
     */
    public List<SetmealVO> listByCategory(Long categoryId) {
        List<Setmeal> setmealList = setmealMapper.listByCategoryId(categoryId);
        List<SetmealVO> setmealVOList = new ArrayList<>();
        if (setmealList != null && setmealList.size() > 0) {
            for (Setmeal setmeal : setmealList) {
                SetmealVO setmealVO = new SetmealVO();
                BeanUtils.copyProperties(setmeal, setmealVO);
                List<SetmealDish> setmealDishes = setmealDishMapper.getBySetmealId(setmeal.getId());
                setmealVO.setSetmealDishes(setmealDishes);
                setmealVOList.add(setmealVO);
            }
        }
        return setmealVOList;
    }
}