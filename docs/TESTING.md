# 测试 / Testing

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

后端17项字段/边界单元测试、31项真实MockMvc/JPA/Flyway/Security集成测试；前端12项API/下载/显示条件测试。H2用于隔离服务验证，完整部署验收必须再用全新MySQL8.4。后端Docker构建执行所有测试，不跳过。

17 backend rule/boundary tests, 31 real MockMvc/JPA/Flyway/Security tests, 12 frontend API/download/condition tests. H2 isolates service tests; fresh MySQL8.4 deployment is verified separately. Docker Maven builds run all tests.

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

部署验收脚本仅允许127.0.0.1，检查空库后创建TEST记录、账号和附件；拒绝对非空实例执行。私有快照在已忽略output/，不输出密码或回执密钥。执行一次，重跑须重新建立专用空数据库，不要清理已有业务环境。

The deployment suite requires localhost and an empty database; it creates TEST records/accounts/files and keeps private snapshots under ignored output/. It never prints passwords or receipt keys. Run once; repeated QA requires a new isolated empty volume, never deletion of an existing business instance.

```sh
python3 scripts/quality.py --base http://127.0.0.1:8142
docker compose -p formdesk restart backend
python3 scripts/verify-persistence.py --base http://127.0.0.1:8142
```

覆盖：完整发布/恢复/补充/分派/完成/导出、条件隐藏字段、草稿/发布隔离、跨组织及本人分派范围、重复提交与版本冲突、截止时刻、私有附件、收集删除及回执撤销；重启与备份恢复核对完整业务记录和附件散列。

Coverage includes publication/resume/supplement/assignment/completion/export, conditional hidden values, revision isolation, organization/assigned scope, duplicate submission/version conflict, exact deadline, private files and purge/receipt revocation. Restart/restore checks compare complete business records and attachment hashes.

实际浏览器验收另检查创建编辑、字段类型切换与选项输入、条件显示、发布与二维码、公开填写、资料补充、处理、管理、统计、中英文、手机宽度和截图。硬件手机扫码、公网网络、真实邮件/CRM对接、病毒扫描、大数据容量及压力测试未覆盖；不得称为生产容量验收。

Browser verification additionally checks editing, choice input, conditions, publishing/QR, public filling, supplements, processing, administration, statistics, bilingual UI and mobile-width screenshots. Physical phone scanning, public-network delivery, email/CRM integration, malware scanning and capacity/load tests are not covered.
