// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/** 管理端与业务端API；全部权限由事务服务实时校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@RestController
@RequestMapping("/api")
public class ApiController {
  final FormService forms;
  final AdminService admin;

  public ApiController(FormService forms, AdminService admin) {
    this.forms = forms;
    this.admin = admin;
  }

  /** 读取组织与选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/options")
  public Object options() {
    return forms.options();
  }

  /** 分页表单目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/forms")
  public Object list(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "newest") String sort) {
    return forms.forms(q, state, page, size, sort);
  }

  /** 发布与草稿详情。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/forms/{id}")
  public Object detail(@PathVariable long id) {
    return forms.detail(id);
  }

  /** 新建草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/forms")
  public Object create(@RequestBody Map<String, Object> b) {
    return forms.save(null, b);
  }

  /** 版本检查后保存草稿。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/forms/{id}")
  public Object save(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return forms.save(id, b);
  }

  /** 发布、关闭、归档与删除。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/forms/{id}/{action}")
  public Object action(
      @PathVariable long id, @PathVariable String action, @RequestBody Map<String, Object> b) {
    return forms.action(id, action, b);
  }

  /** 本人分派或组织范围的填写记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/submissions")
  public Object submissions(
      @RequestParam(required = false) Long formId,
      @RequestParam(defaultValue = "") String state,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "15") int size) {
    return forms.submissions(formId, state, q, page, size);
  }

  /** 收集内容及内部事件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/submissions/{id}")
  public Object submission(@PathVariable long id) {
    return forms.submission(id);
  }

  /** 处理或分派一条记录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/submissions/{id}/process")
  public Object process(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return forms.process(id, b);
  }

  /** 显式删除收集数据及附件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/submissions/{id}/purge")
  public Object purge(@PathVariable long id, @RequestBody Map<String, Object> b) {
    return forms.purge(id, b);
  }

  /** 汇总范围内真实状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/reports")
  public Object reports() {
    return forms.reports();
  }

  /** 记录导出事件，非幂等审计使用受CSRF保护POST。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/forms/{id}/export")
  public ResponseEntity<byte[]> export(@PathVariable long id) {
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=responses.csv")
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .body(forms.export(id).getBytes(StandardCharsets.UTF_8));
  }

  /** 附件下载仍执行范围校验。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/attachments/{id}")
  public ResponseEntity<byte[]> attachment(@PathVariable long id) {
    return PublicController.file(forms.attachment(id, null));
  }

  /** 不含填写内容的审计目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/audit")
  public Object audit() {
    return forms.audit();
  }

  /** 系统目录和账户选项。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/options")
  public Object adminOptions() {
    return admin.options();
  }

  /** 读取受权限控制的管理目录。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @GetMapping("/admin/{kind}")
  public Object adminRead(@PathVariable String kind) {
    return admin.read(kind);
  }

  /** 新建账号、角色、组织或分类。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PostMapping("/admin/{kind}")
  public Object adminCreate(@PathVariable String kind, @RequestBody Map<String, Object> b) {
    return admin.save(kind, null, b);
  }

  /** 更新管理目录且保留最后管理员。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @PutMapping("/admin/{kind}/{id}")
  public Object adminSave(
      @PathVariable String kind, @PathVariable Long id, @RequestBody Map<String, Object> b) {
    return admin.save(kind, id, b);
  }
}
