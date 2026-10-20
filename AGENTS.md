# LIMS Project - Agents Guide

**项目名称**：实验室信息管理系统 (Laboratory Information Management System)  
**技术架构**：前后端分离  
- 前端：`frontend/` — Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router 4  
- 后端：`backend/` — Spring Boot 3 + Java 17 + Maven + MyBatis-Plus  
- 数据库：MySQL 8 (InnoDB, utf8mb4)  
- 权限模型：RBAC (用户 / 角色 / 权限 / 菜单 / 部门 / 数据权限)  

**本文件是 Codex 代理的核心行为指南**。任何代码生成、重构、建议都必须遵循以下规则。

---

## 1. 总体原则

- **不偏离技术栈**：前端只用 Vue3 生态，后端只用 Spring Boot 3 生态。引入新依赖必须说明用途并获得确认。
- **安全第一**：所有权限校验、数据校验、业务安全逻辑必须在后端实现，前端仅做体验层展示控制。
- **改表必同步**：修改数据库表结构必须同步更新：SQL脚本 → Entity → Mapper → DTO/VO → 前端接口与页面。
- **可构建原则**：生成的代码必须保证 `mvn compile`（后端）和 `npm run build`（前端）通过，不允许给出无法编译的示意代码。
- **敏感信息零提交**：不将密码、密钥、Token、私钥、`application-prod.yml` 真实配置提交到仓库。
- 数据库是MySql8.0，用户名为root，登录密码为：11111111,
- 数据库脚本文件为：lims.sql

---

## 2. 目录结构约定

lims/

├── frontend/                # Vue3 前端工程

│   ├── src/

│   │   ├── api/             # 接口请求封装 (Axios)

│   │   ├── views/           # 页面组件

│   │   ├── components/      # 通用/业务组件

│   │   ├── stores/          # Pinia 状态管理

│   │   ├── router/          # 路由配置 (含动态路由生成)

│   │   ├── utils/           # 工具函数

│   │   ├── types/           # TypeScript 类型定义

│   │   ├── layouts/         # 布局组件

│   │   ├── directives/      # 自定义指令 (如 v-permission)

│   │   ├── App.vue

│   │   └── main.ts

│   ├── .env.development

│   ├── .env.production

│   ├── vite.config.ts

│   ├── tsconfig.json

│   └── package.json

│

├── backend/                 # Spring Boot 后端工程

│   ├── src/main/java/com/lims/

│   │   ├── controller/      # 控制器 (参数校验 + 调用Service + 统一响应)

│   │   ├── service/         # 业务逻辑层

│   │   ├── mapper/          # MyBatis-Plus Mapper 接口

│   │   ├── entity/          # 数据库实体类

│   │   ├── dto/             # 请求数据传输对象 (含 JSR-303 校验)

│   │   ├── vo/              # 响应视图对象

│   │   ├── config/          # 框架配置 (Security, MyBatis-Plus, CORS, Jackson)

│   │   ├── security/        # JWT 认证 + RBAC 授权核心

│   │   ├── common/          # 公共类 (R 统一响应, 异常处理, 常量)

│   │   └── LimsApplication.java

│   ├── src/main/resources/

│   │   ├── application.yml

│   │   ├── application-dev.yml

│   │   └── mapper/          # MyBatis XML 映射文件

│   └── pom.xml

│

├── db/                      # 数据库脚本

│   ├── init/                # 建库建表初始化脚本

│   └── migrations/          # 增量迁移脚本 (V1__xxx.sql, V2__xxx.sql)

│

├── docs/                    # 项目文档

├── AGENTS.md                # 本文件

└── README.md

---

## 3. 后端开发规范 (backend/)

### 3.1 技术约束
- JDK 17+, Spring Boot 3.x, Maven 3.8+, MyBatis-Plus 3.5.x+

- 认证：JWT (access_token + refresh_token)

- 授权：Spring Security + RBAC (基于权限标识符)

- 统一响应格式：

  json

  { "code": 0, "msg": "success", "data": {} }

  约定：`code=0` 成功，`code≠0` 业务失败，`401` 未认证，`403` 无权限，`400` 参数错误，`500` 系统异常。

  ### 3.2 分层职责
  | 层         | 职责                                    | 禁止                       |
  | ---------- | --------------------------------------- | -------------------------- |
  | Controller | 参数校验、调用Service、返回统一响应     | 写业务逻辑、直接操作数据库 |
  | Service    | 业务逻辑编排、事务管理 (@Transactional) | 直接暴露 Repository 细节   |
  | Mapper     | 继承 BaseMapper，定义数据库操作方法     | 写业务逻辑、字符串拼接 SQL |
  | Entity     | 表映射，使用 MyBatis-Plus 注解          | 包含业务方法               |
  | DTO        | 接收前端请求参数，使用 JSR-303 注解校验 | 包含复杂业务逻辑           |
  | VO         | 返回给前端的数据结构，脱敏处理          | 包含 password/salt/secret  |

  ### 3.3 MyBatis-Plus 编码规范
  - 优先使用 `LambdaQueryWrapper` / `LambdaUpdateWrapper`，禁止字符串拼接 SQL。
  - 分页使用 `Page<T>` + `MybatisPlusInterceptor` (`PaginationInnerInterceptor`)。
  - 逻辑删除字段统一为 `deleted`（`0` 有效，`1` 删除）。
  - 审计字段（所有表必须包含）：
  - `created_by` VARCHAR(64)
  - `created_at` DATETIME
  - `updated_by` VARCHAR(64)
  - `updated_at` DATETIME
  - 主键策略：`BIGINT AUTO_INCREMENT`，字段名 `id`。

  ### 3.4 RBAC 权限设计
  **数据库表结构（必须包含）**：
  - `sys_user` — 用户表
  - `sys_role` — 角色表
  - `sys_permission` — 权限标识表 (如 `sample:create`, `report:export`)
  - `sys_menu` — 菜单表 (前端路由 + 按钮)
  - `sys_dept` — 部门表 (数据权限基础)
  - `sys_user_role` — 用户角色关联表
  - `sys_role_permission` — 角色权限关联表
  - `sys_role_menu` — 角色菜单关联表

  **接口鉴权流程**：
  1. 登录：`POST /api/auth/login` → 返回 JWT Token
  2. 获取当前用户信息：`GET /api/auth/me` → 返回用户信息 + 角色列表 + 权限标识集合 + 菜单树
  3. 接口权限校验：后端使用 `@PreAuthorize("hasAuthority('permission:code')")` 注解或在 Security 配置中统一校验
  4. 数据权限：普通用户只能看到自己所在部门及其下属部门的数据，超级管理员可查看全部

  **安全红线**：
  - ❌ 不允许前端传递 `role` 或 `permission` 参数来决定后端是否放行
  - ❌ 不允许前端判断“是否为管理员”来控制数据可见性
  - ✅ 前端只做菜单/按钮显隐控制，后端必须再次校验权限

  ### 3.5 异常处理
  - 使用全局 `@RestControllerAdvice` 捕获异常
  - 业务异常使用自定义 `BusinessException(code, message)`，由全局处理器转换为统一响应格式
  - 参数校验异常（`MethodArgumentNotValidException`）自动返回 `400` 及具体字段错误信息

  ---

  ## 4. 前端开发规范 (frontend/)

  ### 4.1 技术约束
  - Vue 3 Composition API (`<script setup lang="ts">`)
  - TypeScript strict 模式
  - Element Plus UI 组件库
  - Pinia 状态管理
  - Vue Router 4 路由管理
  - Axios HTTP 客户端 (封装统一拦截器处理 Token 刷新、401/403 跳转)

  ### 4.2 编码规范
  - **类型安全**：禁止使用 `any`，所有 API 响应数据必须定义 TypeScript 类型/接口。
  - **组件通信**：优先使用 Props + Emits，跨层级状态使用 Pinia。
  - **状态管理**：业务状态放入 Pinia，避免滥用 `localStorage` 存储业务数据。
  - **表单校验**：前端做用户体验层的即时校验，后端做安全层面的最终校验。
  - **权限指令**：按钮级别权限控制使用自定义指令 `v-permission="'permission:code'"` 或工具函数 `hasPermission('permission:code')`。

  ### 4.3 路由与权限集成
  - **公开路由**：`/login`、`/403`、`/404`
  - **动态路由**：登录成功后，根据 `GET /api/auth/me` 返回的菜单树动态生成路由并添加到路由器
  - **路由守卫**：
  - 无 Token → 重定向 `/login`
  - 有 Token 但无任何菜单权限 → 显示 403 页面
  - 已登录访问 `/login` → 重定向首页

  ### 4.4 前端禁止项
  - ❌ 在前端代码中硬编码后端 API 地址（必须使用 `import.meta.env.VITE_API_BASE_URL`）
  - ❌ 将 Token 放在 URL 参数中传输
  - ❌ 在前端存储或比较用户密码
  - ❌ 在生产代码中使用 `console.log`（应使用日志服务或 `console.warn`/`console.error`）
  - ❌ 依赖前端角色判断来做安全决策（只能做 UI 展示控制）

  ---

  ## 5. 数据库规范

  - **引擎**：InnoDB
  - **字符集**：`utf8mb4`，排序规则 `utf8mb4_general_ci`
  - **命名**：表名和字段名均使用小写字母和下划线（`snake_case`）
  - **主键**：`id BIGINT NOT NULL AUTO_INCREMENT`
  - **时间字段**：使用 `DATETIME` 类型
  - **枚举字段**：不使用 MySQL `ENUM` 类型，使用 `TINYINT` 或 `VARCHAR` + 字典表解释含义
  - **索引**：外键字段、频繁查询的条件字段、排序字段必须建立索引
  - **修改流程**：
  1. 编写增量迁移 SQL 至 `db/migrations/` 目录
  2. 更新对应的 Entity 类
  3. 更新 Mapper/DTO/VO
  4. 更新前端接口和页面
  5. 在代码注释或 Commit Message 中说明影响范围

  ---

  ## 6. 构建与质量门禁

  在执行任何代码变更后，Codex 必须确保以下命令可通过：

  **前端 (frontend/)**

  bash

  cd frontend

  npm install

  npm run build          # 生产构建

  npx vue-tsc --noEmit   # 类型检查

  npm run lint           # ESLint 检查

  **后端 (backend/)**

  bash

  cd backend

  mvn clean compile -q           # 编译

  mvn clean package -DskipTests  # 打包

  mvn spring-boot:run            # 启动验证（可选）

  ## 7. Git 工作流

  - **分支策略**：
    - `main` / `master`：受保护分支，只接受 Pull Request 合并
    - `develop`：日常开发分支
    - `feature/xxx`：功能开发分支，从 `develop` 切出
    - `fix/xxx`：Bug 修复分支
  - **Commit Message 规范**（Conventional Commits）：
    - `feat:` 新功能
    - `fix:` 修复 Bug
    - `refactor:` 重构
    - `chore:` 构建/配置/依赖变更
    - `docs:` 文档
    - `test:` 测试
  - **禁止操作**：
    - `git push --force` 到 `main` / `master`
    - `git reset --hard` 后强制推送
    - 提交 `node_modules/`、`target/`、`.env` 文件、IDE 配置文件

  ---

  ## 8. LIMS 领域术语对照

  | 中文     | 英文 (代码中使用)  | 说明                   |
  | -------- | ------------------ | ---------------------- |
  | 样品     | Sample             | 被检测的对象           |
  | 采样     | Sampling           | 样品采集过程           |
  | 检测任务 | TestTask           | 一次检测工作的任务单元 |
  | 检验项目 | TestItem           | 具体的检测指标         |
  | 仪器设备 | Instrument         | 实验设备管理           |
  | 检测方法 | Method             | 检测所依据的方法标准   |
  | 报告     | Report             | 检测结果报告           |
  | 客户     | Customer           | 委托方信息             |
  | 委托合同 | Order / Commission | 检测委托单             |
  | 字典     | Dictionary         | 数据字典（下拉选项等） |
  | 操作日志 | OperationLog       | 用户操作记录           |
  | 审计日志 | AuditLog           | 数据变更审计记录       |

  ---

  ## 9. Codex 行为约束清单

  当 Codex 代理在本项目中工作时：

  1. **修改后端时**：
     - 保持 Maven 项目结构完整性
     - 新接口必须标注所属 LIMS 模块（如 SampleController、ReportController）
     - 涉及权限变更时，同步更新数据库权限数据和前端菜单配置

  2. **修改前端时**：
     - 确保 `npm run build` 和 `vue-tsc --noEmit` 通过
     - 新增页面必须补充路由配置、菜单项和权限标识
     - 新增 API 调用必须定义对应的 TypeScript 类型

  3. **修改权限系统时**：
     - 必须同时处理三个层面：后端接口鉴权、前端菜单/按钮显隐、数据库权限种子数据
     - 不允许出现“前端隐藏即安全”的逻辑漏洞

  4. **修改数据库时**：
     - 必须输出对应的增量迁移 SQL
     - 必须说明影响的实体类、DTO、VO 和前端页面

  5. **生成代码时**：
     - 不允许只生成“代码片段”，必须生成完整的可编译文件
     - 不允许在代码中留下 TODO/FIXME 占位符而不实现（除非明确标记为后续迭代）