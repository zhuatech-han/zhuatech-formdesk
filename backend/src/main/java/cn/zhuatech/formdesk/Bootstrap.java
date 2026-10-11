// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 初始化组织、角色、菜单和参数；不注入业务数据。 官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Component
public class Bootstrap implements ApplicationRunner {
  static final List<String> CODES =
      List.of(
          "forms",
          "forms_manage",
          "submissions",
          "process",
          "export",
          "purge",
          "reports",
          "users",
          "roles",
          "settings",
          "audit");
  final Store db;
  final BCryptPasswordEncoder encoder;
  final String username, password;

  /** 空库管理员使用私有环境变量。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Bootstrap(
      Store db,
      BCryptPasswordEncoder encoder,
      @Value("${formdesk.admin-username}") String u,
      @Value("${formdesk.admin-password}") String p) {
    this.db = db;
    this.encoder = encoder;
    username = u;
    password = p;
  }

  /** 重启不重置管理员和既有数据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (!db.all(Account.class).isEmpty()) return;
    AdminService.validatePassword(password);
    if (!username.matches("[a-zA-Z0-9_.-]{3,60}"))
      throw new IllegalStateException("Invalid administrator name");
    for (var code : CODES) {
      var p = new Permission();
      p.code = code;
      p.name = code;
      db.save(p);
    }
    var d = new Department();
    d.name = "主组织 / Main organization";
    d.zone = "Asia/Shanghai";
    db.save(d);
    var admin = role("管理员 / Administrator", "ALL", CODES);
    role(
        "表单负责人 / Form manager",
        "DEPARTMENT",
        List.of("forms", "forms_manage", "submissions", "process", "export", "reports", "audit"));
    role("处理员 / Processor", "ASSIGNED", List.of("forms", "submissions", "process"));
    role("统计查阅 / Report viewer", "DEPARTMENT", List.of("forms", "reports"));
    var a = new Account();
    a.username = username.toLowerCase(Locale.ROOT);
    a.displayName = "管理员 / Administrator";
    a.passwordHash = encoder.encode(password);
    a.roleId = admin.id;
    a.departmentId = d.id;
    db.save(a);
    String[][] menus = {
      {"forms", "表单", "Forms", "forms"},
      {"submissions", "填写记录", "Submissions", "submissions"},
      {"reports", "统计", "Reports", "reports"},
      {"users", "登录账号", "Accounts", "users"},
      {"roles", "角色与权限", "Roles & permissions", "roles"},
      {"settings", "组织与设置", "Organizations & settings", "settings"},
      {"audit", "操作记录", "Audit trail", "audit"}
    };
    for (int i = 0; i < menus.length; i++) {
      var m = new NavMenu();
      m.code = menus[i][0];
      m.name = menus[i][1];
      m.nameEn = menus[i][2];
      m.permissionCode = menus[i][3];
      m.position = i;
      m.enabled = true;
      db.save(m);
    }
    String[][] categories = {
      {"ENQUIRY", "询盘", "Enquiries"},
      {"APPLICATION", "报名申请", "Applications"},
      {"INFORMATION", "资料收集", "Information"}
    };
    for (var v : categories) {
      var x = new DictionaryEntry();
      x.type = "CATEGORY";
      x.code = v[0];
      x.name = v[1];
      x.nameEn = v[2];
      db.save(x);
    }
    for (var v : new String[][] {{"max_responses", "1000"}, {"max_files", "10"}}) {
      var x = new SystemSetting();
      x.code = v[0];
      x.value = v[1];
      db.save(x);
    }
  }

  private AccessRole role(String n, String scope, List<String> codes) {
    var r = new AccessRole();
    r.name = n;
    r.scope = scope;
    r.permissions.addAll(codes);
    return db.save(r);
  }
}
