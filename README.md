# LIMS 实验室信息管理系统

南通理工学院 软件专业综合实践 —— VibeCoding 辅助 LIMS 项目设计（AI 部分）。

基于 Vue 3 + Spring Boot 3 的前后端分离实验室信息管理系统，覆盖样品登记、分解排样、
任务安排、检验数据录入、报告签发、报告下载与数据导出等完整业务流程。

## 技术栈

| 层 | 技术 |
| --- | --- |
| 后端 | Spring Boot 3.4、Java 17、Maven、MyBatis-Plus、Spring JDBC、JWT、Apache POI |
| 前端 | Vue 3、TypeScript、Vite、Element Plus、Pinia、Vue Router 4、Axios |
| 数据库 | MySQL 8（InnoDB / utf8mb4） |

## 目录结构

```text
LIMS/
├── backend/              # Spring Boot 后端
│   └── src/main/java/com/lims/
│       ├── controller/   # 接口层
│       ├── service/      # 业务逻辑
│       ├── security/     # JWT 认证
│       ├── common/       # 统一响应与异常
│       └── config/       # 框架配置
├── frontend/             # Vue 3 前端
│   └── src/
│       ├── api/          # 接口封装
│       ├── views/        # 页面
│       ├── components/   # 公共组件
│       ├── router/       # 路由
│       └── utils/        # 工具
├── db/                   # 数据库脚本目录
├── docs/                 # 实训文档
├── lims.sql              # 数据库结构与示例数据
└── AGENTS.md             # 项目开发规范（AI 协作约束）
```

## 功能模块

| 菜单 | 功能 |
| --- | --- |
| 服务信息 | 中心文件资料（导入模板下载）、项目库查询、在检项目查询、历史项目查询 |
| 任务管理 | 监抽任务管理、监抽执行查询、监抽抽样查询 |
| 样品管理 | 导入采样单、样品登记确认、报告下载、登记查询、导出数据 |
| 分解排样 | 单个样品分解、批量样品分解、任务安排、批量安排 |
| 项目检验 | 任务查询、数据录入 |
| 审核签发 | 报告签发、检验汇总查询 |
| 基础数据 | 用户信息、方法检验员、检验岗位、项目库维护、中心资料上传 |

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- MySQL 8.0

## 启动步骤

### 1. 初始化数据库

```sql
CREATE DATABASE lims DEFAULT CHARACTER SET utf8mb4;
```

然后在 `lims` 库中导入根目录的 `lims.sql`。

### 2. 配置数据库连接

后端默认使用 `dev` profile，数据库口令通过环境变量或本地配置文件提供（不提交到仓库）：

```powershell
# 方式一：环境变量
$env:DB_PASSWORD = '你的数据库密码'

# 方式二：本地配置文件（已被 .gitignore 忽略）
Copy-Item backend\src\main\resources\application-local.yml.example backend\src\main\resources\application-local.yml
# 编辑 application-local.yml，填入 password
```

可选环境变量：`DB_URL`、`DB_USERNAME`、`DB_PASSWORD`。

### 3. 启动后端（端口 8081，上下文路径 `/api`）

```powershell
cd backend
mvn spring-boot:run
```

### 4. 启动前端（端口 5173）

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问 <http://localhost:5173>，测试账号：`nj001 / nj001`。

### 5. 构建校验

```powershell
cd backend; mvn clean package -DskipTests
cd frontend; npm run build
```

## 分支管理

遵循《Git 代码版本管理操作说明书（实训专用）》：

| 分支 | 用途 |
| --- | --- |
| `main` | 稳定版本，仅通过合并更新 |
| `develop` | 日常开发主分支 |
| `feature/功能名-姓名` | 个人功能分支 |

开发流程：从 `develop` 拉取功能分支 → 开发自测 → 合并回 `develop` → 联调通过后合并到 `main`。

## 提交规范

提交信息格式：`功能：完成 xxx-姓名`（修复类使用 `fix: 修复 xxx-姓名`），要求说明开发内容并可追溯到人。

## 文档

- `docs/AI学习总结报告-软件2301-2310501102-孙熙隆.docx`：AI 辅助开发实训报告
- `AGENTS.md`：项目开发规范与安全红线
- `db/migrations/`：增量数据库脚本

## 许可

本项目采用 [MIT License](LICENSE) 开源许可。
