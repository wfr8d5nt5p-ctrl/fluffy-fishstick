# ConeEats · 管理端前端（admin-web）

多商家外卖点餐系统「ConeEats」的管理后台前端。

技术栈：Vue 3 + Vite + Element Plus + Vue Router + Axios。

## 目录结构

```
admin-web/
├─ package.json         项目配置 & 依赖
├─ vite.config.js       Vite 配置（dev 代理 /admin -> localhost:8080）
├─ index.html
└─ src/
   ├─ main.js           入口
   ├─ App.vue
   ├─ router/index.js   路由 + 登录守卫
   ├─ utils/
   │  ├─ auth.js        token / storeId 本地存取
   │  └─ request.js     Axios 封装（携带 token、统一错误处理）
   ├─ api/              接口封装
   │  ├─ employee.js    登录 / 员工分页 / 启禁用
   │  ├─ category.js    分类管理
   │  ├─ dish.js        菜品管理
   │  ├─ setmeal.js     套餐管理
   │  ├─ report.js      数据统计 + 导出
   │  ├─ shop.js        营业状态
   │  └─ common.js      图片上传
   └─ views/
      ├─ login/         登录页
      ├─ layout/        主布局（侧边栏 + 顶栏 + 营业开关）
      ├─ dashboard/     工作台
      ├─ employee/      员工管理
      ├─ category/      分类管理
      ├─ dish/          菜品管理（含口味配置）
      ├─ setmeal/       套餐管理（含弹窗选菜）
      └─ report/        数据统计（汇总卡 + 每日明细 + 导出）
```

## 本地开发

```bash
# 1. 安装依赖（如已安装可跳过）
npm install

# 2. 启动开发服务（默认 http://localhost:8090）
npm run dev
```

> 开发模式下，`vite.config.js` 已配置把 `/admin` 请求代理到 `http://localhost:8080`（后端地址）。
> 若后端不在 8080 端口，请修改 `vite.config.js` 中 `server.proxy` 的 `target`。

## 访问与登录

- 打开 `http://localhost:8090`，未携带 token 会自动跳转登录页。
- 使用后端 `employee` 表的账号密码登录。
- 平台管理员（`store_id` 为 null）登录后管理全平台数据；
  商家账号登录后自动按店铺隔离，只能管理自己店铺的数据。
- 图片上传走 `/admin/common/upload`，返回 OSS 图片 URL。

## 生产构建与部署（Nginx）

```bash
npm run build
```

产物输出到 `dist/` 目录，将其拷贝到 Nginx 的 `html` 目录（或部署到静态服务）。

Nginx 需把 `/admin` 反向代理到后端，示例配置：

```nginx
server {
    listen 80;
    server_name localhost;

    # 管理端静态资源
    location / {
        root   html/admin-web;
        index  index.html;
        try_files $uri $uri/ /index.html;   # SPA 路由需回退到 index.html
    }

    # 后端接口反向代理
    location /admin {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

## 说明

- 登录令牌（JWT）存放在 `localStorage`，头字段名为 `token`，与后端 JWT 拦截器一致。
- 导出运营数据调用 `/admin/report/export`，接口返回 Excel 文件流，前端以 blob 触发下载。