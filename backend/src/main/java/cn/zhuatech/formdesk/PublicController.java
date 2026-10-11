// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** 匿名填写端；公开表单和私有回执分开授权，CSRF仍保护每个写操作。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api/public")
public class PublicController {
  final FormService forms;
  final PublicLimiter limiter;

  public PublicController(FormService forms, PublicLimiter limiter) {
    this.forms = forms;
    this.limiter = limiter;
  }

  void limit(HttpServletRequest r) {
    limiter.check(r.getRemoteAddr());
  }

  /** 当前发布页面。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/forms/{key}")
  public Object form(@PathVariable String key) {
    return forms.publicForm(key);
  }

  /** 创建草稿并返回私有恢复凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/forms/{key}/begin")
  public Object begin(@PathVariable String key, HttpServletRequest r) {
    limit(r);
    return forms.begin(key);
  }

  /** 使用专用请求头恢复记录，不在路径保存密钥。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/response")
  public Object resume(@RequestHeader("X-Receipt-Key") String key) {
    return forms.resume(key);
  }

  /** 保存草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/response")
  public Object save(
      @RequestHeader("X-Receipt-Key") String key,
      @RequestBody Map<String, Object> b,
      HttpServletRequest r) {
    limit(r);
    return forms.respond(key, b, false);
  }

  /** 正式提交或补充提交。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/response/submit")
  public Object submit(
      @RequestHeader("X-Receipt-Key") String key,
      @RequestBody Map<String, Object> b,
      HttpServletRequest r) {
    limit(r);
    return forms.respond(key, b, true);
  }

  /** 上传PDF/PNG/JPEG并返回新版本回执。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/response/files")
  public Object upload(
      @RequestHeader("X-Receipt-Key") String key,
      @RequestParam String field,
      @RequestParam long version,
      @RequestPart MultipartFile file,
      HttpServletRequest r) {
    limit(r);
    return forms.upload(key, field, file, version);
  }

  /** 私有附件删除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @DeleteMapping("/response/files/{id}")
  public Object remove(
      @RequestHeader("X-Receipt-Key") String key,
      @PathVariable long id,
      @RequestParam long version,
      HttpServletRequest r) {
    limit(r);
    return forms.removeFile(key, id, version);
  }

  /** 私有回执附件下载。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/response/files/{id}")
  public ResponseEntity<byte[]> download(
      @RequestHeader("X-Receipt-Key") String key, @PathVariable long id) {
    return file(forms.attachment(id, key));
  }

  /** 强制下载、禁缓存，浏览器不执行上传内容。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  static ResponseEntity<byte[]> file(Attachment a) {
    return ResponseEntity.ok()
        .header(
            "Content-Disposition",
            ContentDisposition.attachment()
                .filename(a.filename, StandardCharsets.UTF_8)
                .build()
                .toString())
        .header("Cache-Control", "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(a.content);
  }
}
