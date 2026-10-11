[中文](README.md) | [English](README.en.md)

# FormDesk 表单与业务信息收集系统 · Java / Spring Boot / Vue / MySQL

**知华科技（上海如静知华信息科技有限公司）** · [官网](https://www.zhuatech.cn/)

FormDesk 把“发布收集入口—手机填写—保存进度—提交—分派处理—要求补充—完成—导出”放在一套可自行部署的系统中。适合官网项目询盘、报名申请、项目交付资料收集，以及软件实施团队为客户交付独立收集系统。

**公开源码学习版／非商业源码版。** 仅限个人学习、技术研究与非商业交流；未经书面授权不得商用。源码公开不代表免费商用或OSI开源许可，详见 [LICENSE](LICENSE)。

## 谁使用、做什么

| 使用者 | 已实现能力 |
| --- | --- |
| 填写者（无需注册） | 扫码/链接填写、显示条件、格式检查、附件、明确保存进度、私有回执恢复、查看状态、待补充时修改提交 |
| 表单负责人 | 建立表单与字段、预览、保存草稿、发布版本/截止、关闭归档、查询记录、分派、处理、内部备注、CSV导出 |
| 处理员 | 读取与处理分派给自己的记录；不能重新分派或越权读其他记录 |
| 管理员 | 账号启停与改密/重置、角色权限、组织数据范围、菜单、分类字典、系统收集/附件上限、审计及明确删除收集数据 |
| 统计查阅员 | 读取表单与真实汇总统计，不能读提交答案 |

八类字段：单行、多行、邮箱、数字、日期、单选、多选、附件。条件只引用前面的无条件单选字段。每份表单最多40字段、每选择字段最多30选项；数据由后端独立校验。列表支持搜索、状态筛选、分页、排序；统计来自真实数据库。

保存草稿不影响当前发布版本；新发布生成不可变快照，既有填写记录保留原版本。提交后锁定，负责人要求补充后才可修改；再次提交恢复处理中。关闭/截止/组织停用会阻止新填、保存及补充，已存在回执仍可查看（组织停用则拒绝回执）。归档不可恢复。删除记录同时删除附件及内部事件，保留无内容审计。

## 实际运行页面

截图使用本系统实际运行的TEST验收记录，不代表真实客户案例。空库安装不会加载这些业务记录或测试账号。截图不包含密码、回执密钥或真实客户资料。

### 登录 / Login

私有管理账号会话登录。 / Private management session login.

![登录 / Login](docs/screenshots/01-login.jpg)

### 表单工作台 / Forms workspace

真实数据库目录、分类、状态和查询。 / Persistent directory, category, state and search.

![表单工作台 / Forms workspace](docs/screenshots/02-forms.jpg)

### 表单设计 / Form design

字段与发布草稿分开维护。 / Edit fields in an unpublished draft.

![表单设计 / Form design](docs/screenshots/03-editor.jpg)

### 填写端 / Respondent page

无需账号，按发布版本显示及校验。 / Account-free response against a published revision.

![填写端 / Respondent page](docs/screenshots/04-public.jpg)

### 处理详情 / Processing

查看答案、分派、补充要求和处理事件。 / Review answers, ownership, information requests and events.

![处理详情 / Processing](docs/screenshots/05-submission.jpg)

### 后台账号 / Accounts

真实角色、组织、账号启停与重置。 / Roles, organizations, enablement and reset.

![后台账号 / Accounts](docs/screenshots/06-accounts.jpg)

### 数据统计 / Statistics

草稿、正式提交及处理状态统计。 / Draft, submitted and processing counts.

![数据统计 / Statistics](docs/screenshots/07-reports.jpg)

### 权限设置 / Permissions

角色权限和ALL/组织/分派范围。 / Roles and ALL/organization/assigned scope.

![权限设置 / Permissions](docs/screenshots/08-roles.jpg)

### 手机填写 / Mobile form

响应式填写页面。 / Responsive respondent page.

![手机填写 / Mobile form](docs/screenshots/09-mobile.jpg)

### 英文工作台 / English workspace

界面切换，业务数据不翻译。 / UI language switching; business data stays unchanged.

![英文工作台 / English workspace](docs/screenshots/10-english.jpg)

## 架构与目录

Java21、Spring Boot4.0.7、Spring Security、JPA/Hibernate、Flyway；Vue3.5.43、Vite8.1.5、Lucide、qrcode；MySQL8.4、Nginx、Docker Compose。浏览器通过同源Nginx访问 `/api`，数据库保存表单、发布版本、答案、处理记录及附件。BCrypt密码、HttpOnly/SameSite会话、CSRF、实时权限/组织范围和版本校验共同执行。

```text
backend/src/main/java/cn/zhuatech/formdesk/  # auth, administration, forms, responses
backend/src/main/resources/db/migration/   # V1 identity, V2 versioned collection
backend/src/test/java/                     # rule + HTTP/security/JPA tests
frontend/src/components/                  # designer, respondent and admin pages
frontend/public/brand/                    # official brand and original contact images
docs/screenshots/                         # actual running pages
scripts/                                  # environment, QA, release, backup/restore
compose.yaml                              # MySQL + Java backend + Nginx/Vue
.env.example                              # names only, no credentials
```

## 环境与启动

Docker Engine/Desktop、Compose V2，建议至少4GB可用内存；源码调试 JDK21、Maven3.9+、Node24.19.0+、npm、Python3.10+。构建须访问官方Maven/npm/Docker源；无付费第三方服务依赖。

```sh
python3 scripts/init-env.py
docker compose -p formdesk config --quiet
docker compose -p formdesk up -d --build --wait --wait-timeout 240
```

管理端 [http://127.0.0.1:8142/](http://127.0.0.1:8142/)；健康 [http://127.0.0.1:8142/actuator/health](http://127.0.0.1:8142/actuator/health)。首次账号 `admin`，密码读取本机私有 `.env` 的 `ADMIN_PASSWORD`，没有共享演示口令。初始化脚本随机生成独立凭证，拒绝覆盖.env；重启和修改环境变量不会重置已有账号。源码开发可先启动Compose数据库，再使用环境变量启动后端与 `npm run dev` 前端（Vite代理8080），不要提交真实.env。

## 数据库与配置

Flyway顺序执行 `V1__identity.sql`、`V2__forms.sql`，Hibernate仅校验结构。14张应用表及Flyway历史表；初始目录：一个组织、管理员、4角色、11权限、7菜单、3分类、2参数，无业务示例。MySQL和附件都持久化到 `mysql-data` 卷。升级前备份并新增版本，不改已执行迁移校验和。

| 配置名称 | 用途 |
| --- | --- |
| MYSQL_ROOT_PASSWORD / DATABASE_PASSWORD | 必填、独立强密码，无弱默认值 |
| ADMIN_USERNAME / ADMIN_PASSWORD | admin；密码12–72字节含大小写及数字；仅首次空库 |
| WEB_PORT / BIND_ADDRESS | 8142 / 127.0.0.1，可私有覆盖避免端口冲突 |
| COOKIE_SECURE | 本机HTTP false；HTTPS部署 true |
| DATABASE_URL / DATABASE_USER | 可选外部MySQL；受信任CA、VERIFY_IDENTITY与迁移DDL权限 |

全部配置名称见 [.env.example](.env.example)，组织设置中可调整每表单记录上限（含草稿，默认1000、最大10000）、每记录附件数量（默认10、最大20）。附件5MB/文件固定上限，检查PDF/PNG/JPEG扩展名和签名，下载强制保存；不提供病毒扫描。

## 操作、部署与安全

先建立实际组织和账号，再设计字段及真实收集说明，保存后发布，复制链接或二维码。填写者保存进度后妥善保管私有恢复链接。处理端分派、查看补充、完成或驳回，必要时导出CSV。详细步骤见 [操作手册](docs/USER_GUIDE.md)、[API与权限](docs/API.md)。

**手机不能访问另一台机器的127.0.0.1。** 扫码地址来自管理页面当前域名：局域网试用须使用可达地址，公网须自己的域名、HTTPS反向代理和安全Cookie。仓库不提供公网托管试用。部署、升级和一致备份/全新独立恢复见 [部署说明](docs/DEPLOYMENT.md)。

回执密钥在URL fragment，通过专用头传输，数据库只存散列；持有链接可访问该记录，丢失无法匿名找回，勿公开分享。不要记录请求答案或恢复头；备份含答案、密码散列和附件，需私有保存及制定保留策略。角色/组织范围是单实例访问控制，不是运营型多租户托管边界。未经容量、安全及业务审查，不应直接宣称生产可用。

## 测试、反馈与维护

```sh
mvn -B -f backend/pom.xml spotless:check test package
cd frontend
npm ci --no-audit --no-fund
npm run format:check
npm run lint
npm test
npm run build
cd ..
python3 scripts/release-check.py
git diff --check
```

48项后端测试（17单元+31真实HTTP/JPA/Flyway/Security）、12项前端测试。全新MySQL、业务流程、权限、持久化和浏览器检查另见 [测试说明](docs/TESTING.md)。Docker后端打包执行测试，不跳过。专用空库部署验收：

```sh
python3 scripts/quality.py --base http://127.0.0.1:8142
python3 scripts/verify-persistence.py --base http://127.0.0.1:8142
```

常见问题：无法登录先核实私有管理员密码；版本冲突刷新核对；扫码失败检查域名和网络；不能提交检查关闭/截止/额度/必填。详细排查见部署说明。问题可提交仓库Issue；贡献须保留品牌、自有许可、第三方版权并附可复现验证，不提交凭据或客户数据。漏洞请通过下方联系入口私下反馈，勿公开敏感内容。

## 已知限制与未实现集成

- 目前为单实例学习版，每类实体查询最多10000条，部分过滤/统计在内存完成，所有写入通过数据库序列化锁；没有大规模、高并发生产容量承诺。
- 无自动保存、后台定时清理、邮件/短信/微信通知、强身份核验、防机器人验证码、SSO、付费收款、电子签名、考试题库或AI功能。
- 无已完成CRM/ERP/Webhook集成、跨语言表单内容翻译、网站iframe嵌入或外部对象存储；界面中英文切换不翻译业务数据。附件存在数据库，备份受512MB解压上限限制，实际容量须部署方规划。
- 文件签名检查不等于病毒扫描；私有回执不是实名认证。硬件手机扫码、公网域名和跨网络访问未做实机验收。只验证响应式手机宽度，不提供原生App。
- 基础流程无需付费外部凭据；通知、支付、CRM等尚未实现，商业授权不表示这些集成已经完成。第三方依赖保持各自许可，见 [第三方说明](docs/THIRD_PARTY.md)。软件按现状提供，授权和免责声明以LICENSE为准。

## 授权与联系知华科技

本项目由知华科技（上海如静知华信息科技有限公司）提供公开源码学习版本，主要用于个人学习、技术研究与非商业交流。未经书面授权不得商用。企业信息化建设、中小企业数字化转型、中小企业 AI 转型、私有化部署、软件外包、软件项目外包、软件实施、FDE 外包、OPC 技术支持及深度定制开发，请访问知华科技官网 https://www.zhuatech.cn/，或添加微信 zhuatech、zhuatech2 咨询。

**商业授权或深度定制开发请联系知华科技。** 公司：上海如静知华信息科技有限公司；[官网](https://www.zhuatech.cn/)；微信 `zhuatech`、`zhuatech2`。仅限个人学习交流，未经授权不得商用，非MIT/Apache免费商用协议。

<table><tr><td align="center"><img src="docs/images/wechat-zhuatech.png" alt="微信 zhuatech" height="200"><br>微信 zhuatech</td><td align="center"><img src="docs/images/wechat-zhuatech2.png" alt="微信 zhuatech2" height="200"><br>微信 zhuatech2</td></tr></table>

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
