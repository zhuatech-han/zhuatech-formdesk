<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import {
  FileText,
  Inbox,
  ChartColumn,
  Users,
  Shield,
  Settings,
  History,
  Plus,
  ArrowLeft,
  RefreshCw,
  LogOut,
  Menu,
  X,
  ExternalLink,
} from "@lucide/vue";
import QRCode from "qrcode";
import { api, resetApi, download } from "./api.js";
import {
  language,
  t,
  stateName,
  errorMessage,
  dateTime,
  typeName,
  confirmation,
  answerConfirmation,
  ask,
} from "./ui.js";
import AdminPanel from "./components/AdminPanel.vue";
import PublicForm from "./components/PublicForm.vue";
import FormEditor from "./components/FormEditor.vue";
const path = location.pathname;
const publicKey = path.startsWith("/f/") ? path.slice(3) : "";
const receiptKey = path === "/resume" ? location.hash.slice(1) : "";
const publicMode = path.startsWith("/f/") || path === "/resume";
const profile = ref(null),
  ready = ref(false),
  pending = ref(false),
  notice = ref(null),
  view = ref("forms"),
  mobile = ref(false),
  about = ref(false),
  passwordForm = ref(null),
  login = ref({ username: "", password: "" }),
  adminRef = ref(null),
  editorRef = ref(null);
const options = ref({ departments: [], categories: [], accounts: [] }),
  forms = ref({ items: [], total: 0 }),
  records = ref({ items: [], total: 0 }),
  report = ref(null),
  audit = ref([]),
  selected = ref(null),
  submission = ref(null),
  editing = ref(false),
  editId = ref(null),
  q = ref(""),
  state = ref(""),
  sort = ref("newest"),
  page = ref(1),
  formFilter = ref(""),
  publishDialog = ref(null),
  qr = ref(""),
  shareLink = ref(""),
  processForm = ref({ action: "IN_PROGRESS", note: "", assigneeId: "" });
const has = (code) => profile.value?.permissions.includes(code);
const icons = {
  forms: FileText,
  submissions: Inbox,
  reports: ChartColumn,
  users: Users,
  roles: Shield,
  settings: Settings,
  audit: History,
};
const menuName = (code) =>
  ({
    forms: t("表单", "Forms"),
    submissions: t("填写记录", "Submissions"),
    reports: t("统计", "Reports"),
    users: t("登录账号", "Accounts"),
    roles: t("角色与权限", "Roles & permissions"),
    settings: t("组织与设置", "Organizations & settings"),
    audit: t("操作记录", "Audit trail"),
  })[code] || code;
const menus = computed(
  () => profile.value?.menus.filter((m) => icons[m.code]) || [],
);
const actorName = (id) =>
  options.value.accounts.find((a) => a.id === id)?.displayName ||
  (id ? "#" + id : t("未分派", "Unassigned"));
const departmentName = (id) =>
  options.value.departments.find((d) => d.id === id)?.name || "#" + id;
const categoryName = (code) => {
  const c = options.value.categories.find((c) => c.code === code);
  return c ? t(c.name, c.nameEn) : code;
};
const maxPage = computed(() =>
  Math.max(
    1,
    Math.ceil(
      (view.value === "forms" ? forms.value.total : records.value.total) / 15,
    ),
  ),
);
const actionOptions = computed(() => {
  if (!submission.value) return [];
  const s = submission.value.status;
  let a = ["note"];
  if (s === "SUBMITTED")
    a.push("IN_PROGRESS", "NEEDS_INFO", "COMPLETED", "REJECTED");
  if (s === "IN_PROGRESS") a.push("NEEDS_INFO", "COMPLETED", "REJECTED");
  if (["COMPLETED", "REJECTED"].includes(s)) a.push("reopen");
  if (
    !["DRAFT", "COMPLETED", "REJECTED"].includes(s) &&
    profile.value.scope !== "ASSIGNED"
  )
    a.push("assign");
  if (s === "DRAFT") a = [];
  return a;
});
const eventLabels = {
  DRAFT_CREATED: ["开始填写", "Started response"],
  DRAFT_SAVED: ["保存进度", "Saved progress"],
  SUBMITTED: ["已提交", "Submitted"],
  FILE_ADDED: ["添加附件", "Added attachment"],
  FILE_REMOVED: ["移除附件", "Removed attachment"],
};
const processLabel = (a) =>
  eventLabels[a]
    ? t(...eventLabels[a])
    : a === "assign"
      ? t("分派负责人", "Assigned owner")
      : a === "note"
        ? t("内部备注", "Internal note")
        : a === "reopen"
          ? t("重新处理", "Reopened")
          : stateName(a);
const auditName = (a) =>
  ({
    LOGIN: t("登录", "Signed in"),
    PASSWORD_CHANGE: t("修改密码", "Changed password"),
    FORM_SAVE: t("保存表单草稿", "Saved form draft"),
    FORM_PUBLISH: t("发布表单", "Published form"),
    FORM_CLOSE: t("关闭收集", "Closed form"),
    FORM_ARCHIVE: t("归档表单", "Archived form"),
    FORM_DELETE: t("删除空草稿", "Deleted empty draft"),
    RESPONSE_EXPORT: t("导出记录", "Exported responses"),
    RESPONSE_PURGE: t("删除收集数据", "Purged response"),
    ADMIN_USERS: t("维护账号", "Updated accounts"),
    ADMIN_ROLES: t("维护角色", "Updated roles"),
    ADMIN_DEPARTMENTS: t("维护组织", "Updated organizations"),
    ADMIN_MENUS: t("维护菜单", "Updated menus"),
    ADMIN_PERMISSIONS: t("维护权限名称", "Updated permission labels"),
    ADMIN_DICTIONARIES: t("维护分类", "Updated categories"),
    ADMIN_SETTINGS: t("修改参数", "Updated parameters"),
  })[a] || (a.startsWith("RESPONSE_") ? processLabel(a.slice(9)) : a);

function fail(e) {
  notice.value = { type: "error", text: errorMessage(e) };
  if (e.status === 401) {
    profile.value = null;
    selected.value = null;
    submission.value = null;
    editing.value = false;
    resetApi();
  }
}
/** 集中写入防重复，不自动重试结果未知的请求。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function perform(url, method, body) {
  if (pending.value) return null;
  pending.value = true;
  notice.value = null;
  try {
    const v = await api(url, { method, body });
    notice.value = { type: "success", text: t("已保存。", "Saved.") };
    return v;
  } catch (e) {
    fail(e);
    return null;
  } finally {
    pending.value = false;
  }
}
async function read(fn) {
  try {
    await fn();
  } catch (e) {
    fail(e);
  }
}
onMounted(async () => {
  if (publicMode) {
    ready.value = true;
    return;
  }
  try {
    profile.value = await api("/auth/me");
    await initialize();
  } catch (e) {
    if (e.status !== 401) fail(e);
  } finally {
    ready.value = true;
  }
});
async function initialize() {
  view.value = menus.value[0]?.code || "forms";
  if (has("forms")) options.value = await api("/options");
  await load();
}
async function signIn() {
  const v = await perform("/auth/login", "POST", login.value);
  if (v) {
    profile.value = v;
    login.value.password = "";
    notice.value = null;
    await read(initialize);
  }
}
async function canLeave() {
  if (pending.value) return false;
  if (editorRef.value?.canLeave && !(await editorRef.value.canLeave()))
    return false;
  if (adminRef.value?.canLeave && !(await adminRef.value.canLeave()))
    return false;
  return true;
}
async function signOut() {
  if (!(await canLeave())) return;
  const v = await perform("/auth/logout", "POST", {});
  if (v) {
    profile.value = null;
    selected.value = null;
    submission.value = null;
    editing.value = false;
    notice.value = null;
    resetApi();
  }
}
async function navigate(code) {
  if (!(await canLeave())) return;
  view.value = code;
  page.value = 1;
  q.value = "";
  state.value = "";
  selected.value = null;
  submission.value = null;
  editing.value = false;
  mobile.value = false;
  notice.value = null;
  await read(load);
}
/** 读取真实分页与统计，不植入示例业务数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function load() {
  if (view.value === "forms")
    forms.value = await api(
      `/forms?q=${encodeURIComponent(q.value)}&state=${state.value}&page=${page.value}&sort=${sort.value}`,
    );
  else if (view.value === "submissions")
    records.value = await api(
      `/submissions?q=${encodeURIComponent(q.value)}&state=${state.value}&page=${page.value}${formFilter.value ? "&formId=" + formFilter.value : ""}`,
    );
  else if (view.value === "reports") report.value = await api("/reports");
  else if (view.value === "audit") audit.value = await api("/audit");
}
async function search() {
  page.value = 1;
  await read(load);
}
async function turn(d) {
  page.value += d;
  await read(load);
}
async function openForm(id) {
  await read(async () => {
    selected.value = await api("/forms/" + id);
    shareLink.value = location.origin + "/f/" + selected.value.form.publicKey;
    qr.value = await QRCode.toDataURL(shareLink.value, {
      width: 220,
      margin: 3,
      errorCorrectionLevel: "M",
    });
  });
}
async function edit(id = null) {
  if (!(await canLeave())) return;
  editId.value = id;
  editing.value = true;
}
async function saved(f) {
  editing.value = false;
  await read(load);
  await openForm(f.id);
}
async function formAction(action) {
  const f = selected.value.form;
  if (action === "publish") {
    publishDialog.value = {
      closesAt: f.closesAt
        ? new Date(
            new Date(f.closesAt).getTime() -
              new Date().getTimezoneOffset() * 60000,
          )
            .toISOString()
            .slice(0, 16)
        : "",
    };
    return;
  }
  if (
    !(await ask(
      action === "delete"
        ? t(
            "删除未发布的空表单？此操作不能撤销。",
            "Delete this empty draft? This cannot be undone.",
          )
        : action === "archive"
          ? t(
              "归档后不能再编辑或发布，已有回执仍可查看。确定归档？",
              "Archived forms cannot be edited or published. Existing receipts remain readable. Archive?",
            )
          : t(
              "关闭后无法新填、保存或补充，已有回执仍可查看。确定关闭？",
              "Closing blocks new responses, saves and additions. Existing receipts remain readable. Close?",
            ),
    ))
  )
    return;
  const v = await perform(`/forms/${f.id}/${action}`, "POST", {
    version: f.version,
  });
  if (v) {
    if (action === "delete") selected.value = null;
    else await openForm(f.id);
    await read(load);
  }
}
async function publish() {
  let closesAt = "";
  if (publishDialog.value.closesAt) {
    const d = new Date(publishDialog.value.closesAt);
    if (Number.isNaN(d.getTime())) {
      fail(new Error("DEADLINE_INVALID"));
      return;
    }
    closesAt = d.toISOString();
  }
  const f = selected.value.form;
  const v = await perform(`/forms/${f.id}/publish`, "POST", {
    version: f.version,
    closesAt,
  });
  if (v) {
    publishDialog.value = null;
    await openForm(f.id);
    await read(load);
  }
}
async function copyShare() {
  try {
    await navigator.clipboard.writeText(shareLink.value);
    notice.value = {
      type: "success",
      text: t("填写链接已复制。", "Response link copied."),
    };
  } catch {
    fail(new Error("CLIPBOARD_FAILED"));
  }
}
async function showSubmissions() {
  formFilter.value = selected.value.form.id;
  await navigate("submissions");
}
async function openSubmission(id) {
  await read(async () => {
    submission.value = await api("/submissions/" + id);
    processForm.value = {
      action: actionOptions.value[0] || "note",
      note: "",
      assigneeId: submission.value.assigneeId || "",
    };
  });
}
async function process() {
  if (
    !(await ask(t("确认执行此处理操作？", "Confirm this processing action?")))
  )
    return;
  const s = submission.value;
  const v = await perform(`/submissions/${s.id}/process`, "POST", {
    ...processForm.value,
    version: s.version,
  });
  if (v) {
    submission.value = v;
    processForm.value = {
      action: actionOptions.value[0] || "note",
      note: "",
      assigneeId: v.assigneeId || "",
    };
    await read(load);
  }
}
async function purge() {
  if (
    !(await ask(
      t(
        "永久删除此记录、填写内容和附件？私有回执将失效，无法撤销。",
        "Permanently delete this response, answers and files? Its receipt will stop working. This cannot be undone.",
      ),
    ))
  )
    return;
  const s = submission.value;
  const v = await perform(`/submissions/${s.id}/purge`, "POST", {
    version: s.version,
  });
  if (v) {
    submission.value = null;
    await read(load);
  }
}
async function exportCsv() {
  await read(() =>
    download(
      "/forms/" + selected.value.form.id + "/export",
      "form-" + selected.value.form.id + ".csv",
      { method: "POST", body: {} },
    ),
  );
}
async function changePassword() {
  const v = await perform("/auth/password", "POST", passwordForm.value);
  if (v) {
    passwordForm.value = null;
    profile.value = null;
    resetApi();
  }
}
async function refresh() {
  await read(async () => {
    if (has("forms")) options.value = await api("/options");
    if (selected.value) await openForm(selected.value.form.id);
    if (submission.value) await openSubmission(submission.value.id);
    await load();
  });
}
function responseValue(f) {
  const v = submission.value.answers[f.key];
  return Array.isArray(v) ? v.join(" / ") : v || "—";
}
</script>
<template>
  <PublicForm
    v-if="publicMode"
    :form-key="publicKey"
    :receipt-key="receiptKey"
  />
  <div v-else-if="!ready" class="startup">{{ t("正在加载…", "Loading…") }}</div>
  <div v-else-if="!profile" class="login-page">
    <header class="login-top">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" />FormDesk
      </div>
      <button @click="language = language === 'zh' ? 'en' : 'zh'">
        {{ language === "zh" ? "English" : "中文" }}
      </button>
    </header>
    <form class="login-card" @submit.prevent="signIn">
      <span class="eyebrow">FORMDESK</span>
      <h1>{{ t("登录工作台", "Sign in to your workspace") }}</h1>
      <p>
        {{
          t("表单发布与收集记录管理", "Form publishing and response management")
        }}
      </p>
      <div v-if="notice" class="notice error" role="alert">
        {{ notice.text }}
      </div>
      <label
        >{{ t("账号", "Username")
        }}<input
          v-model="login.username"
          required
          maxlength="60"
          autocomplete="username" /></label
      ><label
        >{{ t("密码", "Password")
        }}<input
          v-model="login.password"
          required
          type="password"
          autocomplete="current-password" /></label
      ><button class="primary full" :disabled="pending">
        {{ pending ? t("正在登录…", "Signing in…") : t("登录", "Sign in") }}
      </button>
    </form>
    <footer class="login-footer">
      {{
        t(
          "知华科技（上海如静知华信息科技有限公司）",
          "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
        )
      }}<br />{{
        t(
          "公开源码学习版 · 商用须书面授权",
          "Source-available learning edition · Commercial use requires written authorization",
        )
      }}
      <button class="link-button" @click="about = true">
        {{ t("联系知华科技", "Contact ZhiHua") }}
      </button>
    </footer>
  </div>
  <div v-else class="app-layout">
    <aside :class="['sidebar', { expanded: mobile }]">
      <div class="brand">
        <img src="/brand/logo.jpg" alt="知华科技" />FormDesk
      </div>
      <p class="sidebar-label">{{ t("工作空间", "WORKSPACE") }}</p>
      <nav>
        <button
          v-for="m in menus"
          :key="m.code"
          :class="{ active: view === m.code }"
          @click="navigate(m.code)"
        >
          <component :is="icons[m.code]" :size="17" />{{
            language === "zh" ? m.name : m.nameEn
          }}
        </button>
      </nav>
      <footer>
        <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
          >{{ t("知华科技官网", "ZhiHua website")
          }}<ExternalLink :size="12" /></a
        ><button class="link-button" @click="about = true">
          {{ t("商业授权与定制", "Licensing & customization") }}</button
        ><small
          >{{ t("非商业源码版", "Non-commercial source edition") }} ·
          1.0.0</small
        >
      </footer>
    </aside>
    <div class="workspace">
      <header class="topbar">
        <div class="actions">
          <button
            class="mobile-toggle"
            :aria-label="t('菜单', 'Menu')"
            @click="mobile = !mobile"
          >
            <Menu :size="18" /></button
          ><span>{{ menuName(view) }}</span>
        </div>
        <div class="actions">
          <span class="account-name">{{ profile.displayName }}</span
          ><button @click="language = language === 'zh' ? 'en' : 'zh'">
            {{ language === "zh" ? "English" : "中文" }}</button
          ><button @click="passwordForm = { oldPassword: '', newPassword: '' }">
            {{ t("改密", "Password") }}</button
          ><button
            :disabled="pending"
            :aria-label="t('退出登录', 'Sign out')"
            @click="signOut"
          >
            <LogOut :size="16" />
          </button>
        </div>
      </header>
      <main class="main-content">
        <div v-if="notice" :class="['notice', notice.type]" role="status">
          {{ notice.text
          }}<button
            :aria-label="t('关闭提示', 'Dismiss')"
            @click="notice = null"
          >
            <X :size="16" />
          </button>
        </div>
        <FormEditor
          v-if="editing"
          ref="editorRef"
          :id="editId"
          :options="options"
          :profile="profile"
          :pending="pending"
          :perform="perform"
          @saved="saved"
          @cancel="editing = false"
          @error="fail"
        />
        <AdminPanel
          v-else-if="['users', 'roles', 'settings'].includes(view)"
          ref="adminRef"
          :key="view"
          :section="view"
          :pending="pending"
          :perform="perform"
          @error="fail"
        />
        <section v-else-if="view === 'forms' && selected">
          <button class="link-button back" @click="selected = null">
            <ArrowLeft :size="16" />{{ t("返回表单", "Back to forms") }}
          </button>
          <header class="page-heading">
            <div>
              <div class="actions">
                <span
                  class="badge"
                  :class="selected.form.status.toLowerCase()"
                  >{{ stateName(selected.form.status) }}</span
                ><span class="muted"
                  >{{ categoryName(selected.form.category) }} ·
                  {{ departmentName(selected.form.departmentId) }}</span
                >
              </div>
              <h1>{{ selected.form.title }}</h1>
              <p>
                {{ t("已发布版本", "Published version") }}
                {{
                  selected.revisions.find(
                    (r) => r.id === selected.form.publishedId,
                  )?.number || "—"
                }}
                · {{ t("截止", "Closes") }}
                {{ dateTime(selected.form.closesAt) }}
              </p>
            </div>
            <div class="actions">
              <button :disabled="pending" @click="refresh">
                <RefreshCw :size="15" />{{ t("刷新", "Refresh") }}</button
              ><button
                v-if="
                  has('forms_manage') && selected.form.status !== 'ARCHIVED'
                "
                :disabled="pending"
                @click="edit(selected.form.id)"
              >
                {{ t("编辑草稿", "Edit draft") }}</button
              ><button
                v-if="
                  has('forms_manage') && selected.form.status !== 'ARCHIVED'
                "
                class="primary"
                :disabled="pending"
                @click="formAction('publish')"
              >
                {{ t("发布新版本", "Publish version") }}
              </button>
            </div>
          </header>
          <div class="form-overview">
            <section class="panel">
              <div class="section-heading">
                <h2>{{ t("收集入口", "Response access") }}</h2>
                <button v-if="has('submissions')" @click="showSubmissions">
                  {{ t("查看填写记录", "View submissions") }}
                </button>
              </div>
              <template v-if="selected.form.publishedId"
                ><div class="share-panel">
                  <img
                    v-if="qr"
                    :src="qr"
                    :alt="t('填写二维码', 'Response QR code')"
                  />
                  <div>
                    <p>
                      {{
                        t(
                          "扫码或打开链接填写。请分享可被填写者访问的部署地址。",
                          "Scan or open the link. Share a deployment address respondents can access.",
                        )
                      }}
                    </p>
                    <div class="link-row">
                      <input
                        :value="shareLink"
                        readonly
                        :aria-label="t('填写链接', 'Response link')"
                      /><button @click="copyShare">
                        {{ t("复制", "Copy") }}
                      </button>
                    </div>
                    <a :href="shareLink" target="_blank" rel="noopener"
                      >{{ t("打开填写页面", "Open response page") }} ↗</a
                    >
                    <p v-if="selected.form.status !== 'PUBLISHED'" class="hint">
                      {{
                        t(
                          "当前已关闭或归档，入口不能继续填写。",
                          "Closed or archived: response access is disabled.",
                        )
                      }}
                    </p>
                  </div>
                </div></template
              >
              <p v-else class="empty">
                {{
                  t(
                    "保存草稿后发布，即可获得填写链接和二维码。",
                    "Publish this draft to get a response link and QR code.",
                  )
                }}
              </p>
            </section>
            <section class="panel">
              <div class="section-heading">
                <h2>{{ t("草稿字段", "Draft fields") }}</h2>
                <span class="muted">{{ selected.fields.length }}</span>
              </div>
              <div
                v-for="(f, i) in selected.fields"
                :key="f.key"
                class="schema-row"
              >
                <span class="mono">{{ String(i + 1).padStart(2, "0") }}</span
                ><strong>{{ f.label }}</strong
                ><span>{{ typeName(f.type) }}</span
                ><span>{{
                  f.required ? t("必填", "Required") : t("选填", "Optional")
                }}</span>
              </div>
            </section>
          </div>
          <div class="section-heading">
            <h2>{{ t("发布历史", "Publication history") }}</h2>
            <div class="actions">
              <button
                v-if="has('export') && selected.form.publishedId"
                @click="exportCsv"
              >
                {{ t("导出填写记录 CSV", "Export response CSV") }}</button
              ><button
                v-if="
                  has('forms_manage') && selected.form.status === 'PUBLISHED'
                "
                :disabled="pending"
                @click="formAction('close')"
              >
                {{ t("关闭收集", "Close form") }}</button
              ><button
                v-if="
                  has('forms_manage') &&
                  ['CLOSED', 'DRAFT'].includes(selected.form.status)
                "
                :disabled="pending"
                @click="formAction('archive')"
              >
                {{ t("归档", "Archive") }}</button
              ><button
                v-if="has('forms_manage') && selected.form.status === 'DRAFT'"
                class="danger"
                :disabled="pending"
                @click="formAction('delete')"
              >
                {{ t("删除空草稿", "Delete empty draft") }}
              </button>
            </div>
          </div>
          <div class="panel table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("版本", "Version") }}</th>
                  <th>{{ t("标题", "Title") }}</th>
                  <th>{{ t("字段数", "Fields") }}</th>
                  <th>{{ t("发布状态", "Publication") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="r in selected.revisions" :key="r.id">
                  <td>v{{ r.number }}</td>
                  <td>{{ r.title }}</td>
                  <td>{{ r.fields.length }}</td>
                  <td>
                    {{
                      r.id === selected.form.publishedId
                        ? t("当前发布版本", "Current publication")
                        : t("历史快照", "Historical snapshot")
                    }}
                  </td>
                </tr>
                <tr v-if="!selected.revisions.length">
                  <td colspan="4" class="empty">
                    {{ t("尚未发布", "Not yet published") }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
        <section v-else-if="view === 'submissions' && submission">
          <button class="link-button back" @click="submission = null">
            <ArrowLeft :size="16" />{{
              t("返回填写记录", "Back to submissions")
            }}
          </button>
          <header class="page-heading">
            <div>
              <span class="badge" :class="submission.status.toLowerCase()">{{
                stateName(submission.status)
              }}</span>
              <h1>#{{ submission.id }} · {{ submission.revision.title }}</h1>
              <p>
                {{ t("版本", "Version") }} {{ submission.revision.number }} ·
                {{ actorName(submission.assigneeId) }} ·
                {{ dateTime(submission.updatedAt) }}
              </p>
            </div>
            <div class="actions">
              <button @click="refresh">{{ t("刷新", "Refresh") }}</button
              ><button
                v-if="has('purge')"
                class="danger"
                :disabled="pending"
                @click="purge"
              >
                {{ t("删除收集数据", "Delete response data") }}
              </button>
            </div>
          </header>
          <div class="record-layout">
            <section class="panel response-detail">
              <h2>{{ t("填写内容", "Response content") }}</h2>
              <dl>
                <template v-for="f in submission.revision.fields" :key="f.key"
                  ><dt>{{ f.label }}</dt>
                  <dd v-if="f.type !== 'FILE'">{{ responseValue(f) }}</dd>
                  <dd v-else>
                    <div
                      v-for="a in submission.attachments.filter(
                        (a) => a.fieldKey === f.key,
                      )"
                      :key="a.id"
                    >
                      <button
                        class="link-button"
                        @click="
                          read(() =>
                            download('/attachments/' + a.id, a.filename),
                          )
                        "
                      >
                        {{ a.filename }}
                      </button>
                      <small>{{ Math.ceil(a.size / 1024) }} KB</small>
                    </div>
                    <span
                      v-if="
                        !submission.attachments.some(
                          (a) => a.fieldKey === f.key,
                        )
                      "
                      >—</span
                    >
                  </dd></template
                >
              </dl>
              <p class="hint">
                {{
                  submission.consented
                    ? t(
                        "填写者已同意该版本收集说明。",
                        "Respondent accepted this version’s collection notice.",
                      )
                    : t(
                        "尚未同意收集说明。",
                        "Collection notice not accepted yet.",
                      )
                }}
              </p>
            </section>
            <section class="panel process-panel">
              <h2>{{ t("处理记录", "Process response") }}</h2>
              <form
                v-if="has('process') && actionOptions.length"
                @submit.prevent="process"
              >
                <label
                  >{{ t("操作", "Action")
                  }}<select v-model="processForm.action">
                    <option v-for="a in actionOptions" :key="a" :value="a">
                      {{ processLabel(a) }}
                    </option>
                  </select></label
                ><label v-if="processForm.action === 'assign'"
                  >{{ t("负责人", "Owner")
                  }}<select v-model="processForm.assigneeId" required>
                    <option value="">{{ t("请选择", "Choose") }}</option>
                    <option
                      v-for="a in options.accounts.filter(
                        (a) => a.departmentId === submission.departmentId,
                      )"
                      :key="a.id"
                      :value="a.id"
                    >
                      {{ a.displayName }}
                    </option>
                  </select></label
                ><label
                  >{{
                    processForm.action === "NEEDS_INFO"
                      ? t(
                          "要求补充的内容（填写者可见）",
                          "Information request (visible to respondent)",
                        )
                      : t("内部备注", "Internal note")
                  }}<textarea
                    v-model="processForm.note"
                    rows="4"
                    maxlength="3000"
                    :required="
                      ['note', 'NEEDS_INFO', 'reopen'].includes(
                        processForm.action,
                      )
                    "
                  ></textarea></label
                ><button class="primary full" :disabled="pending">
                  {{ t("确认处理", "Apply action") }}
                </button>
              </form>
              <p v-else class="hint">
                {{
                  t(
                    "当前没有可执行的处理操作。",
                    "No processing actions available.",
                  )
                }}
              </p>
              <div class="event-list">
                <div v-for="e in [...submission.events].reverse()" :key="e.id">
                  <strong
                    >{{
                      e.actor === "RESPONDENT"
                        ? t("填写者", "Respondent")
                        : e.actor
                    }}
                    · {{ processLabel(e.action) }}</strong
                  >
                  <p v-if="e.note">{{ e.note }}</p>
                  <time>{{ dateTime(e.createdAt) }}</time>
                </div>
              </div>
            </section>
          </div>
        </section>
        <section v-else-if="['forms', 'submissions'].includes(view)">
          <header class="page-heading">
            <div>
              <span class="eyebrow">{{
                view === "forms" ? "FORMS" : "RESPONSES"
              }}</span>
              <h1>{{ menuName(view) }}</h1>
              <p>
                {{
                  view === "forms"
                    ? t(
                        "设计、发布和管理收集入口",
                        "Design, publish and manage response access",
                      )
                    : t(
                        "查看提交、分派负责人并跟踪处理状态",
                        "Review submissions, assign owners and track progress",
                      )
                }}
              </p>
            </div>
            <div class="actions">
              <button :disabled="pending" @click="refresh">
                <RefreshCw :size="15" />{{ t("刷新", "Refresh") }}</button
              ><button
                v-if="view === 'forms' && has('forms_manage')"
                class="primary"
                :disabled="pending"
                @click="edit()"
              >
                <Plus :size="16" />{{ t("新建表单", "New form") }}
              </button>
            </div>
          </header>
          <div class="panel">
            <form class="toolbar" @submit.prevent="search">
              <input
                v-model="q"
                class="search-input"
                :placeholder="
                  view === 'forms'
                    ? t('搜索表单名称', 'Search form title')
                    : t('搜索编号或填写内容', 'Search ID or answers')
                "
                :aria-label="t('搜索', 'Search')"
              /><select
                v-model="state"
                :aria-label="t('状态', 'Status')"
                @change="search"
              >
                <option value="">{{ t("全部状态", "All states") }}</option>
                <option
                  v-for="s in view === 'forms'
                    ? ['DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED']
                    : [
                        'DRAFT',
                        'SUBMITTED',
                        'IN_PROGRESS',
                        'NEEDS_INFO',
                        'COMPLETED',
                        'REJECTED',
                      ]"
                  :key="s"
                  :value="s"
                >
                  {{ stateName(s) }}
                </option></select
              ><select
                v-if="view === 'forms'"
                v-model="sort"
                :aria-label="t('排序', 'Sort')"
                @change="search"
              >
                <option value="newest">
                  {{ t("最新创建", "Newest first") }}
                </option>
                <option value="title">
                  {{ t("名称排序", "Title") }}
                </option></select
              ><label v-else class="compact-label"
                >{{ t("表单编号", "Form ID")
                }}<input
                  v-model="formFilter"
                  type="number"
                  min="1"
                  style="width: 90px" /></label
              ><button>{{ t("查询", "Search") }}</button
              ><span class="count"
                >{{ view === "forms" ? forms.total : records.total }}
                {{ t("条", "records") }}</span
              >
            </form>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr v-if="view === 'forms'">
                    <th>{{ t("表单名称", "Form title") }}</th>
                    <th>{{ t("分类", "Category") }}</th>
                    <th>{{ t("状态", "Status") }}</th>
                    <th>{{ t("组织", "Organization") }}</th>
                    <th>{{ t("创建时间", "Created") }}</th>
                    <th></th>
                  </tr>
                  <tr v-else>
                    <th>{{ t("编号 / 表单", "ID / Form") }}</th>
                    <th>{{ t("发布版本", "Revision") }}</th>
                    <th>{{ t("状态", "Status") }}</th>
                    <th>{{ t("负责人", "Owner") }}</th>
                    <th>{{ t("更新时间", "Updated") }}</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <template v-if="view === 'forms'"
                    ><tr v-for="f in forms.items" :key="f.id">
                      <td>
                        <button
                          class="link-button title-link"
                          @click="openForm(f.id)"
                        >
                          {{ f.title }}</button
                        ><small class="row-note">#{{ f.id }}</small>
                      </td>
                      <td>{{ categoryName(f.category) }}</td>
                      <td>
                        <span class="badge" :class="f.status.toLowerCase()">{{
                          stateName(f.status)
                        }}</span>
                      </td>
                      <td>{{ departmentName(f.departmentId) }}</td>
                      <td>{{ dateTime(f.createdAt) }}</td>
                      <td>
                        <button @click="openForm(f.id)">
                          {{ t("查看", "Open") }}
                        </button>
                      </td>
                    </tr></template
                  ><template v-else
                    ><tr v-for="s in records.items" :key="s.id">
                      <td>
                        <button
                          class="link-button title-link"
                          @click="openSubmission(s.id)"
                        >
                          #{{ s.id }} · {{ s.title }}</button
                        ><small class="row-note"
                          >{{ t("表单", "Form") }} #{{ s.formId }}</small
                        >
                      </td>
                      <td>v{{ s.revision }}</td>
                      <td>
                        <span class="badge" :class="s.status.toLowerCase()">{{
                          stateName(s.status)
                        }}</span>
                      </td>
                      <td>{{ actorName(s.assigneeId) }}</td>
                      <td>{{ dateTime(s.updatedAt) }}</td>
                      <td>
                        <button @click="openSubmission(s.id)">
                          {{ t("查看", "Open") }}
                        </button>
                      </td>
                    </tr></template
                  >
                  <tr
                    v-if="
                      !(view === 'forms' ? forms.items : records.items).length
                    "
                  >
                    <td colspan="6" class="empty">
                      <FileText :size="28" /><strong>{{
                        t("暂无记录", "No records")
                      }}</strong>
                      <p>
                        {{
                          view === "forms"
                            ? t(
                                "创建一份表单，发布后即可开始收集。",
                                "Create and publish a form to start collecting responses.",
                              )
                            : t(
                                "发布表单后，填写者提交的内容会出现在这里。",
                                "Responses appear here after respondents start filling a published form.",
                              )
                        }}
                      </p>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="pager">
              <span>{{ page }} / {{ maxPage }}</span
              ><button :disabled="page <= 1" @click="turn(-1)">
                {{ t("上一页", "Previous") }}</button
              ><button :disabled="page >= maxPage" @click="turn(1)">
                {{ t("下一页", "Next") }}
              </button>
            </div>
          </div>
        </section>
        <section v-else-if="view === 'reports'">
          <header class="page-heading">
            <div>
              <span class="eyebrow">OVERVIEW</span>
              <h1>{{ t("收集统计", "Collection overview") }}</h1>
              <p>
                {{
                  t(
                    "草稿与正式提交分开统计",
                    "Drafts counted separately from submitted responses",
                  )
                }}
              </p>
            </div>
            <button @click="refresh">{{ t("刷新", "Refresh") }}</button>
          </header>
          <template v-if="report"
            ><div class="metric-grid">
              <div>
                <span>{{ t("表单", "Forms") }}</span
                ><strong>{{ report.forms }}</strong>
              </div>
              <div>
                <span>{{ t("收集中", "Published") }}</span
                ><strong>{{ report.published }}</strong>
              </div>
              <div>
                <span>{{ t("待处理", "Submitted") }}</span
                ><strong>{{ report.states.SUBMITTED }}</strong>
              </div>
              <div>
                <span>{{ t("待补充", "Needs information") }}</span
                ><strong>{{ report.states.NEEDS_INFO }}</strong>
              </div>
              <div>
                <span>{{ t("已完成", "Completed") }}</span
                ><strong>{{ report.states.COMPLETED }}</strong>
              </div>
            </div>
            <div class="report-grid">
              <section class="panel">
                <h2>{{ t("填写记录状态", "Response states") }}</h2>
                <div
                  v-for="(count, s) in report.states"
                  :key="s"
                  class="summary-row"
                >
                  <span>{{ stateName(s) }}</span
                  ><strong>{{ count }}</strong>
                </div>
              </section>
              <section class="panel">
                <h2>{{ t("各表单正式提交", "Submitted by form") }}</h2>
                <div v-for="f in report.byForm" :key="f.id" class="summary-row">
                  <span>{{ f.title }}</span
                  ><strong>{{ f.responses }}</strong>
                </div>
                <p v-if="!report.byForm.length" class="empty">
                  {{ t("暂无表单", "No forms") }}
                </p>
              </section>
            </div></template
          >
        </section>
        <section v-else-if="view === 'audit'">
          <header class="page-heading">
            <div>
              <h1>{{ t("操作记录", "Audit trail") }}</h1>
              <p>
                {{
                  t(
                    "记录操作身份与对象，不记录填写答案和私有回执",
                    "Actor and object records without answers or private receipts",
                  )
                }}
              </p>
            </div>
            <button @click="refresh">{{ t("刷新", "Refresh") }}</button>
          </header>
          <div class="panel table-wrap">
            <table>
              <thead>
                <tr>
                  <th>{{ t("时间", "Time") }}</th>
                  <th>{{ t("操作者", "Actor") }}</th>
                  <th>{{ t("操作", "Action") }}</th>
                  <th>{{ t("对象", "Object") }}</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="a in audit.slice(0, 200)" :key="a.id">
                  <td>{{ dateTime(a.createdAt) }}</td>
                  <td>{{ a.actor }}</td>
                  <td>{{ auditName(a.action) }}</td>
                  <td>{{ a.objectId }}</td>
                </tr>
                <tr v-if="!audit.length">
                  <td colspan="4" class="empty">
                    {{ t("暂无操作", "No events") }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <p class="hint">
            {{ t("展示最近 200 条操作。", "Shows the latest 200 events.") }}
          </p>
        </section>
      </main>
    </div>
  </div>
  <div v-if="publishDialog" class="modal-backdrop">
    <form class="modal compact" @submit.prevent="publish">
      <header>
        <h2>{{ t("发布新版本", "Publish a version") }}</h2>
        <button type="button" :disabled="pending" @click="publishDialog = null">
          <X :size="18" />
        </button>
      </header>
      <p class="hint">
        {{
          t(
            "新的填写者使用当前草稿；已保存记录仍使用原版本。",
            "New respondents use this draft. Existing responses keep their original version.",
          )
        }}
      </p>
      <label
        >{{
          t(
            "截止时间（可留空，按本机时区）",
            "Deadline (optional, local time)",
          )
        }}<input v-model="publishDialog.closesAt" type="datetime-local"
      /></label>
      <div class="modal-actions">
        <button type="button" :disabled="pending" @click="publishDialog = null">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" :disabled="pending">
          {{ t("确认发布", "Publish") }}
        </button>
      </div>
    </form>
  </div>
  <div v-if="passwordForm" class="modal-backdrop">
    <form class="modal compact" @submit.prevent="changePassword">
      <header>
        <h2>{{ t("修改密码", "Change password") }}</h2>
        <button type="button" @click="passwordForm = null">
          <X :size="18" />
        </button>
      </header>
      <label
        >{{ t("原密码", "Current password")
        }}<input
          v-model="passwordForm.oldPassword"
          type="password"
          required
          autocomplete="current-password" /></label
      ><label
        >{{ t("新密码", "New password")
        }}<input
          v-model="passwordForm.newPassword"
          type="password"
          required
          autocomplete="new-password"
      /></label>
      <p class="hint">
        {{
          t(
            "12–72 字节，含大小写字母和数字。修改后需重新登录。",
            "12–72 bytes with upper/lowercase letters and a digit. Sign in again after changing.",
          )
        }}
      </p>
      <button class="primary full" :disabled="pending">
        {{ t("保存并退出", "Save and sign out") }}
      </button>
    </form>
  </div>
  <div v-if="about" class="modal-backdrop">
    <section class="modal contact-modal">
      <header>
        <h2>{{ t("关于 FormDesk", "About FormDesk") }}</h2>
        <button @click="about = false"><X :size="18" /></button>
      </header>
      <img class="contact-logo" src="/brand/logo.jpg" alt="知华科技" />
      <p>
        {{
          t(
            "知华科技（上海如静知华信息科技有限公司）",
            "ZhiHua Technology (Shanghai Rujing Zhihua Information Technology Co., Ltd.)",
          )
        }}
      </p>
      <p>
        {{
          t(
            "公开源码学习版，未经书面授权不得商用。",
            "Source-available learning edition. Commercial use requires written authorization.",
          )
        }}
      </p>
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener"
        >https://www.zhuatech.cn/</a
      ><template v-if="language === 'zh'"
        ><p>商业授权或深度定制开发请联系知华科技</p>
        <div class="qr-grid">
          <figure>
            <img src="/brand/wechat-zhuatech.png" alt="微信 zhuatech" />
            <figcaption>微信 zhuatech</figcaption>
          </figure>
          <figure>
            <img src="/brand/wechat-zhuatech2.png" alt="微信 zhuatech2" />
            <figcaption>微信 zhuatech2</figcaption>
          </figure>
        </div></template
      >
      <div v-else class="contact-links">
        <a href="mailto:han@zhuatech.cn">han@zhuatech.cn</a
        ><a href="mailto:jack@zhuatech.cn">jack@zhuatech.cn</a
        ><a href="https://wa.me/8617521234993" target="_blank" rel="noopener"
          >WhatsApp +86 17521234993</a
        >
      </div>
    </section>
  </div>
  <div v-if="confirmation" class="modal-backdrop confirmation-layer">
    <section class="modal compact" role="alertdialog" aria-modal="true">
      <h2>{{ t("确认操作", "Confirm action") }}</h2>
      <p>{{ confirmation.message }}</p>
      <div class="modal-actions">
        <button @click="answerConfirmation(false)">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" @click="answerConfirmation(true)">
          {{ t("确认", "Confirm") }}
        </button>
      </div>
    </section>
  </div>
</template>
