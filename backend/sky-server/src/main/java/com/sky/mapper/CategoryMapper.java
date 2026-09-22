package com.sky.mapper;

import com.github.pagehelper.Page;
import com.sky.annotation.AutoFill;
import com.sky.enumeration.OperationType;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface CategoryMapper {

    /**
     * 插入数据
     * @param category
     */
    @AutoFill(OperationType.INSERT)
    @Insert("insert into category(type, name, sort, status, store_id, create_time, update_time, create_user, update_user)" +
            " VALUES" +
            " (#{type}, #{name}, #{sort}, #{status}, #{storeId}, #{createTime}, #{updateTime}, #{createUser}, #{updateUser})")
    void insert(Category category);

    /**
     * 分页查询
     * @param categoryPageQueryDTO
     * @return
     */
    Page<Category> pageQuery(CategoryPageQueryDTO categoryPageQueryDTO);

    /**
     * 根据id删除分类（按店铺隔离）
     * @param id
     * @param storeId
     */
    void deleteById(@Param("id") Long id, @Param("storeId") Long storeId);

    /**
     * 根据id修改分类
     * @param category
     */
    @AutoFill(OperationType.UPDATE)
    void update(Category category);

    /**
     * 根据类型查询分类（按店铺隔离，平台管理员 storeId 为 null 时查全部）
     * @param type
     * @param storeId
     * @return
     */
    List<Category> list(@Param("type") Integer type, @Param("storeId") Long storeId);

    /**
     * 根据商家id查询该店启用的分类，按顺序排序
     * @param storeId
     * @return
     */
    @Select("select * from category where status = 1 and store_id = #{storeId} order by sort asc")
    List<Category> listByStoreId(Long storeId);
}
