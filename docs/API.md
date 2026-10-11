# API 与权限 / API and access

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

浏览器同源会话、HttpOnly/SameSite=Strict cookie，所有写入包括匿名写入和导出均须CSRF头。`GET /api/auth/csrf` 获取 header/token；`POST /api/auth/login` 使用 username/password；退出与改密通过POST。密码重置/账号停用/组织停用使旧会话失效，角色权限实时查询。

Same-origin session authentication, HttpOnly/SameSite Strict cookies and CSRF for all mutations, including public responses and export. Fetch header/token from `GET /api/auth/csrf`, login with username/password, use POST for logout/password change. Reset/disable revokes sessions; permissions are evaluated live.

| Endpoint | Purpose / 权限 |
| --- | --- |
| GET /api/options | forms；范围内组织、分类、可分派账号 |
| GET/POST /api/forms | forms / forms_manage；查询或创建草稿 |
| GET/PUT /api/forms/:id | forms / forms_manage；详情或保存完整草稿 |
| POST /api/forms/:id/publish,close,archive,delete | forms_manage；version；publish另有closesAt |
| GET /api/submissions | submissions；q,state,formId,page,size；先过滤范围 |
| GET /api/submissions/:id | submissions；答案、附件元数据、内部事件 |
| POST /api/submissions/:id/process | process；version,action,note,assigneeId |
| POST /api/submissions/:id/purge | purge；version；不可逆删除 |
| POST /api/forms/:id/export | export；CSV且写审计，不能使用GET |
| GET /api/attachments/:id | submissions；检查记录归属范围 |
| GET /api/reports | reports；状态与表单汇总 |
| GET /api/audit | audit；不含答案或回执 |
| GET/POST/PUT /api/admin/:kind[/:id] | users/roles/settings + ALL范围 |
| GET /api/public/forms/:key | 当前已发布且未关闭/截止的版本 |
| POST /api/public/forms/:key/begin | 创建草稿，返回token与response |
| GET/PUT /api/public/response | X-Receipt-Key；恢复或保存 |
| POST /api/public/response/submit | X-Receipt-Key；正式提交/补充提交 |
| POST /api/public/response/files | X-Receipt-Key；multipart file,field,version |
| GET/DELETE /api/public/response/files/:id | X-Receipt-Key；下载/删除；删除需version |
| GET /actuator/health | 匿名健康检查，无详细配置 |

编辑字段示例 / Field example:

```json
{"key":"name","label":"Contact name","type":"TEXT","help":"","required":true,"options":[],"whenKey":"","whenValue":""}
```

表单保存 / Form write: `title,description,category,departmentId,consentText,fields`；更新另需 `version`。发布 `version,closesAt`（ISO8601 UTC或空）。填写写入 `version,answers,consented`。错误只返回code，401身份、403权限、404不存在、409版本/状态、410关闭、413大小、429速率。匿名公开写入每来源每分钟最多60次；这是单实例内存窗口，需网关补充反滥用。

Mutations require current version, and invalid state transitions are rejected. Errors expose only codes: 401 identity, 403 permission, 404 missing, 409 state/version, 410 closed, 413 size and 429 throttle. Public writes are limited to 60/source/minute in one instance; add gateway abuse controls.

角色/组织不是真正多租户SaaS托管边界，本项目提供单实例组织数据范围；站点管理员有全局授权能力。 / Organization scope is not a managed multi-tenant SaaS boundary; site administrators have global access.
