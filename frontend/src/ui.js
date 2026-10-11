// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import { ref } from "vue";
export const language = ref("zh");
/** 双语显示，业务数据不翻译。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function t(zh, en) {
  return language.value === "zh" ? zh : en;
}
export const states = {
  DRAFT: ["草稿", "Draft"],
  PUBLISHED: ["收集中", "Published"],
  CLOSED: ["已关闭", "Closed"],
  ARCHIVED: ["已归档", "Archived"],
  SUBMITTED: ["待处理", "Submitted"],
  IN_PROGRESS: ["处理中", "In progress"],
  NEEDS_INFO: ["待补充", "Needs information"],
  COMPLETED: ["已完成", "Completed"],
  REJECTED: ["已驳回", "Rejected"],
};
/** 真实状态名称。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function stateName(c) {
  return states[c] ? t(...states[c]) : c;
}
export const types = {
  TEXT: ["单行文本", "Short text"],
  TEXTAREA: ["多行文本", "Long text"],
  EMAIL: ["邮箱", "Email"],
  NUMBER: ["数字", "Number"],
  DATE: ["日期", "Date"],
  SELECT: ["单选", "Single choice"],
  MULTI: ["多选", "Multiple choice"],
  FILE: ["附件", "Attachment"],
};
/** 字段类型名称。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function typeName(c) {
  return types[c] ? t(...types[c]) : c;
}
const errors = {
  UNAUTHENTICATED: [
    "会话失效，请重新登录。",
    "Session expired. Sign in again.",
  ],
  LOGIN_FAILED: [
    "账号、密码错误或账号已停用。",
    "Invalid credentials or disabled account.",
  ],
  LOGIN_THROTTLED: [
    "尝试过多，五分钟后重试。",
    "Too many attempts. Retry in five minutes.",
  ],
  FORBIDDEN: ["没有操作权限。", "Permission denied."],
  OUT_OF_SCOPE: ["记录不在访问范围内。", "Record outside your scope."],
  VERSION_CONFLICT: [
    "记录已更新，请刷新核对后再操作。",
    "Record changed. Refresh and check before retrying.",
  ],
  INVALID_INPUT: [
    "字段格式不正确，请检查填写内容。",
    "Invalid field format. Check your input.",
  ],
  REQUIRED_FIELD: [
    "还有必填项未完成，请检查当前显示的字段。",
    "Complete all visible required fields.",
  ],
  CONSENT_REQUIRED: [
    "提交前请阅读并确认收集说明。",
    "Read and accept the collection notice before submitting.",
  ],
  SCHEMA_INVALID: [
    "字段需唯一代码、名称，选项不能重复或为空。",
    "Use unique field keys, labels and nonempty unique choices.",
  ],
  CONDITION_INVALID: [
    "条件只能引用前面的无条件单选字段及其有效选项。",
    "Conditions must reference an earlier unconditional single-choice field.",
  ],
  UNKNOWN_FIELD: [
    "字段不属于当前发布版本。",
    "Field not in this published version.",
  ],
  EMAIL_INVALID: ["邮箱格式不正确。", "Invalid email address."],
  NUMBER_INVALID: [
    "数字格式或精度不正确。",
    "Invalid number format or precision.",
  ],
  DATE_INVALID: ["日期格式不正确。", "Invalid date."],
  OPTION_INVALID: [
    "请选择当前版本提供的选项。",
    "Select options from this form version.",
  ],
  FORM_CLOSED: [
    "表单已关闭、截止或停用，暂不能填写。",
    "Form closed, expired or disabled.",
  ],
  FORM_ARCHIVED: ["已归档表单不能修改。", "Archived forms cannot be edited."],
  DEADLINE_INVALID: [
    "截止时间必须晚于当前时间。",
    "Deadline must be in the future.",
  ],
  RESPONSE_LIMIT: [
    "该表单已达到收集上限，请联系发布者。",
    "Form response limit reached. Contact the publisher.",
  ],
  FILE_LIMIT: ["附件数量已达到上限。", "Attachment limit reached."],
  FILE_TOO_LARGE: [
    "文件不能为空且不能超过 5 MB。",
    "File must be nonempty and at most 5 MB.",
  ],
  FILE_INVALID: [
    "仅支持有效 PDF、PNG、JPEG，文件名不能包含路径。",
    "Use a PDF, PNG or JPEG without a path in its filename.",
  ],
  RECEIPT_INVALID: [
    "恢复链接无效或记录已删除。",
    "Resume link invalid or response deleted.",
  ],
  SUBMISSION_LOCKED: [
    "已提交的记录不能修改；等待负责人要求补充。",
    "Submitted response locked unless more information is requested.",
  ],
  STATE_INVALID: [
    "当前状态不允许此操作，请刷新核对。",
    "Action not allowed in this state. Refresh and check.",
  ],
  ASSIGNEE_INVALID: [
    "负责人需启用、属于该组织且有处理权限。",
    "Assignee must be enabled in this organization with processing permission.",
  ],
  PUBLIC_THROTTLED: [
    "操作过于频繁，一分钟后重试。",
    "Too many requests. Retry in a minute.",
  ],
  RECORD_REFERENCED: [
    "有历史记录，不能删除；请关闭后归档。",
    "Record has history. Close and archive it instead.",
  ],
  CATEGORY_DISABLED: ["分类不存在或已停用。", "Category missing or disabled."],
  IDENTITY_LOCKED: [
    "记录的组织、账号或代码不能更换。",
    "Record identity cannot be changed.",
  ],
  DEPARTMENT_DISABLED: ["组织已停用。", "Organization disabled."],
  LAST_ADMIN: [
    "必须保留一个启用的完整管理员。",
    "Keep one enabled full administrator.",
  ],
  PASSWORD_WEAK: [
    "密码需 12–72 字节，含大小写字母和数字。",
    "Password needs 12–72 bytes with uppercase, lowercase and a digit.",
  ],
  OLD_PASSWORD_INVALID: ["原密码错误。", "Current password incorrect."],
  NOT_FOUND: ["记录不存在。", "Record not found."],
  CONFLICT: ["数据重复或仍被使用。", "Duplicate or referenced record."],
  RESULT_UNKNOWN: [
    "网络中断，结果未知。请刷新核对，不要重复提交。",
    "Connection interrupted; result unknown. Refresh and check before resubmitting.",
  ],
  NETWORK_ERROR: [
    "网络连接失败，请检查后重试。",
    "Network unavailable. Check your connection.",
  ],
  RESOURCE_LIMIT: [
    "超过学习版查询上限。",
    "Learning edition query limit reached.",
  ],
  CLIPBOARD_FAILED: [
    "无法复制，请手动选中链接复制。",
    "Copy unavailable. Select and copy the link manually.",
  ],
};
/** 可理解的错误反馈，不暴露服务端内部信息。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function errorMessage(e) {
  return errors[e.message]
    ? t(...errors[e.message])
    : `${t("操作未完成", "Action failed")} (${e.message})`;
}
export const confirmation = ref(null);
/** 页面内确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function ask(message) {
  if (confirmation.value) return Promise.resolve(false);
  return new Promise((resolve) => (confirmation.value = { message, resolve }));
}
/** 接受或取消确认。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function answerConfirmation(ok) {
  const p = confirmation.value;
  confirmation.value = null;
  p?.resolve(ok);
}
/** 编辑副本避免直接改变列表。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function cloneRecord(r) {
  return JSON.parse(JSON.stringify(r));
}
/** 按浏览器时区显示日期，截止由后端UTC判定。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function dateTime(v) {
  return v
    ? new Intl.DateTimeFormat(language.value === "zh" ? "zh-CN" : "en", {
        dateStyle: "medium",
        timeStyle: "short",
      }).format(new Date(v))
    : "—";
}
/** 前端显示条件与服务端相同；服务端仍独立校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function visibleField(f, answers) {
  return !f.whenKey || answers[f.whenKey] === f.whenValue;
}
/** 新字段拥有稳定代码，移动排序不改变已有代码。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function newField() {
  return {
    key: "f_" + crypto.randomUUID().replaceAll("-", "").slice(0, 16),
    type: "TEXT",
    label: "",
    help: "",
    required: false,
    options: [],
    whenKey: "",
    whenValue: "",
  };
}

/** 已关闭或截止的回执只读；服务端仍独立拒绝写入。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
export function canEditResponse(response) {
  return (
    !response ||
    (response.accepting && ["DRAFT", "NEEDS_INFO"].includes(response.status))
  );
}
