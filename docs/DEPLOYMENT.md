# 部署、升级与备份 / Deployment, upgrades and backups

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

## 首次安装 / First installation

Docker/Compose V2，建议4GB可用内存；源码调试JDK21、Maven3.9+、Node24.19+、Python3.10+。

```sh
python3 scripts/init-env.py
docker compose -p formdesk config --quiet
docker compose -p formdesk up -d --build --wait --wait-timeout 240
```

初始化不覆盖 `.env`。admin密码从私有 `.env` 读取。首次空库Flyway迁移与目录初始化自动完成；重启不重置密码。WEB_PORT默认8142，BIND_ADDRESS默认127.0.0.1；仅前端端口暴露。数据库与附件同一mysql-data卷持久化。

Initialization refuses to overwrite `.env`. Read administrator password privately. Flyway and bootstrap initialize an empty DB automatically and never reset existing accounts. Default WEB_PORT 8142 and localhost bind; only the frontend port is exposed. Database and attachments persist in mysql-data.

## 配置 / Configuration

MYSQL_ROOT_PASSWORD、DATABASE_PASSWORD、ADMIN_PASSWORD必须是独立强密码，无弱默认值。ADMIN_PASSWORD 12–72字节，含大写、小写与数字。ADMIN_USERNAME仅首次空库生效。COOKIE_SECURE本机HTTP=false，HTTPS=true。DATABASE_URL和DATABASE_USER可选外部MySQL；需受信任CA、VERIFY_IDENTITY及迁移DDL权限。内置网络使用REQUIRED TLS，外部MySQL不由备份脚本管理。

Use independent strong root/database/admin passwords; admin passwords require 12–72 bytes with uppercase/lowercase/digit. Administrator environment variables only apply to an empty database. COOKIE_SECURE=false for local HTTP, true for HTTPS. External MySQL requires trusted CA, VERIFY_IDENTITY and migration DDL permissions; bundled backup scripts do not manage external databases.

## 网络与公开填写 / Network and public forms

本机地址不能供外部手机扫码。局域网测试可私有修改BIND_ADDRESS为可达网卡并使用该IP打开管理端；不要未经评估暴露到公网。公网需自己的域名、有效HTTPS证书和反向代理，同源代理所有路径，配置COOKIE_SECURE=true，限制来源和请求速率、上传和存储配额、监控及备份。不要代理到数据库端口。

Localhost URLs are not reachable from respondents' phones. For controlled LAN testing bind to a reachable interface and access management through that address. Public deployment requires a domain, valid HTTPS reverse proxy, secure cookies, rate/storage/upload limits, monitoring and backups. Proxy all paths to the same frontend origin; never expose MySQL.

不缓存 `/api`，不要记录 X-Receipt-Key、请求答案或真实密码。应用不信任Forwarded/X-Forwarded头；本机Nginx代理下匿名速率限制共享代理来源，应在入口另配置实际来源限速。浏览器fragment不会发送给服务器。持有恢复链接者拥有记录权限，不适合强身份认证的敏感收集场景。

Do not cache API responses or log receipt headers/answers/passwords. Forwarded headers are not trusted. With bundled Nginx, application public limits share the proxy source; add actual-source limits at the gateway. URL fragments are not sent to the server. Receipt possession grants access; use stronger identity designs for sensitive collection.

## 备份与恢复 / Backup and restore

仅备份当前指定项目；短暂停止该后端写入以获得一致快照。SQL包含账号散列、答案、回执散列和附件二进制，必须私有保存。只恢复到全新独立本机项目，不能覆盖已有容器或卷；只导入可信备份，SQL不是沙箱。

Backups pause only the named backend for consistency. SQL includes password/receipt hashes, answers and binary attachments; keep it private. Restore only to a fresh isolated localhost project with no existing resources. Import trusted backups only.

```sh
python3 scripts/backup.py --project formdesk --output private-backups/formdesk.zip
# Prepare a private .env.restore with a different WEB_PORT, e.g. 28142.
python3 scripts/restore.py private-backups/formdesk.zip --project formdesk-restore --env-file .env.restore
```

升级前备份，新增Flyway版本，不修改已执行的SQL校验和；不要运行`down -v`删除业务库。代码版本与数据库版本需同时保留。停止学习环境使用`docker compose -p formdesk down`；只有确认是本次临时验收资源时才能删除卷。

Back up before upgrading, add Flyway versions and retain executed checksums. Never delete production volumes with `down -v`. Preserve source/database versions together; cleanup may only remove explicitly temporary QA resources.

## 常见故障 / Troubleshooting

- 登录失败：核实私有ADMIN_PASSWORD；已有库不会因修改.env重置管理员。 / Changing environment does not reset an existing admin.
- 保存冲突：刷新记录核对版本，勿直接重复提交。 / Refresh after a version conflict.
- 扫码无法打开：检查链接域名/IP、网络可达性和HTTPS，不能用另一台机器的localhost。 / Use a reachable deployment origin.
- 表单不能写入：检查关闭、截止、组织停用、额度与状态。 / Check closing, deadline, organization, quotas and response state.
- 附件失败：仅5MB内PDF/PNG/JPEG，签名与扩展名须匹配，附件上限由管理员配置。 / Check type, signature, size and quotas.
- 构建失败：读取当前项目日志并核实官方镜像源可达，不跳过测试。 / Diagnose logs and registry access; do not skip tests.

## 源码调试 / Source development

JDK 21 / Maven 3.9+ / Node 24.19+。使用开发覆盖文件只把数据库绑定到本机，不启动容器版后端/前端；backend 为8080、Vite默认5173，MySQL开发端口可通过MYSQL_DEV_PORT私有配置。 / Expose MySQL to localhost only with the development override; do not run container backend/frontend simultaneously. Backend listens8080, Vite5173; MYSQL_DEV_PORT defaults33142.

```sh
docker compose -p formdesk-dev -f compose.yaml -f compose.dev.yaml up -d mysql
python3 scripts/dev-backend.py
# Another terminal / 另一终端
cd frontend
npm ci --no-audit --no-fund
npm run dev
```

标准交付验收使用完整Compose；本机源码调试路径需要安装上述JDK/Maven，未另行验证本机Java运行时。 / Delivery verification uses full Compose; native source debugging requires the stated installed JDK/Maven and has not been separately verified on this host.
