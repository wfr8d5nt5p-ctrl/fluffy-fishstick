package com.sky.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.sky.constant.JwtClaimsConstant;
import com.sky.constant.MessageConstant;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.exception.LoginFailedException;
import com.sky.mapper.UserMapper;
import com.sky.properties.JwtProperties;
import com.sky.properties.WeChatProperties;
import com.sky.service.UserService;
import com.sky.utils.HttpClientUtil;
import com.sky.utils.JwtUtil;
import com.sky.vo.UserLoginVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    //微信服务接口地址
    public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";
    //获取access_token
    public static final String WX_TOKEN = "https://api.weixin.qq.com/cgi-bin/token";
    //获取手机号
    public static final String WX_PHONE = "https://api.weixin.qq.com/wxa/business/getuserphonenumber";

    @Autowired
    private WeChatProperties weChatProperties;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtProperties jwtProperties;

    /**
     * 微信登录
     * @param userLoginDTO
     * @param ip 用户来源IP
     * @return
     */
    @Override
    public UserLoginVO wxLogin(UserLoginDTO userLoginDTO, String ip) {
        //1、调用微信服务接口，用临时凭证code获取用户openid
        String openid = getOpenid(userLoginDTO.getCode());

        //2、判断openid是否为空，如果为空表示登录失败，抛出业务异常
        if (openid == null) {
            throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
        }

        //3、判断当前用户是否为新用户，如果是新用户就自动完成注册
        User user = userMapper.getByOpenid(openid);
        if (user == null) {
            user = User.builder()
                    .openid(openid)
                    .createTime(LocalDateTime.now())
                    .build();
            userMapper.insert(user);
            //重新查询，拿到自增id
            user = userMapper.getByOpenid(openid);
        }

        //3.1、记录用户来源IP
        if (ip != null && !ip.isEmpty()) {
            userMapper.updateIp(user.getId(), ip);
        }

        //4、为该用户生成jwt令牌
        Map<String, Object> claims = new HashMap<>();
        claims.put(JwtClaimsConstant.USER_ID, user.getId());
        String token = JwtUtil.createJWT(
                jwtProperties.getUserSecretKey(),
                jwtProperties.getUserTtl(),
                claims);

        //5、封装返回数据
        UserLoginVO userLoginVO = UserLoginVO.builder()
                .id(user.getId())
                .openid(user.getOpenid())
                .token(token)
                .name(user.getName())
                .phone(user.getPhone())
                .avatar(user.getAvatar())
                .build();

        return userLoginVO;
    }

    @Override
    public void updateInfo(User user) {
        userMapper.updateInfo(user);
    }

    @Override
    public User getById(Long id) {
        return userMapper.getById(id);
    }

    @Override
    public void updateLocation(User user) {
        userMapper.updateLocation(user);
    }

    /**
     * 通过微信code获取并绑定手机号
     */
    @Override
    public String getPhoneNumber(Long userId, String code) {
        String accessToken = getAccessToken();
        if (accessToken == null) {
            log.warn("获取微信access_token失败");
            return null;
        }
        try {
            String json = postJson(WX_PHONE + "?access_token=" + accessToken,
                    "{\"code\":\"" + code + "\"}");
            if (json == null || json.isEmpty()) {
                return null;
            }
            JSONObject obj = JSONObject.parseObject(json);
            Integer errcode = obj.getInteger("errcode");
            if (errcode == null || errcode != 0) {
                log.warn("获取手机号失败：{}", json);
                return null;
            }
            JSONObject phoneInfo = obj.getJSONObject("phone_info");
            String phone = phoneInfo == null ? null : phoneInfo.getString("phoneNumber");
            if (phone != null && !phone.isEmpty()) {
                userMapper.updatePhone(userId, phone);
            }
            return phone;
        } catch (Exception e) {
            log.warn("获取手机号异常", e);
            return null;
        }
    }

    private String getAccessToken() {
        Map<String, String> map = new HashMap<>();
        map.put("grant_type", "client_credential");
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        String json = HttpClientUtil.doGet(WX_TOKEN, map);
        if (json == null || json.isEmpty()) {
            return null;
        }
        return JSONObject.parseObject(json).getString("access_token");
    }

    private String postJson(String urlStr, String body) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.getBytes(StandardCharsets.UTF_8));
            }
            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("调用微信接口异常", e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * 调用微信接口，获取用户openid
     * @param code 前端获取到的临时凭证
     * @return
     */
    private String getOpenid(String code) {
        Map<String, String> map = new HashMap<>();
        map.put("appid", weChatProperties.getAppid());
        map.put("secret", weChatProperties.getSecret());
        map.put("js_code", code);
        map.put("grant_type", "authorization_code");
        String json = HttpClientUtil.doGet(WX_LOGIN, map);
        if (json == null || json.isEmpty()) {
            return null;
        }
        JSONObject jsonObject = JSONObject.parseObject(json);
        return jsonObject.getString("openid");
    }
}