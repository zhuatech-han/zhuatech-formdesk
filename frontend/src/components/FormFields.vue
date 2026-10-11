<!-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2 -->
<script setup>
import { t, visibleField } from "../ui.js";
const props = defineProps({
  fields: { type: Array, required: true },
  answers: { type: Object, required: true },
  attachments: { type: Array, default: () => [] },
  disabled: Boolean,
  preview: Boolean,
});
const emit = defineEmits(["update:answers", "upload", "remove", "download"]);
function change(key, value) {
  emit("update:answers", { ...props.answers, [key]: value });
}
function toggle(key, value, checked) {
  const current = props.answers[key] || [];
  change(
    key,
    checked ? [...current, value] : current.filter((x) => x !== value),
  );
}
</script>
<template>
  <div class="form-fields">
    <template v-for="f in fields" :key="f.key"
      ><div v-if="visibleField(f, answers)" class="answer-field">
        <label :for="'answer-' + f.key"
          >{{ f.label }}
          <span v-if="f.required" class="required">*</span></label
        >
        <p v-if="f.help" class="field-help">{{ f.help }}</p>
        <textarea
          v-if="f.type === 'TEXTAREA'"
          :id="'answer-' + f.key"
          :value="answers[f.key] || ''"
          :disabled="disabled"
          maxlength="6000"
          rows="4"
          @input="change(f.key, $event.target.value)"
        ></textarea>
        <select
          v-else-if="f.type === 'SELECT'"
          :id="'answer-' + f.key"
          :value="answers[f.key] || ''"
          :disabled="disabled"
          @change="change(f.key, $event.target.value)"
        >
          <option value="">{{ t("请选择", "Choose an option") }}</option>
          <option v-for="o in f.options" :key="o">{{ o }}</option>
        </select>
        <fieldset
          v-else-if="f.type === 'MULTI'"
          :id="'answer-' + f.key"
          :aria-label="f.label"
        >
          <label v-for="o in f.options" :key="o" class="check"
            ><input
              type="checkbox"
              :checked="(answers[f.key] || []).includes(o)"
              :disabled="disabled"
              @change="toggle(f.key, o, $event.target.checked)"
            />{{ o }}</label
          >
        </fieldset>
        <div v-else-if="f.type === 'FILE'" class="file-field">
          <input
            :id="'answer-' + f.key"
            type="file"
            accept=".pdf,.png,.jpg,.jpeg"
            :disabled="disabled || preview"
            @change="
              emit('upload', f.key, $event.target.files[0]);
              $event.target.value = '';
            "
          /><small>{{
            t(
              "PDF / PNG / JPEG，每个文件最多 5 MB",
              "PDF / PNG / JPEG, up to 5 MB each",
            )
          }}</small>
          <div
            v-for="a in attachments.filter((a) => a.fieldKey === f.key)"
            :key="a.id"
            class="attachment"
          >
            <button
              type="button"
              class="link-button"
              @click="emit('download', a)"
            >
              {{ a.filename }}</button
            ><span>{{ Math.ceil(a.size / 1024) }} KB</span
            ><button v-if="!disabled" type="button" @click="emit('remove', a)">
              {{ t("移除", "Remove") }}
            </button>
          </div>
        </div>
        <input
          v-else
          :id="'answer-' + f.key"
          :type="
            { EMAIL: 'email', NUMBER: 'number', DATE: 'date' }[f.type] || 'text'
          "
          :value="answers[f.key] || ''"
          :disabled="disabled"
          maxlength="1000"
          step="any"
          @input="change(f.key, $event.target.value)"
        /></div
    ></template>
  </div>
</template>
