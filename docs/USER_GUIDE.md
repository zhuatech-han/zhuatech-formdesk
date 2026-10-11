# FormDesk 操作手册 / User guide

知华科技（上海如静知华信息科技有限公司） · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2

## 首次设置 / First setup

中文：管理员登录后，在组织与设置维护实际组织、分类和收集上限；在登录账号建立表单负责人、处理员或查阅员。权限管理支持自定义角色和 ALL / DEPARTMENT / ASSIGNED 范围。账号、角色、系统目录管理还需要 ALL 范围。ASSIGNED 只能查看分派给自己的填写记录，不能重新分派。

English: Set up organizations, categories and collection limits, then create managers, processors and report viewers. Roles combine permissions with ALL / DEPARTMENT / ASSIGNED scope. Identity and settings administration additionally requires ALL. ASSIGNED processors can only read assigned responses and cannot reassign them.

## 设计与发布 / Design and publication

填写名称、分类、组织和真实收集说明，添加字段、选项、必填和提示。八类字段：单行、多行、邮箱、数字、日期、单选、多选、附件。条件只引用前面的、无条件单选字段；字段移动后需要重新检查条件。最多40字段、30选项。数字最多18位有效数字及6位小数。

Provide title, category, organization and an accurate collection notice. Add fields, choices, required flags and help. Eight types: short/long text, email, number, date, single/multiple choice and attachment. Conditions may reference an earlier unconditional single-choice field. Check conditions after moving fields. Maximum 40 fields, 30 choices; numbers allow 18 significant digits and up to 6 decimal places.

保存仅改变草稿。发布创建不可变版本，生成填写链接和二维码；新记录使用当前版本，既有草稿和正式提交保留原版本。可设置截止时间，界面使用浏览器时区，服务端按UTC校验。截止时刻起拒绝写入。关闭或组织停用也阻止新填、保存及补充。重新发布允许继续收集；归档不可恢复。

Saving updates the draft only. Publication creates an immutable revision and a response URL/QR code. New responses use it; existing drafts and submissions retain their revision. The browser displays local deadlines; the server uses UTC and rejects writes at the exact deadline. Closing or disabling the organization blocks creation, saving and supplements. Republishing resumes collection; archiving is irreversible.

二维码编码的是当前部署地址。`127.0.0.1` 二维码不能在另一台手机访问。手机试用须从可访问的局域网域名或公网HTTPS域名打开管理端后复制链接；生产部署需要自己的域名和网络配置。未提供公网托管服务。

The QR code encodes the current deployment origin. A phone cannot reach another machine's `127.0.0.1`. Open the management UI through a reachable LAN address or HTTPS domain before sharing. Public hosting is not included.

## 填写、恢复与补充 / Respond, resume and supplement

无需注册。第一次保存、上传或提交时创建数据库记录并签发私有回执，浏览器地址自动转为 `/resume#密钥`。密钥存在fragment，请求仅通过 `X-Receipt-Key` 头传输，数据库只保存SHA-256散列。持有链接即拥有该记录读取/草稿编辑能力；请自行保存，勿公开分享。丢失后无法匿名找回。没有自动保存：保存进度后才持久化文本。

No respondent account is required. First save, upload or submit creates a persistent response and private receipt; the browser changes to `/resume#key`. The key stays in the fragment, is transmitted only in `X-Receipt-Key`, and is stored only as a SHA-256 hash. Possession grants read/draft-edit access. Save it privately; anonymous recovery is unavailable if lost. Text persists only after explicit Save progress; there is no autosave.

上传限制PDF/PNG/JPEG，5MB/文件，并检查扩展名和文件签名。附件直接存入数据库，拥有独立记录归属校验；下载强制保存。签名验证不等于病毒扫描。上传成功不自动保存尚未保存的文本。正式提交需完成显示中的必填项、有效格式和收集同意；提交后锁定编辑。仅待补充状态可再次编辑并提交，之后恢复处理中。内部备注不出现在公开回执；要求补充的文字可见。

Uploads accept PDF/PNG/JPEG up to 5MB each with extension/signature checks. Database storage and ownership checks protect files; downloads force attachment handling. This is not malware scanning. Uploading does not automatically save unsaved text. Submission validates visible required fields, formats and consent, then locks editing. Only Needs information can be edited again; resubmission becomes In progress. Internal notes are absent from receipts; information requests are visible.

## 分派、处理、导出与删除 / Process, export and delete

负责人查看正式提交后分派组织内启用且拥有处理权限的账号。待处理→处理中/待补充/完成/驳回；处理中→待补充/完成/驳回；待补充由填写者补充提交→处理中；完成/驳回须填写原因才能重新处理。草稿不能处理或分派。每次修改检查记录版本，冲突后必须刷新核对。

Managers assign submitted responses to enabled processors in the same organization. Submitted → In progress / Needs information / Completed / Rejected; In progress → Needs information / Completed / Rejected; respondent resubmission → In progress. Reopening a completed/rejected response requires a reason. Drafts cannot be processed or assigned. Every mutation checks record version; refresh and verify after conflict.

CSV按发布版本的字段标签逐字段导出，包含编号、版本、状态、负责人编号和更新时间；不包含回执密钥或附件二进制，文件仅导出名称。草稿不导出，角色和范围仍生效，公式前缀转义。具有删除数据权限的账号可以明确删除一条记录、附件和内部事件；保留无内容的删除审计。回执立即失效。备份中可能仍有历史内容，备份保留策略需部署方管理。

CSV exports one row per revision field with response ID, revision, status, assignee ID and update time. Receipt secrets and file binaries are excluded; filenames only. Drafts are excluded; scope applies and formula prefixes are escaped. Purge deletes one response, files and internal events; only a content-free audit remains. Receipts stop working immediately. Backups may retain historic data; deployers control backup retention.
