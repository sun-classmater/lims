# 任务

从零开发一个**食品/水产检测 LIMS（实验室信息管理系统）**，前后端分离，中文界面。
功能对标一套实际在用的旧系统（原先用桌面工具管理检测业务），因此**表名、字段名、业务编号规则都沿用现实中的习惯写法，不要改成"更规范"的名字**。

交付一个可运行、可登录、能完整跑通"任务 → 登记 → 分解 → 安排 → 检验 → 签发 → 报告"全流程的系统。

## 一、交付目标（先看这里，不要提前收工）

必须全部产出，缺一不可：

1. `db/init/lims.sql` —— 完整建库建表 + 种子数据
2. `backend/` —— Spring Boot 后端，全部接口可调通
3. `frontend/` —— Vue 3 前端，全部页面可访问
4. `docs/README.md` —— 启动步骤
5. 前后端构建命令均通过（见第九节）

分批交付时按 `DB → 后端 → 前端` 顺序，每批结束给出可验证的结果。

## 二、技术栈（锁定，不要替换）

- 前端：Vue 3.5+ / TypeScript（strict）/ Vite 6+ / Element Plus 2.9+ / Pinia / Vue Router 4 / Axios
- 后端：Spring Boot 3.4+ / Java 17 / Maven / MyBatis-Plus 3.5+ / JdbcTemplate
- 数据库：MySQL 8（InnoDB）
- 认证：JWT（jjwt 0.12+）+ Spring MVC `HandlerInterceptor`
- Excel/Word：Apache POI 5.2+（只处理 `.xls`，即 HSSF）
- **不引入 Spring Security**（本期用手写拦截器，见第五节）

## 三、工程骨架（目录必须一致）

```
LIMS/
├── backend/
│   ├── src/main/java/com/lims/
│   │   ├── LimsApplication.java
│   │   ├── common/     R.java  ResultCode.java  BusinessException.java
│   │   │               GlobalExceptionHandler.java  PageResult.java
│   │   ├── config/     MybatisPlusConfig.java  WebMvcConfig.java  CorsConfig.java
│   │   ├── security/   JwtUtil.java  AuthInterceptor.java
│   │   ├── controller/  service/  mapper/  entity/  dto/  vo/
│   │   └── ...
│   └── src/main/resources/
│       ├── application.yml  application-dev.yml  application-local.yml.example
│       └── templates/report-cma.doc  report-catl.doc
└── frontend/
    └── src/
        ├── api/  views/  components/  stores/  router/  utils/  types/
        ├── layouts/  directives/  App.vue  main.ts  env.d.ts
```

`application.yml` 关键项：
```yaml
server:
  port: 8081
  servlet:
    context-path: /api
spring:
  profiles: { active: dev }
  servlet:
    multipart: { max-file-size: 20MB, max-request-size: 20MB }
lims:
  jwt: { secret: "<32位以上字符串>", expiration: 86400000 }
  upload-dir: ./uploads
mybatis-plus:
  global-config:
    db-config: { id-type: auto, logic-delete-field: deleted }
```

## 四、数据库

统一 `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`（旧系统为 utf8，若需 1:1 兼容旧数据可保留 utf8）。
**日期一律用 `varchar` 存**，格式 `2023.12.5`（历史原因，不要改成 DATE）。
**状态一律单字符**：`T` 完成 / `F` 未完成 / `R` 退回（不要用布尔或枚举）。
**编号列前后可能有空格或制表符**，所有按编号匹配都要 `TRIM(col) = ?`。

### 4.1 `project` —— 核心主表（一行一个样品/项目，90 列）

```sql
CREATE TABLE `project` (
  `id_nj` bigint NOT NULL AUTO_INCREMENT,
  -- 样品标识
  `bh` varchar(20) COMMENT '项目编号，如 JK(2023)-SA-422',
  `customerId` varchar(10), `productName` varchar(40),
  `ggxh` varchar(30), `ypsb` varchar(30), `ypdj` varchar(30), `ypbz` varchar(255),
  -- 检验信息
  `jymd` varchar(20), `jylb` varchar(20), `jylx` varchar(60),
  `date_sy` varchar(10), `date_cy` varchar(10), `date_yywc` varchar(10),
  `userId_cy` varchar(20) COMMENT '抽样人工号', `cyjs` varchar(20),
  `cyaddr` varchar(80), `isUpfile` varchar(2),
  -- 三方单位
  `customerId_scdw` varchar(10), `customerId_sjdw` varchar(10),
  -- 样品属性
  `date_sc` varchar(30), `scBatch` varchar(20), `ypNum` varchar(20), `byNum` varchar(20),
  `otherFile` varchar(30), `ypSaveReq` varchar(30), `yyProcess` varchar(30),
  `getReport` varchar(30), `isFitTestReq` varchar(4),
  `ypCharacter` varchar(30), `ypSituation` varchar(30), `testLib` varchar(50),
  -- 备注
  `remarks_register` varchar(255), `remarks_div` varchar(255), `remarks_agree` varchar(255),
  `remarks_arrange` varchar(255), `remarks_examine` varchar(255),
  `remarks_verify` varchar(255), `remarks_financial` varchar(255),
  -- 状态位（全部 varchar(8)，取值 T/F/R）
  `state_register`, `state_div`, `state_arrange`, `state_comlete`, `state_agree`,
  `state_examine`, `state_verify`, `state_financial`, `state_cwprocess`,
  `state_reportprint`, `state_isstop`,
  -- 环节经办人与日期（varchar(10)，格式 yyyy-MM-dd）
  `userId_reg`, `date_reg`, `userId_div`, `date_div`,
  `userId_agree`, `date_agree`, `userId_arrange`, `date_arrange`,
  `userId_examine`, `userId_verify`, `date_verify`,
  `date_jystart`, `date_jycomplete`, `date_downreport`, `date_printreport`,
  -- 任务/计划
  `jhmc` varchar(50), `jhflag` varchar(50), `exstd` varchar(20),
  `category` varchar(50), `category_sub` varchar(50), `cybh` varchar(30),
  `scdateflag` varchar(30) COMMENT '导入日期标志',
  `result` varchar(10) COMMENT '项目检验结论：合格/不合格',
  `taskno` varchar(30), `taskbatchno` varchar(30),
  `dwname_sj_nj` varchar(40), `dwname_wt_nj` varchar(40), `dwname_sc_nj` varchar(40),
  -- 收费
  `price` varchar(10), `totalprice` varchar(10), `sfmethod` varchar(10),
  `htbh` varchar(10), `htcoin` float(10,2), `subtotal` float(10,2),
  `isreceived` varchar(10), `billno` varchar(10), `fileno` varchar(10),
  `date_stamp` varchar(10), `date_recfee` varchar(10), `usergetmoney_nj` varchar(20),
  `memo` text, `sentreport` varchar(255),
  PRIMARY KEY (`id_nj`)
);
```
（其余状态位与日期列类型同上一行示例，均为 `varchar(8)` / `varchar(10)`。）

### 4.2 明细与任务

```sql
CREATE TABLE `prj_detail` (
  `project_bh` varchar(20) NOT NULL, `item_xh` smallint NOT NULL,
  `testItem` varchar(100) NOT NULL COMMENT '检测项目，带单位如「阿维菌素,mg/kg」',
  `stdValue` varchar(255), `basis` varchar(100), `mathod` varchar(100),
  `method_note` varchar(100), `userId_jyy` varchar(10) COMMENT '检验员工号',
  `jyResult` varchar(255), `jyResult_note` varchar(100),
  `item_evaluate` varchar(10) COMMENT '单项评价：合格/不合格',
  `num_lowerst` varchar(30) COMMENT '最低检出限',
  PRIMARY KEY (`project_bh`,`item_xh`)
);

CREATE TABLE `task` (
  `id` int NOT NULL AUTO_INCREMENT,
  `userId_jyy` varchar(10) NOT NULL, `project_bh` varchar(20) NOT NULL,
  `date_jyy` varchar(10), `state_jyy` varchar(8) COMMENT 'F未检/T已检',
  PRIMARY KEY (`id`)
);
-- 注意：task 一行 = 一个样品 × 一个检验员，同一样品可有多行

CREATE TABLE `lib` (
  `productId` varchar(10) NOT NULL, `xh` smallint NOT NULL,
  `testItem` varchar(255), `basis` varchar(100), `mathod` varchar(100),
  `stdValue` varchar(50), `num_lowerst` varchar(20), `method_note` varchar(30),
  PRIMARY KEY (`productId`,`xh`)
);

CREATE TABLE `product` (
  `id` varchar(10) NOT NULL COMMENT '如 nagw001 / nasc001 / nasg001',
  `prd_category` varchar(50) NOT NULL COMMENT '谷物/蔬菜/水果',
  `libName` varchar(100) NOT NULL COMMENT '产品名，如 糙米',
  `prd_contains` text,
  PRIMARY KEY (`id`)
);
```

### 4.3 基础数据

```sql
CREATE TABLE `user` (
  `gh` varchar(10) NOT NULL COMMENT '工号即主键',
  `name` varchar(10) NOT NULL, `password` varchar(10) NOT NULL,
  `userRoleId` varchar(4) NOT NULL, `deptId` varchar(20),
  `xueli` varchar(10), `zhicheng` varchar(10), `zy` varchar(20),
  `mobile` varchar(20), `email` varchar(20), `position` varchar(20),
  `certificate` varchar(20), `nature` varchar(20),
  PRIMARY KEY (`gh`)
);

CREATE TABLE `customer` (
  `id` smallint NOT NULL AUTO_INCREMENT,
  `company` varchar(50) NOT NULL, `lxr` varchar(10) NOT NULL, `leader` varchar(10),
  `lxrMobileNo` varchar(20), `leaderMobileNO` varchar(20), `addr` varchar(80),
  `type` varchar(40), `bank` varchar(20), `bankNo` varchar(20), `status` varchar(20),
  `fenlei` varchar(50), `kaituoren` varchar(50), `weihuren` varchar(50),
  `diqu` varchar(255), `pinyin` varchar(255),
  PRIMARY KEY (`id`)
);

CREATE TABLE `dept` (
  `deptno` varchar(2) NOT NULL, `deptName` varchar(20) NOT NULL,
  `leader` varchar(10), `memo1` varchar(20), PRIMARY KEY (`deptno`)
);

CREATE TABLE `basisname` (
  `id` int NOT NULL AUTO_INCREMENT, `basis` varchar(100), `basisName` varchar(255),
  `note` varchar(255), PRIMARY KEY (`id`)
);

CREATE TABLE `user_method` (   -- 方法检验员：method 存方法码 SA/NA/XA
  `id` smallint NOT NULL AUTO_INCREMENT, `usergh` varchar(10),
  `method` varchar(100), `method_note` varchar(30), `memo` varchar(255),
  PRIMARY KEY (`id`)
);

CREATE TABLE `user_item` (     -- 项目检验员：item 存检测项目主体名
  `id` smallint NOT NULL AUTO_INCREMENT, `usergh` varchar(10),
  `item` varchar(100), `memo` varchar(255), PRIMARY KEY (`id`)
);

CREATE TABLE `contract` (
  `id` int NOT NULL AUTO_INCREMENT, `htbh` varchar(10), `customerId` varchar(10),
  `content` text, `total` float(10,2), `htcoin` float(10,2),
  `htcoin_remain` float(10,2), `unitcost` float(10,2),
  `isreceive` varchar(10), `alive_status` varchar(10), `date` varchar(10),
  PRIMARY KEY (`id`)
);
```

### 4.4 任务/权限/杂项

```sql
CREATE TABLE `sjtask` (        -- 监抽/采样任务，由抽样单 xls 导入
  `id` int NOT NULL AUTO_INCREMENT,
  `taskno` varchar(30), `leader` varchar(30), `source` varchar(100),
  `region` varchar(50), `note` varchar(255), `date` varchar(20),
  `taskname` varchar(50), `taskxz` varchar(30) COMMENT '委托送样/委托抽样/监督抽检',
  `picino` varchar(30), `xiaddate` varchar(20), `wancdate` varchar(10),
  `rwdji` varchar(10), `yxlyaoq` varchar(50), `chouyhj` varchar(10),
  `jcxm` varchar(40), `chouystate` varchar(10), `state` varchar(10),
  PRIMARY KEY (`id`)
);

CREATE TABLE `sjtask_import_log` (   -- 防重复导入
  `marker` varchar(128) NOT NULL COMMENT 'xls 的 A1 单元格',
  `original_name` varchar(255) NOT NULL, `row_count` int NOT NULL DEFAULT 0,
  `created_by` varchar(64), `created_at` datetime NOT NULL,
  PRIMARY KEY (`marker`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `rolemenu` (
  `id` int NOT NULL, `roleId` varchar(10), `menuId` varchar(10), PRIMARY KEY (`id`)
);

CREATE TABLE `sys` (           -- 注意：这是流水号表，不是系统配置表
  `id` smallint NOT NULL, `tnsw_no` varchar(10), `tnsj_no` varchar(10),
  `zzlevel` varchar(10), `permissioner` varchar(10), `chief` varchar(10),
  PRIMARY KEY (`id`)
);
```

### 4.5 种子数据（保证每个页面打开都有内容）

- `user`：造 15~20 个账号，**密码 = 工号**，`userRoleId` 用 `100`(管理员) / `3`(检验员) / `6`
- `user_method`：`njsa000→SA`、`njna000→NA`、`njxa000→XA`（水检/农检/畜检三员）
- `user_item`：若干条"检验员 ↔ 检测项目"对应关系
- `product`：`nagw001..013` 谷物、`nasc001..039` 蔬菜、`nasg001..040` 水果
- `lib`：给 `nagw001`、`nagw002` 各造 20~30 条检测项目（`testItem` 形如 `阿维菌素,mg/kg`）
- `project`：造 15~20 个样品，覆盖各种状态组合（未登记 / 已登记未分解 / 已分解未安排 / 已安排未检 / 已检未签发 / 已签发）
- `customer`：8~10 家单位；`basisname`、`dept` 若干
- 编号规则：`JK(2023)-SA-001`、`JK(2023)-NA-001`，中段 `SA/NA/XA` 即方法码

## 五、后端接口清单

统一响应 `R<T>`：`{"code":0,"msg":"success","data":{}}`
`code`：0 成功 / 400 参数错误 / 401 未认证 / 403 无权限 / 500 系统异常。
业务异常抛 `BusinessException(code, msg)`，由 `@RestControllerAdvice` 转统一格式。

| Controller | 前缀 | 接口 |
|---|---|---|
| AuthController | `/auth` | `POST /login`（username/password → token/tokenType/expiresIn）、`GET /me` |
| LimsDataController | `/data/{entity}` | `GET`（page/size/keyword/任意过滤参数）、`POST`、`PUT`、`DELETE`、`GET /{entity}/export` |
| SampleSheetController | `/sample-sheet` | `POST /preview`(multipart)、`POST /import`(token)、`GET /template` |
| SampleRegisterController | `/sample-register` | `GET /pending`、`GET /registered`、`POST /confirm`{ids}、`POST /revert`{id} |
| SampleRegisterQueryController | `/sample-register-query` | `GET /list`、`GET /divide-detail`(bh) |
| SampleExportController | `/sample-export` | `POST /{type}`{bhs}，type=`water\|agri\|livestock\|detail` |
| DecomposeController | `/decompose` | `GET /pending`、`GET /done`、`POST /generate`、`POST /complete`、`POST /revert`、`POST /return-register` |
| ArrangeController | `/arrange` | `GET /pending`、`GET /done`、`POST /assign`、`POST /revert`、`POST /return-decompose`、`POST /return-register` |
| InspectController | `/inspect` | `GET /pending`(mine)、`GET /done`(mine)、`GET /items`(bhs)、`POST /save`、`POST /revert`、`POST /return-arrange` |
| ReviewController | `/review` | `GET /pending`、`POST /issue`{bhs}、`POST /return-register`、`GET /summary` |
| FileLibraryController | `/file-library` | `GET`、`GET /{type}` |
| FileController | `/files` | `POST /upload`、`GET`、`GET /{name}`、`DELETE /{name}` |
| LibImportController | `/lib-import` | `POST /preview`、`POST /confirm`{token}、`GET /template` |
| ReportDownloadController | `/report-download` | `GET /{type}?bh=`，type=`cma\|catl` |

**鉴权**：`AuthInterceptor` 拦截 `/**`，放行 `/auth/login`、`/error`、`OPTIONS`。
校验 `Authorization: Bearer <token>`，解出工号后写入 `request.setAttribute("userId", gh)`，
Controller 用 `@RequestAttribute("userId") String userId` 取当前用户。

## 六、前端页面与菜单

侧边栏 8 个分组、28 个菜单项（`el-sub-menu` + `el-menu-item`，路由与菜单项 `index` 一致）：

| 分组 | 菜单项 → 路由 |
|---|---|
| 服务信息 | 中心文件资料 `/service/files`｜项目库查询 `/service/project-library`｜在检项目查询 `/service/current`｜历史项目查询 `/service/history` |
| 任务管理 | 监抽任务管理 `/task/supervision-manage`｜监抽执行查询 `/task/supervision-execute`｜监抽抽样查询 `/task/supervision-sample` |
| 样品管理 | 导入采样单 `/sample/import`｜样品登记确认 `/sample/register`｜报告下载 `/sample/report-download`｜登记查询 `/sample/register-query`｜导出数据 `/sample/export` |
| 分解排样 | 单个样品分解 `/dispatch/single`｜批量样品分解 `/dispatch/batch`｜任务安排 `/dispatch/arrange`｜批量安排 `/dispatch/batch-arrange` |
| 项目检验 | 任务查询 `/inspection/query`｜数据录入 `/inspection/input` |
| 审核签发 | 报告签发 `/review/issue`｜检验汇总查询 `/review/query` |
| 基础数据 | 用户信息 `/base/user`｜方法检验员 `/base/method`｜检验岗位 `/base/position`｜项目库维护 `/base/project`｜中心资料上传 `/base/upload`｜个人资料修改 `/base/profile` |
| 系统标准库名 | 标准库名维护 `/standards/name` |

另有 `/login`、`/403`、`/404`（公开路由），`/` 重定向到 `/service/files`。

### 关键设计：配置驱动的通用表格页（必须实现）

写**一个** `DataPage.vue`，由路由 `meta.config` 描述表格，覆盖 8 个标准 CRUD 页
（`product` / `user` / `user_method` / `basisname` / `sjtask` / `task`）。
`PageConfig` 字段：`entity,title,primaryKeys,columns[],showIndex,searchFields[],formFields[],canCreate,canEdit,canDelete,canExport,showFilters,showKeyword,defaultFilters,createButtonText,deleteButtonText,...`
`ColumnConfig` 支持 `prop,label,width,minWidth,align,detail`（`detail:true` 时单元格变链接，点击弹明细窗）。

### 其余为定制页

批量分解/安排用「左待办 + 右已完成」双面板布局（`el-row :gutter="16"` + `el-col :md="14/10"` + 两张 `el-card`）。
行内操作按钮（分解完毕/退登记/转待分解…）逐行显示，`size="small"`，操作中按钮 `loading`。

## 七、必须照做的七个实现细节

### 7.1 状态机

```
导入采样单(xls→sjtask)
 → 登记确认   state_register F→T，回写 date_reg/userId_reg
 → 分解       lib → prj_detail，state_div→T，state_arrange→R
 → 安排       写 prj_detail.userId_jyy + INSERT task，state_arrange→T
 → 数据录入   prj_detail.jyResult/item_evaluate，task.state_jyy→T + date_jyy
 → 签发       state_verify→T + date_stamp
 → 报告下载   按 Word 模板生成 .doc
```
每个环节都要有反向操作：转待分解 / 退分解 / 转待安排 / 退安排 / 转待检验 / 退登记。

**列表口径**（一定要按此写 WHERE）：
- 待登记 `state_register='F'`；已登记 `='T'`
- 待分解 `COALESCE(state_div,'')<>'T'`；已分解 `='T'`
- 待安排 `state_div='T' AND COALESCE(state_arrange,'')<>'T'`；已安排 `='T'`
- 待检验 `COALESCE(task.state_jyy,'')<>'T' AND project.state_arrange='T'`；已检验 `='T'`
- 待签发 `state_comlete='T' AND COALESCE(state_verify,'')<>'T'`

**项目级完成回写**（容易漏，必须做）：数据录入标记完成时，若该样品下**所有 task 都是 T**
且**所有 prj_detail 都有 jyResult**，则回写 `project.state_comlete='T'`、`date_jycomplete=今天`，
并按各 `prj_detail.item_evaluate` 汇总 `project.result`（全合格→`合格`，有不合格→`不合格`）。

### 7.2 检验员匹配算法（`ArrangeService`）

按顺序尝试，命中即停：
1. 取 `prj_detail.testItem` **逗号前的主体**（`阿维菌素,mg/kg` → `阿维菌素`），查 `user_item.item`，取 `usergh`
2. 取 **样品编号中段的方法码**（`JK(2023)-SA-425` 按 `-` 切 → `SA`），查 `user_method.method`，取 `usergh`
3. 都未命中 → **不分配**，把该样品编号写入返回的 `messages` 数组提示用户

命中后：逐条 `UPDATE prj_detail SET userId_jyy=? WHERE project_bh=? AND item_xh=?`；
按「样品 + 检验员」去重后 `INSERT INTO task(userId_jyy, project_bh, date_jyy, state_jyy) VALUES (?,?,NULL,'F')`；
最后 `UPDATE project SET state_arrange='T' WHERE TRIM(bh)=?`。
返回 `{total, assigned, skipped, messages}`。

### 7.3 抽样单 `.xls` 导入格式

- 仅接受 `.xls`（HSSF），数据在名为 **`sheet1`** 的工作表
- **第 1 行 A1 = 文件唯一标识**（用于防重复导入，空则报错）
- **第 2 行 = 中文列标题**；**第 3 行起 = 数据**
- 列标题与 `sjtask` 字段映射（支持别名）：
  `任务编号/任务号→taskno`、`责任人/负责人→leader`、`任务来源/来源→source`、
  `省市区县/区域→region`、`任务描述/描述/备注→note`、`任务接受日期/接受日期→date`、
  `任务名称→taskname`、`任务性质/性质→taskxz`、`任务下达日期/下达日期→xiaddate`、
  `任务完成日期/完成日期→wancdate`、`抽样环节→chouyhj`、`检测项目→jcxm`
- 流程：`preview` 解析 + 落临时文件 + 返回 token → 用户确认 → `import(token)` 才写库
- 导入成功后写 `sjtask_import_log`，同一 `marker` 二次导入必须报错拒绝
- 必须提供 `GET /template` 下载模板（含 A1 标识 + 表头 + 示例行）

### 7.4 检验项目库 `.xls` 导入格式

- 同名 `sheet1`；**A1=项目库产品编号**（如 `nagw999`）、**B1=产品分类**、**C1=库名称**
- 第 2 行列标题：`序号/检测项目/检验依据/检验方法/第几法/标准值/检出限`
- 第 3 行起为数据 → 写 `product` + `lib`
- 同样 preview → confirm 两阶段，同样提供模板下载

### 7.5 报告下载（`ReportService`）

- 用两份 Word 模板 `resources/templates/report-cma.doc`、`report-catl.doc`
  （`catl` 多一行资质认定号；模板本身是 Word 2003 XML 文本，可用字符串替换）
- 打开后按样品编号替换封面与正文占位字段：样品编号、产品名称、受检单位、检验类别、
  抽样地点、抽样日期、样品数量、抽样人、检验起止日期、签发日期
- **结果表必须按 `prj_detail` 重建**：用正则定位 `<w:tr>` / `<w:tc>`，按明细行数复制行结构，
  填入检测项目、依据、指标要求、检出限、检验结果、单项评价
- 结论段按"是否有不合格项"生成；下载文件名 `{样品编号}-{受检单位}.doc`
- 若拿不到真实模板，用最小可用的 Word XML 模板代替，但替换与重建逻辑必须完整

### 7.6 导出 Excel（`SampleExportService`）

`water/agri/livestock` 三种上报口径共用一套列（旧库无字段可区分大类，仅在标题标注），
`detail` 为检验明细口径。单次最多 500 个样品。

- 上报列：`序号,任务编号,样品编号,产品名称,受检单位,生产单位,抽样地点,抽样日期,
  登记日期,检验完成日期,检测项目,检验依据,检验方法,指标要求,检验结果,单项评价`
- 明细列：`序号,样品编号,产品名称,检测项目,检验依据,检验方法,指标要求,
  检验结果,单项评价,单项评价说明,检验员,检验完成日期`

### 7.7 通用 CRUD 白名单（`LimsDataService`）

不接受任意表名。用代码内常量定义，每张表声明 `keyColumns`（主键）、`searchColumns`（keyword 模糊）、
`filterColumns`（精确等值）、`likeColumns`（该字段传值则 LIKE），另加 `dateVarcharColumns`
（varchar 日期列，区间查询时用 `STR_TO_DATE(col,'%Y.%c.%e')`）。
列名永远只来自常量或 `information_schema` 校验结果，值必须 `?` 参数化。

| entity | key | search | filter | like |
|---|---|---|---|---|
| customer | id | company,lxr,lxrMobileNo,pinyin | type,status,diqu,fenlei | — |
| product | id | id,libName,prd_category | prd_category | id,libName |
| lib | productId,xh | testItem,basis,mathod,stdValue | productId,basis,mathod | — |
| project | id_nj | bh,customerId,productName,taskno,htbh,result | state_register,state_div,state_arrange,state_examine,state_verify,state_isstop,result,state_comlete,date_jycomplete,date_reg,date_cy | bh,productName,dwname_wt_nj,dwname_sc_nj,dwname_sj_nj,taskbatchno,taskno,userId_cy,customerId_sjdw,scdateflag |
| prj_detail | project_bh,item_xh | testItem,basis,mathod,jyResult,item_evaluate | project_bh,userId_jyy,item_evaluate,jyResult | — |
| task | id | project_bh,userId_jyy,state_jyy | project_bh,userId_jyy,state_jyy | — |
| sjtask | id | taskno,leader,source,region,taskname,taskxz | taskxz,region,state,date | taskno,source,note |
| user | gh | gh,name,mobile,email,position | gh,userRoleId,deptId,nature,position | — |
| user_method | id | usergh,method,method_note | usergh,method | — |
| user_item | id | usergh,item | usergh,item | — |
| basisname | id | basis,basisName | basis | — |
| contract | id | htbh,customerId,content | customerId,alive_status,isreceive | — |

额外支持：`xxxNe`（不等值，xxx 必须在 filter 中）、`xxxStart` / `xxxEnd`（区间）。

## 八、UI 规范

- 整体：左侧 240px 深色侧边栏（`linear-gradient(180deg,#17324d,#1d4262,#24506f)`），右侧上方 64px 顶栏 + 内容区
- 主色 `#2f6fb3`，内容底色 `#ebf0f6`，卡片白底、圆角 12px、细边框 + 轻阴影
- 表格：通栏边框、斑马纹、表头底色 `#eaf0f7`、单元格纵向内边距 9px
- 表格主体在表内滚动：`max-height: calc(100vh - 280px)`；**水平滚动条常驻可见**（不能被 hover 才显示的逻辑藏掉）
- 列表页结构：`页面标题 + 右侧操作按钮 → 搜索条件行 → el-card 包裹的 el-table → 右下分页`
- 分页 `page-sizes="[20,50,100,200]"`，`layout="total, sizes, prev, pager, next"`
- 主色、圆角、边框统一用 CSS 变量 `--lims-*` 定义在 `App.vue`，不要在页面里散落硬编码颜色
- 全局挂 4 个详情弹窗组件（项目授权信息 / 任务信息 / 项目登记信息 / 项目分解-安排-检验明细），
  列表里的「编号」列点击即可打开；只渲染一份，状态放 `utils/entityDialogs.ts`

## 九、代码规范与质量门禁

**后端**
- 分层：Controller 只做参数校验 + 调 Service + 返回 `R`；业务逻辑在 Service，`@Transactional` 标在 Service
- 用 `JdbcTemplate` 写 SQL；MyBatis-Plus 仅用于简单实体（如 `User`）
- 列名不得来自请求参数；值一律 `?`
- 不删旧表数据、不加外键、不加 `NOT NULL` 到存量列

**前端**
- `<script setup lang="ts">`，TypeScript strict，**禁用 `any`**，每个接口响应都要有 interface
- 接口统一走 `src/api/http.ts` 的 axios 实例，`baseURL = import.meta.env.VITE_API_BASE_URL || '/api'`
- 请求拦截器塞 `Authorization`，响应拦截器处理 401（清 token 跳登录）
- 禁用 `console.log`（用 `warn`/`error`）；禁止硬编码后端地址
- 新增页面必须同时补路由和菜单项

**门禁（全部必须通过）**
```bash
cd backend  && mvn clean compile -q
cd frontend && npx vue-tsc --noEmit && npm run build && npm run lint
```

## 十、验收标准（逐条自测并说明结果）

1. `lims.sql` 导入后 16 张表齐全，`select count(*) from product/lib/project` 均非空
2. 用种子账号（密码=工号）能登录，`/api/auth/me` 返回工号/姓名/角色/部门
3. 未登录访问任意接口返回 401
4. 「导入采样单」下载模板 → 填 3 行 → 上传 → 预览 3 行 → 确认 → `sjtask` 增加 3 行；
   再传同一文件被拒绝（防重复生效）
5. 「样品登记确认」把 1 个样品从待登记确认掉，右面板出现该样品
6. 「单个样品分解」点「分解完毕」→ `prj_detail` 出现该样品的检测项目 → 右面板出现
7. 「任务安排」点「安排」→ `prj_detail.userId_jyy` 有值、`task` 生成记录；
   故意把某检测项目从 `user_item` 删掉，再安排时应提示"没有匹配到检验员"
8. 「数据录入」录完全部项目并标记完成 → `task.state_jyy='T'`
   **且** `project.state_comlete='T'`、`date_jycomplete` 有值、`result` 已按评价汇总
9. 「报告签发」列表出现该样品 → 签发 → `state_verify='T'`、`date_stamp` 有值
10. 「报告下载」选 cma 下载得到 .doc，用 Word 打开不报错，样品编号/产品名/结果表与数据一致
11. 「导出数据」选 detail 导出 .xls，列头与第七节 7.6 完全一致
12. 8 个配置驱动的标准页（用户信息/方法检验员/检验岗位/项目库/标准库名…）都能查、能改、能分页
13. 前端 28 个菜单项全部可点开且无报错，浏览器控制台无红字

## 十一、不要做的事

- 不要把表名/字段名改成"规范"命名（`project`/`prj_detail`/`bh`/`gh` 都是业务约定）
- 不要把 varchar 日期改成 DATE、不要把单字符状态改成枚举或布尔
- 不要为了"架构整洁"引入 Spring Security、DDD 分层、DTO 泛型工厂等本需求外的东西
- 不要在 SQL 里拼接用户输入
- 不要留 `TODO` 或"此处略"，只交能编译运行的完整文件
- 不要跳过第十节的任何一条验收
