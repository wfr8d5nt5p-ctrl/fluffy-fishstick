# ConeEats

Multi-vendor food delivery & ordering platform — WeChat mini-program (C-end) · Spring Boot backend · Vue3 admin console.

一个从零开发的全栈**多商家外卖点餐系统**（C 端微信小程序 + 管理端 Vue3 + 后端 Spring Boot）。

- 支持多商家入驻，平台管理员可管理全部店铺，商家仅能管理自己店铺（`store_id` 数据隔离）
- C 端微信小程序点餐 + 管理端 Vue3 后台 + Spring Boot 后端

## Monorepo 结构

```
ConeEats/
│  ├─ backend/       Spring Boot 后端（Java 17 + Maven，多模块）
│  │  ├─ sky-common  公共组件（Result/JWT/OSS/常量）
│  │  ├─ sky-pojo    实体/DTO/VO
│  │  └─ sky-server  服务端（Controller/Service/Mapper/WebSocket）
├─ miniprogram/   微信小程序（C 端用户点餐）
└─ admin-web/     管理端前端（Vue 3 + Vite + Element Plus）
```

## 技术栈

- **后端**：Spring Boot、MyBatis、MySQL、Redis、阿里云 OSS、WebSocket、Knife4j、POI（Excel 导出）
- **C 端**：微信小程序（uni-app / 原生混合）
- **管理端**：Vue 3 + Vite + Element Plus + Vue Router + Axios

## 核心功能

- 多商家入驻与权限隔离（员工表 `store_id`，平台管理员 `null` 可见全部）
- C 端：附近好店、定位/地图选点、购物车、下单、微信支付回调、再来一单
- 商家端：菜品/分类/套餐管理（按店铺隔离）、营业状态、WebSocket 来单提醒
- 报表：营业额/用户/订单统计 + 运营数据 Excel 导出
- 缓存：菜品/套餐 Redis 缓存 + 变更清空

## 快速启动

### 1. 后端 backend

`application.yml` 中的阿里云 OSS 与 JWT、`application-dev.yml` 中的数据库/微信配置均为占位符，**请替换为你自己的密钥**（该文件已被 gitignore，不会提交）。

```bash
# 数据库：MySQL 建库 sky_take_out，导入 sky-server 下 SQL；Redis 启动
cd backend
mvn -pl sky-server -am spring-boot:run
# 服务启动于 http://localhost:8080
```

> 本地配置模板见 `backend/sky-server/src/main/resources/application-dev.example.yml`。

### 2. 管理端 admin-web

```bash
cd admin-web
npm install
npm run dev
# 打开 http://localhost:8090 （/admin 已代理到后端 8080）
```

### 3. C 端小程序 miniprogram

用微信开发者工具导入 `miniprogram` 目录，替换 `project.config.json` 中的 appid。开发者工具需勾选「不校验合法域名」，并把 `common/api.js` 的 `BASE_URL` 指向你的后端地址。

## 权限模型

| 角色 | store_id | 可见数据 |
|---|---|---|
| 平台管理员 | null | 全平台所有店铺 |
| 商家 | 店铺 id | 仅本店 |

登录 `admin/123456`（管理员）测试管理端。

## 性能压测（Locust）

C 端菜单浏览接口（`/user/dish/list`、`/user/setmeal/list`）在 50 并发、60 秒下的压测结果（`loadtest/locustfile.py`，Redis 缓存命中）：

| 指标 | 修复前 | 修复后 |
|---|---|---|
| 总请求数 | 61,078（6,180 失败） | **71,363（0 失败）** |
| 整体 QPS | ~1,043 | **~1,204** |
| 平均响应时间 | 9.8 ms | **6.2 ms** |
| P99 响应时间 | 29 ms | **17 ms** |
| 套餐列表接口 | 99.8% 失败（缓存反序列化 500） | **0% 失败** |

> 修复内容：Spring Cache 启用 Jackson 类型信息（`@class`）后，`GenericJackson2JsonRedisSerializer` 可将缓存值正确还原为 `Result<List<SetmealVO>>`，解决缓存反序列化 `ClassCastException` 导致的 500。

## 安全说明

- 阿里云 OSS `AccessKey`、微信 `AppSecret`、数据库密码等真实密钥**不作提交**，已统一替换为占位符。
- `application-dev.yml` 被 gitignore；生产/真实配置请使用环境变量或本地文件，勿提交到仓库。