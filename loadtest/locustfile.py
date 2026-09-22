# -*- coding: utf-8 -*-
"""Locust 压测：C端菜品列表接口 /user/dish/list（走 Redis 缓存）"""
import time
import jwt
from locust import HttpUser, task, between
import random

SECRET = "itheima"
TOKEN_NAME = "authentication"
CATEGORY_IDS = [23, 24, 26, 27, 28, 29, 30, 31, 32, 33]  # 真实存在的分类

def make_token(user_id=1, ttl_ms=7200000):
    now = int(time.time() * 1000)
    payload = {"userId": user_id, "exp": now + ttl_ms, "iat": now}
    return jwt.encode(payload, SECRET, algorithm="HS256")

TOKEN = make_token()


class MenuBrowseUser(HttpUser):
    """模拟 C 端用户浏览菜单：随机分类查询菜品列表（命中 Redis 缓存）"""
    wait_time = between(0.01, 0.05)  # 用户思考间隔 10-50ms，模拟高并发点击

    @task(9)
    def dish_list(self):
        cid = random.choice(CATEGORY_IDS)
        with self.client.get(
            "/user/dish/list",
            params={"categoryId": cid},
            headers={TOKEN_NAME: TOKEN},
            name="/user/dish/list?categoryId={cid}",  # 聚合统计
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
            else:
                try:
                    code = resp.json().get("code")
                    if code != 1:
                        resp.failure(f"业务码 {code}")
                except Exception:
                    resp.failure("非JSON响应")

    @task(1)
    def setmeal_list(self):
        """少量套餐浏览，贴近真实菜单浏览比例"""
        cid = random.choice(CATEGORY_IDS)
        with self.client.get(
            "/user/setmeal/list",
            params={"categoryId": cid},
            headers={TOKEN_NAME: TOKEN},
            name="/user/setmeal/list?categoryId={cid}",
            catch_response=True,
        ) as resp:
            if resp.status_code != 200:
                resp.failure(f"HTTP {resp.status_code}")
