# 配方颗粒库存盘点系统

当前已完成终端管理 10 个业务模块的可运行开发版本：盘点库存、要货指导、医院供货、终端销量、整袋库存、零散库存、医院库存、期初库存、中药颗粒、医院管理。

- `frontend/`：Vue 3 + Vite，包含参考布局、筛选、多级库存表格、合计、排序、汇总、删除、导出和演示数据兜底。
- `backend/`：Java 8 + Spring Boot 2.7 + JDBC，包含 MySQL schema、初始化数据、分页查询、服务端排序、汇总、作废、CSV/XLSX 导出、参数校验、导出任务和操作审计。
- 要货指导：已接入 `/api/terminal/goods-guidance`，支持医院远程搜索、分页、排序、全量统计汇总、批量作废、CSV/XLSX 导出、库存链路/告警、来源追溯、详情和库存/消耗/补货建议展示。
- `盘点库存需求设计文档.md`：详细需求、数据模型、接口、计算规则和验收标准。
- 后 8 个模块使用统一业务记录模型，均提供分页查询、医院/物料/日期筛选、汇总、作废、校验和 CSV 导出；医院管理提供树形字段、类别字典和节点维护接口。

## 启动后端

确认本地 `MYSQL84` 服务已启动。业务数据库为 `pfpdxt`，连接配置为 `localhost:3306`、用户 `root`、密码 `123456`。首次启动会执行 `backend/src/main/resources/schema.sql` 初始化表和演示数据。

```powershell
cd backend
mvn spring-boot:run
```

后端地址：`http://localhost:8082`

## 启动前端

```powershell
cd frontend
npm install
npm run dev
```

前端地址：`http://localhost:5173`

前端在后端不可用时会自动展示截图风格的本地演示数据；后端连通后显示“已连接数据库”并使用真实接口。

## 接口

- `GET /api/terminal/stocktake`：分页查询盘点库存。
- `GET /api/terminal/stocktake/hospitals`：医院下拉选项。
- `POST /api/terminal/stocktake/aggregate`：汇总选中记录或条件范围。
- `DELETE /api/terminal/stocktake`：软删除/作废选中记录。
- `POST /api/terminal/stocktake/export`：导出当前查询范围 XLSX（可传 `format=CSV` 兼容 CSV）；小于等于 50,000 条同步返回文件，超过阈值返回导出任务。
- `GET /api/terminal/stocktake/{id}`：查看盘点记录详情、公式和操作记录。
- `GET /api/terminal/goods-guidance`：查询要货指导。
- `POST /api/terminal/goods-guidance/aggregate`：汇总要货指导。
- `DELETE /api/terminal/goods-guidance`：作废要货指导记录。
- `POST /api/terminal/goods-guidance/export`：导出要货指导 XLSX（可传 `format=CSV` 兼容 CSV）；超过 50,000 条返回异步任务。
- `GET /api/terminal/goods-guidance/{id}`：查看要货指导详情、来源同步时间、公式和操作记录。
- `GET /api/exports/tasks/{taskId}`：查询导出任务状态。
- `GET /api/exports/tasks/{taskId}/download`：下载已完成的导出文件（默认保留 30 分钟）。

作废接口要求 `reason` 为 2～200 个字符；汇总和导出均按当前筛选条件处理全量数据，并写入 `operation_audit_log`。数据库已建立统一 `module_business_record`、中药颗粒别名、医院类别和医院树形扩展字段，并为 8 个模块写入演示数据。页面数量最多显示 3 位小数、金额固定 2 位、单价最多 6 位；原有盘点库存和要货指导继续使用完整 XLSX 导出、来源追溯和公式详情。

## 认证与医院数据范围

默认 `stocktake.auth.enabled=false`，便于本地演示。接入登录系统后将其设为 `true`，接口读取以下请求头：`X-User-Id`、`X-Permissions`（逗号分隔权限编码，管理员可传 `*`）和 `X-Hospital-Ids`（逗号分隔医院 ID，全部医院可传 `*`）。服务端会在查询、详情、汇总、作废、导出和导出任务下载时校验权限与医院范围。
