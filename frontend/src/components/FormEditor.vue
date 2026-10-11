<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { ref, computed, onMounted } from "vue";
import { ArrowUp, ArrowDown, Trash2, Plus } from "@lucide/vue";
import { t, types, typeName, newField, cloneRecord, ask } from "../ui.js";
import { api } from "../api.js";
import FormFields from "./FormFields.vue";
const props = defineProps({
  id: { type: Number, default: null },
  options: { type: Object, required: true },
  profile: { type: Object, required: true },
  pending: Boolean,
  perform: { type: Function, required: true },
});
const emit = defineEmits(["saved", "cancel", "error"]);
const row = ref({
    title: "",
    description: "",
    category: "ENQUIRY",
    departmentId: props.profile.departmentId,
    consentText: "",
    fields: [newField()],
  }),
  selected = ref(0),
  tab = ref("edit"),
  previewAnswers = ref({}),
  initial = ref(""),
  ready = ref(false);
const current = computed(() => row.value.fields[selected.value]);
const parents = computed(() =>
  row.value.fields
    .slice(0, selected.value)
    .filter((f) => f.type === "SELECT" && !f.whenKey),
);
onMounted(async () => {
  try {
    if (props.id) {
      const d = await api("/forms/" + props.id);
      row.value = { ...d.form, fields: d.fields };
    }
    initial.value = JSON.stringify(row.value);
    ready.value = true;
  } catch (e) {
    emit("error", e);
  }
});
async function canLeave() {
  return (
    JSON.stringify(row.value) === initial.value ||
    (await ask(t("放弃尚未保存的修改？", "Discard unsaved changes?")))
  );
}
defineExpose({ canLeave });
function add() {
  row.value.fields.push(newField());
  selected.value = row.value.fields.length - 1;
}
async function remove() {
  if (
    await ask(
      t(
        "删除此字段？请检查引用它的条件。",
        "Delete this field? Check conditions that reference it.",
      ),
    )
  ) {
    row.value.fields.splice(selected.value, 1);
    selected.value = Math.max(0, selected.value - 1);
  }
}
function move(d) {
  const i = selected.value,
    j = i + d;
  if (j < 0 || j >= row.value.fields.length) return;
  [row.value.fields[i], row.value.fields[j]] = [
    row.value.fields[j],
    row.value.fields[i],
  ];
  selected.value = j;
}
async function save() {
  const v = await props.perform(
    "/forms" + (props.id ? "/" + props.id : ""),
    props.id ? "PUT" : "POST",
    cloneRecord(row.value),
  );
  if (v) {
    initial.value = JSON.stringify(row.value);
    emit("saved", v);
  }
}
async function cancel() {
  if (await canLeave()) emit("cancel");
}
</script>
<template>
  <section>
    <header class="page-heading">
      <div>
        <h1>
          {{ id ? t("编辑表单", "Edit form") : t("新建表单", "New form") }}
        </h1>
        <p>
          {{
            t(
              "修改保存到草稿，发布后才会更新新填写者看到的内容。",
              "Changes remain a draft until published. Existing responses keep their version.",
            )
          }}
        </p>
      </div>
      <div class="actions">
        <button :disabled="pending" @click="cancel">
          {{ t("取消", "Cancel") }}</button
        ><button class="primary" :disabled="pending || !ready" @click="save">
          {{ t("保存草稿", "Save draft") }}
        </button>
      </div>
    </header>
    <div v-if="ready" class="editor-layout">
      <div class="editor-main">
        <section class="panel form-meta">
          <label
            >{{ t("表单名称", "Form title")
            }}<input v-model="row.title" maxlength="160" required
          /></label>
          <div class="form-grid">
            <label
              >{{ t("所属组织", "Organization")
              }}<select v-model="row.departmentId" :disabled="!!id">
                <option
                  v-for="d in options.departments.filter((d) => d.enabled)"
                  :key="d.id"
                  :value="d.id"
                >
                  {{ d.name }}
                </option>
              </select></label
            ><label
              >{{ t("分类", "Category")
              }}<select v-model="row.category">
                <option
                  v-for="c in options.categories.filter((c) => c.enabled)"
                  :key="c.id"
                  :value="c.code"
                >
                  {{ t(c.name, c.nameEn) }}
                </option>
              </select></label
            >
          </div>
          <label
            >{{ t("说明", "Description")
            }}<textarea
              v-model="row.description"
              rows="2"
              maxlength="6000"
            ></textarea></label
          ><label
            >{{ t("收集说明与同意文本", "Collection and consent notice")
            }}<textarea
              v-model="row.consentText"
              rows="3"
              maxlength="3000"
              :placeholder="
                t(
                  '说明用途、收集主体、保存期限和联系入口',
                  'Explain purpose, publisher, retention and contact details',
                )
              "
            ></textarea>
          </label>
        </section>
        <nav class="sub-tabs">
          <button :class="{ active: tab === 'edit' }" @click="tab = 'edit'">
            {{ t("字段", "Fields") }}</button
          ><button
            :class="{ active: tab === 'preview' }"
            @click="tab = 'preview'"
          >
            {{ t("填写预览", "Response preview") }}
          </button>
        </nav>
        <section v-if="tab === 'edit'" class="panel field-list">
          <button
            v-for="(f, i) in row.fields"
            :key="f.key"
            :class="['field-item', { active: selected === i }]"
            @click="selected = i"
          >
            <span class="field-index">{{ String(i + 1).padStart(2, "0") }}</span
            ><span
              ><strong>{{
                f.label || t("未命名字段", "Untitled field")
              }}</strong
              ><small
                >{{ typeName(f.type)
                }}<template v-if="f.required">
                  · {{ t("必填", "Required") }}</template
                ><template v-if="f.whenKey">
                  · {{ t("有条件", "Conditional") }}</template
                ></small
              ></span
            ></button
          ><button
            class="add-field"
            :disabled="row.fields.length >= 40"
            @click="add"
          >
            <Plus :size="16" />{{ t("添加字段", "Add field") }} ·
            {{ row.fields.length }}/40
          </button>
        </section>
        <section v-else class="panel preview-panel">
          <h2>{{ row.title || t("未命名表单", "Untitled form") }}</h2>
          <p class="description">{{ row.description }}</p>
          <FormFields
            :fields="row.fields"
            v-model:answers="previewAnswers"
            preview
          />
          <p class="hint">
            {{
              t(
                "预览不会提交记录，附件上传在发布后可用。",
                "Preview does not submit responses. Uploads are available after publication.",
              )
            }}
          </p>
        </section>
      </div>
      <aside v-if="tab === 'edit' && current" class="panel field-settings">
        <div class="section-heading">
          <h2>{{ t("字段设置", "Field settings") }}</h2>
          <div class="actions">
            <button
              :disabled="selected === 0"
              :aria-label="t('上移', 'Move up')"
              @click="move(-1)"
            >
              <ArrowUp :size="15" /></button
            ><button
              :disabled="selected === row.fields.length - 1"
              :aria-label="t('下移', 'Move down')"
              @click="move(1)"
            >
              <ArrowDown :size="15" /></button
            ><button
              :aria-label="t('删除字段', 'Delete field')"
              @click="remove"
            >
              <Trash2 :size="15" />
            </button>
          </div>
        </div>
        <label
          >{{ t("名称", "Label")
          }}<input v-model="current.label" maxlength="120" /></label
        ><label
          >{{ t("类型", "Type")
          }}<select v-model="current.type">
            <option v-for="(v, k) in types" :key="k" :value="k">
              {{ t(...v) }}
            </option>
          </select></label
        ><label
          >{{ t("稳定代码", "Stable key")
          }}<input
            v-model="current.key"
            maxlength="40"
            pattern="[a-z][a-z0-9_]{0,39}" /></label
        ><label
          >{{ t("填写提示", "Help text")
          }}<textarea
            v-model="current.help"
            rows="2"
            maxlength="500"
          ></textarea></label
        ><label class="check"
          ><input v-model="current.required" type="checkbox" />{{
            t("必填", "Required")
          }}</label
        ><label v-if="['SELECT', 'MULTI'].includes(current.type)"
          >{{ t("选项（每行一个）", "Choices (one per line)")
          }}<textarea
            :value="current.options.join('\n')"
            rows="5"
            @input="current.options = $event.target.value.split('\n')"
          ></textarea>
        </label>
        <hr />
        <h3>{{ t("显示条件", "Display condition") }}</h3>
        <label
          >{{ t("当以下字段", "When this field")
          }}<select v-model="current.whenKey" @change="current.whenValue = ''">
            <option value="">{{ t("始终显示", "Always visible") }}</option>
            <option v-for="f in parents" :key="f.key" :value="f.key">
              {{ f.label }}
            </option>
          </select></label
        ><label v-if="current.whenKey"
          >{{ t("等于", "Equals")
          }}<select v-model="current.whenValue">
            <option value="">{{ t("请选择", "Choose") }}</option>
            <option
              v-for="v in parents.find((f) => f.key === current.whenKey)
                ?.options || []"
              :key="v"
            >
              {{ v }}
            </option>
          </select></label
        >
      </aside>
    </div>
    <p v-else-if="!ready" class="empty">{{ t("正在加载…", "Loading…") }}</p>
  </section>
</template>
