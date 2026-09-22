package com.sky.mapper;

import com.sky.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    /**
     * 根据openid查询用户
     * @param openid
     * @return
     */
    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    @Select("select * from user where id = #{id}")
    User getById(Long id);

    /**
     * 插入数据
     * @param user
     */
    @Insert("insert into user (openid, name, phone, sex, id_number, avatar, create_time) " +
            "values (#{openid}, #{name}, #{phone}, #{sex}, #{idNumber}, #{avatar}, #{createTime})")
    void insert(User user);

    /**
     * 更新用户昵称/手机号/头像
     * @param user
     */
    @Update("update user set name = #{name}, phone = #{phone}, avatar = #{avatar} where id = #{id}")
    void updateInfo(User user);

    /**
     * 记录最近登录IP
     * @param id
     * @param ip
     */
    @Update("update user set last_login_ip = #{ip} where id = #{id}")
    void updateIp(@Param("id") Long id, @Param("ip") String ip);

    /**
     * 更新用户手机号
     * @param id
     * @param phone
     */
    @Update("update user set phone = #{phone} where id = #{id}")
    void updatePhone(@Param("id") Long id, @Param("phone") String phone);

    /**
     * 更新用户定位（经纬度）
     * @param user
     */
    @Update("update user set longitude = #{longitude}, latitude = #{latitude} where id = #{id}")
    void updateLocation(User user);
}