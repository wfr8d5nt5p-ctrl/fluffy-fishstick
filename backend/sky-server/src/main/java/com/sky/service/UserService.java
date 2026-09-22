package com.sky.service;

import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.vo.UserLoginVO;

public interface UserService {

    /**
     * 微信登录，并记录用户IP
     * @param userLoginDTO
     * @param ip 用户来源IP
     * @return
     */
    UserLoginVO wxLogin(UserLoginDTO userLoginDTO, String ip);

    /**
     * 更新当前用户昵称/手机号/头像
     * @param user
     */
    void updateInfo(User user);

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    User getById(Long id);

    /**
     * 通过微信code获取并绑定手机号
     * @param userId 当前用户id
     * @param code 前端 getPhoneNumber 返回的 code
     * @return 手机号，失败返回 null
     */
    String getPhoneNumber(Long userId, String code);

    /**
     * 更新当前用户定位（经纬度）
     * @param user
     */
    void updateLocation(User user);
}