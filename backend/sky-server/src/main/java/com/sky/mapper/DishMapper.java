package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Dish;
import com.sky.enumeration.OperationType;
import com.sky.vo.DishVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DishMapper {

    /**
     * 根据分类id查询菜品数量
     * @param categoryId
     * @return
     */
    @Select("select count(id) from dish where category_id = #{categoryId}")
    Integer countByCategoryId(Long categoryId);

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    Page<DishVO> pageQuery(DishPageQueryDTO dishPageQueryDTO);

    /**
     * 根据菜品id集合统计处于指定状态的菜品数量
     * @param ids
     * @param status
     * @return
     */
    Integer countByStatus(List<Long> ids, Integer status);

    /**
     * 根据菜品id集合批量删除菜品（按店铺隔离）
     * @param ids
     * @param storeId
     */
    void deleteByIds(@Param("ids") List<Long> ids, @Param("storeId") Long storeId);

    /**
     * 根据id查询菜品
     * @param id
     * @return
     */
    @Select("select * from dish where id = #{id}")
    Dish getById(Long id);

    /**
     * 修改菜品
     * @param dish
     */
    @AutoFill(OperationType.UPDATE)
    void update(Dish dish);

    /**
     * 根据分类id查询在售菜品列表
     * @param categoryId
     * @return
     */
    List<Dish> listByCategoryId(Long categoryId);

    /**
     * 新增菜品
     * @param dish
     */
    @AutoFill(OperationType.INSERT)
    void save(Dish dish);

    /**
     * 管理端：根据分类id查询该分类全部菜品（含停售，按店铺隔离）
     * @param categoryId
     * @param storeId
     * @return
     */
    List<Dish> listByCategoryAll(@Param("categoryId") Long categoryId, @Param("storeId") Long storeId);
}
