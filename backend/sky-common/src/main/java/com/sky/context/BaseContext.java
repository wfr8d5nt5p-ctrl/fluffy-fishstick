package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    // 当前登录员工/用户所属店铺id；管理端平台管理员为 null
    public static ThreadLocal<Long> storeIdThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void setCurrentStoreId(Long storeId) {
        storeIdThreadLocal.set(storeId);
    }

    public static Long getCurrentStoreId() {
        return storeIdThreadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
        storeIdThreadLocal.remove();
    }

}
