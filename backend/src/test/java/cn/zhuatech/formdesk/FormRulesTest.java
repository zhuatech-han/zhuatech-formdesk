// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

/** 字段条件、类型、输入和CSV边界测试。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
class FormRulesTest {
  Map<String, Object> field(String key, String type, boolean required) {
    return new LinkedHashMap<>(
        Map.of(
            "key",
            key,
            "type",
            type,
            "label",
            key,
            "help",
            "",
            "required",
            required,
            "options",
            List.of("A", "B"),
            "whenKey",
            "",
            "whenValue",
            ""));
  }

  List<Map<String, Object>> schema(String type) {
    return FormRules.schema(List.of(field("value", type, true)));
  }

  void rejects(String type, Object value) {
    assertThrows(
        Problem.class,
        () -> FormRules.answers(schema(type), Map.of("value", value), true, Map.of()));
  }

  @Test
  void duplicateKeysRejected() {
    assertThrows(
        Problem.class,
        () -> FormRules.schema(List.of(field("x", "TEXT", false), field("x", "TEXT", false))));
  }

  @Test
  void emptySchemaRejected() {
    assertThrows(Problem.class, () -> FormRules.schema(List.of()));
  }

  @Test
  void unknownTypeRejected() {
    assertThrows(Problem.class, () -> schema("HTML"));
  }

  @Test
  void conditionalRequiredOnlyWhenVisible() {
    var select = field("choice", "SELECT", true);
    var detail = field("detail", "TEXT", true);
    detail.put("whenKey", "choice");
    detail.put("whenValue", "A");
    var fs = FormRules.schema(List.of(select, detail));
    assertFalse(
        FormRules.answers(fs, Map.of("choice", "B", "detail", "hidden"), true, Map.of())
            .containsKey("detail"));
    assertThrows(Problem.class, () -> FormRules.answers(fs, Map.of("choice", "A"), true, Map.of()));
  }

  @Test
  void forwardConditionRejected() {
    var f = field("detail", "TEXT", false);
    f.put("whenKey", "later");
    f.put("whenValue", "A");
    assertThrows(
        Problem.class, () -> FormRules.schema(List.of(f, field("later", "SELECT", false))));
  }

  @Test
  void emailInvalid() {
    rejects("EMAIL", "a@@b.c");
  }

  @Test
  void numberInvalid() {
    rejects("NUMBER", "NaN");
  }

  @Test
  void numberTooPrecise() {
    rejects("NUMBER", "0.1234567");
  }

  @Test
  void dateInvalid() {
    rejects("DATE", "2026-02-30");
  }

  @Test
  void optionInvalid() {
    rejects("SELECT", "Z");
  }

  @Test
  void multiDuplicatesRejected() {
    rejects("MULTI", List.of("A", "A"));
  }

  @Test
  void multiUnknownRejected() {
    rejects("MULTI", List.of("Z"));
  }

  @Test
  void requiredFile() {
    assertThrows(Problem.class, () -> FormRules.answers(schema("FILE"), Map.of(), true, Map.of()));
    assertTrue(FormRules.answers(schema("FILE"), Map.of(), true, Map.of("value", 1L)).isEmpty());
  }

  @Test
  void invalidValueTypes() {
    rejects("TEXT", Map.of("object", 1));
  }

  @Test
  void csvControlFormula() {
    assertTrue(Rules.csv("\t\u200b=2").startsWith("\"'"));
    assertEquals("\"a\"\"b\"", Rules.csv("a\"b"));
  }

  @Test
  void passwordByteBounds() {
    assertThrows(Problem.class, () -> AdminService.validatePassword("Aa9" + "中".repeat(24)));
  }

  @Test
  void limiterIsBounded() {
    var l = new PublicLimiter();
    for (int i = 0; i < 60; i++) l.check("TEST");
    assertThrows(Problem.class, () -> l.check("TEST"));
  }
}
