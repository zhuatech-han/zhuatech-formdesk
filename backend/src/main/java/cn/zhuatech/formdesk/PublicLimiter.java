// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

/** 单实例公开写入速率窗口；反向代理另需容量和反滥用限制。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class PublicLimiter {
  final Map<String, Window> windows = new HashMap<>();

  record Window(int count, Instant until) {}

  /** 按真实请求来源限流，不相信客户端提交的转发地址。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public synchronized void check(String key) {
    var now = Instant.now();
    windows.entrySet().removeIf(e -> !e.getValue().until().isAfter(now));
    var w = windows.get(key);
    if (w != null && w.count() >= 60) throw new Problem(429, "PUBLIC_THROTTLED");
    if (w == null && windows.size() >= 10000) throw new Problem(429, "PUBLIC_THROTTLED");
    windows.put(
        key,
        new Window(w == null ? 1 : w.count() + 1, w == null ? now.plusSeconds(60) : w.until()));
  }
}
