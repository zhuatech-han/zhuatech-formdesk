// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** 版本化字段模型和条件显示规则；服务端独立验证填写。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
public final class FormRules {
  static final Set<String> TYPES =
      Set.of("TEXT", "TEXTAREA", "EMAIL", "NUMBER", "DATE", "SELECT", "MULTI", "FILE");

  /** 字段唯一、条件只引用前面的单选字段，选项与文本有界。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static List<Map<String, Object>> schema(Object raw) {
    if (!(raw instanceof List<?> rows) || rows.isEmpty() || rows.size() > 40)
      throw new Problem(400, "SCHEMA_INVALID");
    var out = new ArrayList<Map<String, Object>>();
    var prior = new HashMap<String, Map<String, Object>>();
    for (var item : rows) {
      if (!(item instanceof Map<?, ?> b)) throw new Problem(400, "SCHEMA_INVALID");
      String key = Rules.text(b.get("key"), 40, true), type = Rules.text(b.get("type"), 20, true);
      Rules.check(
          key.matches("[a-z][a-z0-9_]{0,39}") && !prior.containsKey(key) && TYPES.contains(type),
          "SCHEMA_INVALID");
      var f = new LinkedHashMap<String, Object>();
      f.put("key", key);
      f.put("type", type);
      f.put("label", Rules.text(b.get("label"), 120, true));
      f.put("help", Rules.paragraph(b.get("help"), 500, false));
      f.put("required", Rules.flag(b.get("required")));
      var options = new ArrayList<String>();
      if (Set.of("SELECT", "MULTI").contains(type)) {
        if (!(b.get("options") instanceof List<?> opts) || opts.isEmpty() || opts.size() > 30)
          throw new Problem(400, "SCHEMA_INVALID");
        for (var o : opts) {
          var value = Rules.text(o, 120, true);
          Rules.check(!options.contains(value), "SCHEMA_INVALID");
          options.add(value);
        }
      }
      f.put("options", options);
      String whenKey = Rules.text(b.get("whenKey"), 40, false),
          whenValue = Rules.text(b.get("whenValue"), 120, false);
      if (!whenKey.isEmpty()) {
        var parent = prior.get(whenKey);
        Rules.check(
            parent != null
                && parent.get("type").equals("SELECT")
                && ((List<?>) parent.get("options")).contains(whenValue)
                && parent.get("whenKey").equals(""),
            "CONDITION_INVALID");
      } else Rules.check(whenValue.isEmpty(), "CONDITION_INVALID");
      f.put("whenKey", whenKey);
      f.put("whenValue", whenValue);
      prior.put(key, f);
      out.add(f);
    }
    return out;
  }

  /** 公开条件显示和后端条件校验一致。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static boolean visible(Map<String, Object> f, Map<String, Object> answers) {
    return f.get("whenKey").equals("")
        || Objects.equals(answers.get(f.get("whenKey")), f.get("whenValue"));
  }

  /** 未显示字段丢弃，不接收未知字段；草稿可缺项，正式提交校验必填。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public static Map<String, Object> answers(
      List<Map<String, Object>> fields, Object raw, boolean submit, Map<String, Long> files) {
    if (!(raw instanceof Map<?, ?> map)) throw new Problem(400, "INVALID_INPUT");
    var allowed = new HashSet<String>();
    for (var f : fields) allowed.add((String) f.get("key"));
    if (map.keySet().stream().anyMatch(k -> !allowed.contains(k)))
      throw new Problem(400, "UNKNOWN_FIELD");
    var result = new LinkedHashMap<String, Object>();
    for (var f : fields) {
      if (!visible(f, result)) continue;
      String key = (String) f.get("key"), type = (String) f.get("type");
      Object v = map.get(key);
      boolean empty = v == null || v.equals("") || (v instanceof List<?> a && a.isEmpty());
      if (type.equals("FILE")) {
        empty = files.getOrDefault(key, 0L) == 0;
      }
      if (submit && (boolean) f.get("required") && empty) throw new Problem(400, "REQUIRED_FIELD");
      if (empty || type.equals("FILE")) continue;
      if (type.equals("MULTI")) {
        if (!(v instanceof List<?> list)
            || list.size() > 30
            || new HashSet<>(list).size() != list.size()
            || !((List<?>) f.get("options")).containsAll(list))
          throw new Problem(400, "OPTION_INVALID");
        result.put(key, list);
        continue;
      }
      if (!(v instanceof String) && !(type.equals("NUMBER") && v instanceof Number))
        throw new Problem(400, "INVALID_INPUT");
      String text =
          type.equals("TEXTAREA") ? Rules.paragraph(v, 6000, false) : Rules.text(v, 1000, false);
      switch (type) {
        case "EMAIL" -> {
          if (!text.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new Problem(400, "EMAIL_INVALID");
        }
        case "NUMBER" -> {
          try {
            var n = new BigDecimal(text);
            if (n.precision() > 18 || n.scale() > 6 || n.scale() < -6)
              throw new IllegalArgumentException();
          } catch (Exception e) {
            throw new Problem(400, "NUMBER_INVALID");
          }
        }
        case "DATE" -> {
          try {
            LocalDate.parse(text);
          } catch (Exception e) {
            throw new Problem(400, "DATE_INVALID");
          }
        }
        case "SELECT" -> {
          if (!((List<?>) f.get("options")).contains(text))
            throw new Problem(400, "OPTION_INVALID");
        }
        default -> {}
      }
      result.put(key, text);
    }
    return result;
  }

  private FormRules() {}
}
