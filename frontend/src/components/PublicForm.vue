<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { api, download } from "../api.js";
import {
  language,
  t,
  stateName,
  errorMessage,
  dateTime,
  canEditResponse,
} from "../ui.js";
import FormFields from "./FormFields.vue";
const props = defineProps({
  formKey: { type: String, default: "" },
  receiptKey: { type: String, default: "" },
});
const revision = ref(null),
  response = ref(null),
  answers = ref({}),
  consented = ref(false),
  token = ref(props.receiptKey),
  ready = ref(false),
  pending = ref(false),
  notice = ref(null),
  resumeLink = ref("");
const editable = computed(() => canEditResponse(response.value));
function headers() {
  return { "X-Receipt-Key": token.value };
}
function apply(r) {
  response.value = r;
  revision.value = r.revision;
  answers.value = r.answers;
  consented.value = r.consented;
  resumeLink.value = location.origin + "/resume#" + token.value;
}
async function run(fn) {
  if (pending.value) return;
  pending.value = true;
  notice.value = null;
  try {
    await fn();
  } catch (e) {
    notice.value = { type: "error", text: errorMessage(e) };
  } finally {
    pending.value = false;
  }
}
onMounted(async () => {
  await run(async () => {
    if (token.value)
      apply(await api("/public/response", { headers: headers() }));
    else {
      if (!props.formKey) throw new Error("RECEIPT_INVALID");
      revision.value = await api("/public/forms/" + props.formKey);
    }
  });
  ready.value = true;
});
/** 保存创建后的密钥到URL fragment，避免服务器请求日志和外链referrer泄漏。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
async function ensure() {
  if (token.value) return;
  const original = { ...answers.value },
    consent = consented.value;
  const r = await api("/public/forms/" + props.formKey + "/begin", {
    method: "POST",
    body: {},
  });
  token.value = r.token;
  apply(r.response);
  answers.value = original;
  consented.value = consent;
  history.replaceState(null, "", "/resume#" + token.value);
}
async function save(submit) {
  await run(async () => {
    await ensure();
    apply(
      await api("/public/response" + (submit ? "/submit" : ""), {
        method: submit ? "POST" : "PUT",
        headers: headers(),
        body: {
          version: response.value.version,
          answers: answers.value,
          consented: consented.value,
        },
      }),
    );
    notice.value = {
      type: "success",
      text: submit
        ? t(
            "已提交，请保留回执链接查看处理状态。",
            "Submitted. Keep your receipt link to check the status.",
          )
        : t(
            "进度已保存。请保留恢复链接，之后可继续填写。",
            "Progress saved. Keep your resume link to continue later.",
          ),
    };
  });
}
async function refresh() {
  await run(async () =>
    apply(await api("/public/response", { headers: headers() })),
  );
}
async function upload(field, file) {
  if (!file) return;
  await run(async () => {
    await ensure();
    const original = { ...answers.value },
      consent = consented.value;
    const body = new FormData();
    body.append("file", file);
    body.append("field", field);
    body.append("version", response.value.version);
    apply(
      await api("/public/response/files", {
        method: "POST",
        headers: headers(),
        body,
      }),
    );
    answers.value = original;
    consented.value = consent;
  });
}
async function remove(a) {
  await run(async () => {
    const original = { ...answers.value },
      consent = consented.value;
    apply(
      await api(
        "/public/response/files/" + a.id + "?version=" + response.value.version,
        { method: "DELETE", headers: headers() },
      ),
    );
    answers.value = original;
    consented.value = consent;
  });
}
async function downloadFile(a) {
  await run(() =>
    download("/public/response/files/" + a.id, a.filename, {
      headers: headers(),
    }),
  );
}
async function copy() {
  await run(async () => {
    try {
      await navigator.clipboard.writeText(resumeLink.value);
    } catch {
      throw new Error("CLIPBOARD_FAILED");
    }
    notice.value = {
      type: "success",
      text: t("链接已复制，请妥善保管。", "Link copied. Keep it private."),
    };
  });
}
</script>
<template>
  <div class="public-page">
    <header class="public-top">
      <a
        href="https://www.zhuatech.cn/"
        target="_blank"
        rel="noopener"
        class="brand"
        ><img src="/brand/logo.jpg" alt="知华科技" />FormDesk</a
      ><button @click="language = language === 'zh' ? 'en' : 'zh'">
        {{ language === "zh" ? "English" : "中文" }}
      </button>
    </header>
    <main class="public-card">
      <p v-if="!ready" class="empty">{{ t("正在加载…", "Loading…") }}</p>
      <div v-if="notice" class="notice" :class="notice.type" role="status">
        {{ notice.text }}
      </div>
      <template v-if="revision"
        ><header class="public-heading">
          <span v-if="response" class="badge"
            >#{{ response.id }} · {{ stateName(response.status) }}</span
          >
          <h1>{{ revision.title }}</h1>
          <p class="description">{{ revision.description }}</p>
          <small
            >{{ t("版本", "Version") }} {{ revision.number
            }}<template v-if="response">
              · {{ t("更新于", "Updated") }}
              {{ dateTime(response.updatedAt) }}</template
            ></small
          >
        </header>
        <div v-if="response?.status === 'NEEDS_INFO'" class="inline-note">
          <strong>{{
            t("请补充以下内容", "More information requested")
          }}</strong>
          <p>{{ response.requestNote }}</p>
        </div>
        <form novalidate @submit.prevent="save(true)">
          <FormFields
            :fields="revision.fields"
            v-model:answers="answers"
            :attachments="response?.attachments || []"
            :disabled="pending || !editable"
            @upload="upload"
            @remove="remove"
            @download="downloadFile"
          />
          <div class="consent-block">
            <p>{{ revision.consentText }}</p>
            <label class="check"
              ><input
                v-model="consented"
                type="checkbox"
                :disabled="pending || !editable"
              />{{
                t(
                  "已阅读并同意上述收集说明",
                  "I have read and agree to this collection notice",
                )
              }}</label
            >
          </div>
          <div v-if="editable" class="public-actions">
            <button type="button" :disabled="pending" @click="save(false)">
              {{ t("保存进度", "Save progress") }}</button
            ><button class="primary" type="submit" :disabled="pending">
              {{
                pending
                  ? t("处理中…", "Working…")
                  : response?.status === "NEEDS_INFO"
                    ? t("提交补充", "Submit additional information")
                    : t("提交", "Submit")
              }}
            </button>
          </div>
          <div v-else class="public-actions">
            <p class="muted">
              {{
                t(
                  "当前记录已锁定，如需修改请联系发布者。",
                  "This response is locked. Contact the publisher to request changes.",
                )
              }}
            </p>
            <button type="button" :disabled="pending" @click="refresh">
              {{ t("刷新状态", "Refresh status") }}
            </button>
          </div>
        </form>
        <div v-if="resumeLink" class="receipt">
          <strong>{{
            t("私有回执／恢复链接", "Private receipt / resume link")
          }}</strong>
          <p>
            {{
              t(
                "持有此链接的人可以查看此记录；请勿公开分享。丢失后无法匿名找回。",
                "Anyone with this link can view this response. Keep it private; it cannot be recovered anonymously if lost.",
              )
            }}
          </p>
          <div class="link-row">
            <input
              :value="resumeLink"
              readonly
              :aria-label="t('恢复链接', 'Resume link')"
            /><button type="button" :disabled="pending" @click="copy">
              {{ t("复制", "Copy") }}
            </button>
          </div>
        </div> </template
      ><a v-if="ready && !revision" href="/">{{
        t("返回首页", "Return home")
      }}</a>
    </main>
    <footer class="public-footer">
      <a href="https://www.zhuatech.cn/" target="_blank" rel="noopener">{{
        t("知华科技", "ZhiHua Technology")
      }}</a>
      ·
      {{
        t(
          "公开源码学习版 · 商用须授权",
          "Source-available learning edition · Commercial use requires authorization",
        )
      }}
    </footer>
  </div>
</template>
