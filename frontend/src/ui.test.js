// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
import test from "node:test";
import assert from "node:assert/strict";
import {
  canEditResponse,
  visibleField,
  newField,
  cloneRecord,
  language,
  stateName,
} from "./ui.js";
test("condition uses equality and hidden fields are not shown", () => {
  const f = { whenKey: "choice", whenValue: "A" };
  assert.equal(visibleField(f, { choice: "B" }), false);
  assert.equal(visibleField(f, { choice: "A" }), true);
  assert.equal(visibleField({ whenKey: "" }, {}), true);
});
test("new field keys are independent and stable copies do not mutate source", () => {
  const a = newField(),
    b = newField();
  assert.notEqual(a.key, b.key);
  const copy = cloneRecord(a);
  copy.label = "TEST";
  assert.equal(a.label, "");
  assert.match(a.key, /^[a-z][a-z0-9_]{0,39}$/);
});
test("status names switch language without changing state codes", () => {
  language.value = "en";
  assert.equal(stateName("NEEDS_INFO"), "Needs information");
  language.value = "zh";
  assert.equal(stateName("NEEDS_INFO"), "待补充");
});

test("closed draft and submitted receipts stay read-only", () => {
  assert.equal(canEditResponse(null), true);
  assert.equal(canEditResponse({ status: "DRAFT", accepting: true }), true);
  assert.equal(canEditResponse({ status: "DRAFT", accepting: false }), false);
  assert.equal(
    canEditResponse({ status: "SUBMITTED", accepting: true }),
    false,
  );
});
