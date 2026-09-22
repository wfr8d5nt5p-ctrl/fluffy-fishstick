package com.sky.mapper;

import com.sky.entity.Store;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StoreMapper {

    /**
     * 查询营业中的商家列表，支持按名称/分类/地址模糊搜索
     * @param keyword 关键词，可空
     * @param category 主营分类，可空
     * @return
     */
    @Select("<script>" +
            "select * from store where status = 1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " and (name like concat('%',#{keyword},'%') or category like concat('%',#{keyword},'%') or address like concat('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='category != null and category != \"\"'>" +
            " and category = #{category}" +
            "</if>" +
            " order by id asc" +
            "</script>")
    List<Store> list(@Param("keyword") String keyword, @Param("category") String category);

    /**
     * 根据id查询商家
     * @param id
     * @return
     */
    @Select("select * from store where id = #{id}")
    Store getById(Long id);
}