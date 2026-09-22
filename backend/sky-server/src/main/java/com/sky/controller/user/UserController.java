package com.sky.controller.user;

import com.sky.context.BaseContext;
import com.sky.dto.UserLoginDTO;
import com.sky.entity.User;
import com.sky.result.Result;
import com.sky.service.UserService;
import com.sky.vo.UserLoginVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/user/user")
@Api(tags = "C端用户相关接口")
@Slf4j
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 微信登录
     * @param userLoginDTO
     * @return
     */
    @PostMapping("/login")
    @ApiOperation("微信登录")
    public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletRequest request) {
        log.info("微信用户登录：{}", userLoginDTO.getCode());
        String ip = getClientIp(request);
        UserLoginVO userLoginVO = userService.wxLogin(userLoginDTO, ip);
        return Result.success(userLoginVO);
    }

    /**
     * 更新当前用户昵称/手机号/头像
     * @param user
     * @return
     */
    @PutMapping("/info")
    @ApiOperation("完善用户资料")
    public Result updateInfo(@RequestBody User user) {
        user.setId(BaseContext.getCurrentId());
        log.info("完善用户资料：{}", user);
        userService.updateInfo(user);
        return Result.success();
    }

    /**
     * 查询当前用户资料（含最近登录IP）
     * @return
     */
    @GetMapping("/info")
    @ApiOperation("查询当前用户资料")
    public Result<User> info() {
        User user = userService.getById(BaseContext.getCurrentId());
        return Result.success(user);
    }

    /**
     * 通过微信code获取当前用户手机号并保存
     * @param body 形如 {"code":"xxx"}
     * @return 手机号
     */
    @PostMapping("/phone")
    @ApiOperation("获取微信手机号")
    public Result<String> phone(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isEmpty()) {
            return Result.error("code不能为空");
        }
        String phone = userService.getPhoneNumber(BaseContext.getCurrentId(), code);
        if (phone == null) {
            return Result.error("获取手机号失败，请在下方手动填写");
        }
        return Result.success(phone);
    }

    /**
     * 更新当前用户定位（经纬度）
     * @param body 形如 {"longitude":116.397, "latitude":39.908}
     * @return
     */
    @PutMapping("/location")
    @ApiOperation("更新用户定位")
    public Result updateLocation(@RequestBody Map<String, Object> body) {
        User user = User.builder()
                .id(BaseContext.getCurrentId())
                .longitude(body.get("longitude") == null ? null : Double.valueOf(body.get("longitude").toString()))
                .latitude(body.get("latitude") == null ? null : Double.valueOf(body.get("latitude").toString()))
                .build();
        userService.updateLocation(user);
        return Result.success();
    }

    /**
     * 从请求中解析客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}