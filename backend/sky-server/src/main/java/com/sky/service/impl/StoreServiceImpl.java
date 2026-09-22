package com.sky.service.impl;

import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.entity.Store;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import com.sky.mapper.StoreMapper;
import com.sky.service.StoreService;
import com.sky.vo.StoreMenuVO;
import com.sky.vo.StoreVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class StoreServiceImpl implements StoreService {

    @Autowired
    private StoreMapper storeMapper;
    @Autowired
    private CategoryMapper categoryMapper;
    @Autowired
    private DishMapper dishMapper;
    @Autowired
    private SetmealMapper setmealMapper;
    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Override
    public List<StoreVO> listStores(String keyword, String category) {
        List<Store> list = storeMapper.list(keyword, category);
        if (list == null) {
            return new ArrayList<>();
        }
        return list.stream().map(s -> {
            StoreVO vo = new StoreVO();
            BeanUtils.copyProperties(s, vo);
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public StoreMenuVO getStoreMenu(Long storeId) {
        Store store = storeMapper.getById(storeId);
        if (store == null) {
            return null;
        }
        StoreVO storeVO = new StoreVO();
        BeanUtils.copyProperties(store, storeVO);

        List<StoreMenuVO.CategoryMenu> menus = new ArrayList<>();
        List<Category> categories = categoryMapper.listByStoreId(storeId);
        if (categories != null) {
            for (Category c : categories) {
                StoreMenuVO.CategoryMenu cm = new StoreMenuVO.CategoryMenu();
                cm.setId(c.getId());
                cm.setName(c.getName());

                // 该分类下在售菜品（含口味）
                List<Dish> dishList = dishMapper.listByCategoryId(c.getId());
                List<StoreMenuVO.MenuItem> dishes = new ArrayList<>();
                if (dishList != null) {
                    for (Dish d : dishList) {
                        StoreMenuVO.MenuItem mi = StoreMenuVO.MenuItem.builder()
                                .id(d.getId()).type(1).name(d.getName()).price(d.getPrice())
                                .image(d.getImage()).description(d.getDescription()).status(d.getStatus())
                                .build();
                        List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());
                        if (flavors != null) {
                            List<String> fl = new ArrayList<>();
                            for (DishFlavor f : flavors) {
                                fl.add(f.getName() + "：" + f.getValue());
                            }
                            mi.setFlavors(fl);
                        }
                        dishes.add(mi);
                    }
                }
                cm.setDishes(dishes);

                // 该分类下在售套餐
                List<Setmeal> setmealList = setmealMapper.listByCategoryId(c.getId());
                List<StoreMenuVO.MenuItem> setmeals = new ArrayList<>();
                if (setmealList != null) {
                    for (Setmeal s : setmealList) {
                        setmeals.add(StoreMenuVO.MenuItem.builder()
                                .id(s.getId()).type(2).name(s.getName()).price(s.getPrice())
                                .image(s.getImage()).description(s.getDescription()).status(s.getStatus())
                                .build());
                    }
                }
                cm.setSetmeals(setmeals);

                if (!dishes.isEmpty() || !setmeals.isEmpty()) {
                    menus.add(cm);
                }
            }
        }
        return StoreMenuVO.builder().store(storeVO).menus(menus).build();
    }
}