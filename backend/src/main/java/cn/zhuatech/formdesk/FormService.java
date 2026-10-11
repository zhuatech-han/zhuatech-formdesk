// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.json.JsonMapper;

/** 表单版本、匿名恢复、合法处理流转及附件范围的事务边界。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
@Service
@Transactional
public class FormService {
  final Store db;
  final AccessService access;
  final AdminService admin;
  final Clock clock;
  final JsonMapper json = JsonMapper.builder().build();
  final SecureRandom random = new SecureRandom();

  /** 注入实时权限及数据库序列化写锁。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FormService(Store db, AccessService access, AdminService admin, Clock clock) {
    this.db = db;
    this.access = access;
    this.admin = admin;
    this.clock = clock;
  }

  String encode(Object x) {
    return json.writeValueAsString(x);
  }

  @SuppressWarnings("unchecked")
  List<Map<String, Object>> fields(FormRevision r) {
    return json.readValue(r.schemaJson, List.class);
  }

  @SuppressWarnings("unchecked")
  Map<String, Object> answers(Submission s) {
    return json.readValue(s.answersJson, Map.class);
  }

  String key() {
    byte[] b = new byte[32];
    random.nextBytes(b);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
  }

  String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  FormDefinition form(long id, String perm) {
    var f = db.get(FormDefinition.class, id);
    access.library(f.departmentId, perm);
    return f;
  }

  boolean canSee(Submission s) {
    var f = db.get(FormDefinition.class, s.formId);
    return access.department(f.departmentId)
        && (!access.role().scope.equals("ASSIGNED")
            || Objects.equals(s.assigneeId, access.current().id));
  }

  Submission record(long id, String perm) {
    access.require(perm);
    var s = db.get(Submission.class, id);
    if (!canSee(s)) throw new Problem(403, "OUT_OF_SCOPE");
    return s;
  }

  void version(long current, Object wanted) {
    Rules.check(current == Rules.id(wanted), "VERSION_CONFLICT");
  }

  void event(Submission s, String action, String note, String actor) {
    var e = new SubmissionEvent();
    e.submissionId = s.id;
    e.action = action;
    e.note = note;
    e.actor = actor;
    e.createdAt = clock.instant();
    db.save(e);
    s.updatedAt = e.createdAt;
  }

  void editable(Submission s) {
    Rules.check(Set.of("DRAFT", "NEEDS_INFO").contains(s.status), "SUBMISSION_LOCKED");
    open(db.get(FormDefinition.class, s.formId));
  }

  void open(FormDefinition f) {
    if (!f.status.equals("PUBLISHED")
        || !db.get(Department.class, f.departmentId).enabled
        || (f.closesAt != null && !clock.instant().isBefore(f.closesAt)))
      throw new Problem(410, "FORM_CLOSED");
  }

  List<Attachment> files(Submission s) {
    return db.query(Attachment.class, "from Attachment where submissionId=?1", s.id);
  }

  Map<String, Long> fileCounts(Submission s) {
    var counts = new HashMap<String, Long>();
    for (var a : files(s)) counts.merge(a.fieldKey, 1L, Long::sum);
    return counts;
  }

  Map<String, Object> revision(FormRevision r) {
    return Map.of(
        "id",
        r.id,
        "number",
        r.number,
        "title",
        r.title,
        "description",
        r.description,
        "consentText",
        r.consentText,
        "fields",
        fields(r));
  }

  Map<String, Object> response(Submission s, boolean internal) {
    var m = new LinkedHashMap<String, Object>();
    m.put("id", s.id);
    m.put("formId", s.formId);
    m.put("status", s.status);
    var definition = db.get(FormDefinition.class, s.formId);
    m.put(
        "accepting",
        definition.status.equals("PUBLISHED")
            && db.get(Department.class, definition.departmentId).enabled
            && (definition.closesAt == null || clock.instant().isBefore(definition.closesAt)));
    m.put("version", s.version);
    m.put("answers", answers(s));
    m.put("revision", revision(db.get(FormRevision.class, s.revisionId)));
    m.put("requestNote", s.requestNote);
    m.put("consented", s.consented);
    m.put("createdAt", s.createdAt);
    m.put("updatedAt", s.updatedAt);
    m.put("attachments", files(s));
    if (internal) {
      m.put("departmentId", db.get(FormDefinition.class, s.formId).departmentId);
      m.put("assigneeId", s.assigneeId);
      m.put(
          "events",
          db.query(SubmissionEvent.class, "from SubmissionEvent where submissionId=?1", s.id));
    }
    return m;
  }

  Submission token(String token) {
    if (token == null || !token.matches("[A-Za-z0-9_-]{43}"))
      throw new Problem(404, "RECEIPT_INVALID");
    var list = db.query(Submission.class, "from Submission where tokenHash=?1", hash(token));
    if (list.isEmpty()) throw new Problem(404, "RECEIPT_INVALID");
    var s = list.getFirst();
    if (!db.get(Department.class, db.get(FormDefinition.class, s.formId).departmentId).enabled)
      throw new Problem(404, "RECEIPT_INVALID");
    return s;
  }

  /** 可见表单目录及组织内负责人选项；不返回填写答案。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> options() {
    access.require("forms");
    return Map.of(
        "departments",
        db.all(Department.class).stream().filter(d -> access.department(d.id)).toList(),
        "categories",
        db.all(DictionaryEntry.class),
        "accounts",
        db.all(Account.class).stream()
            .filter(
                a ->
                    a.enabled
                        && access.department(a.departmentId)
                        && db.get(AccessRole.class, a.roleId).permissions.contains("process"))
            .map(
                a ->
                    Map.of(
                        "id", a.id, "displayName", a.displayName, "departmentId", a.departmentId))
            .toList());
  }

  /** 搜索、状态、分页、排序，先执行组织范围过滤。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> forms(String q, String state, int page, int size, String sort) {
    access.require("forms");
    Rules.integer(page, 1, 10000);
    Rules.integer(size, 1, 100);
    var rows =
        db.all(FormDefinition.class).stream()
            .filter(
                f ->
                    access.department(f.departmentId)
                        && f.title.toLowerCase(Locale.ROOT).contains(q.toLowerCase(Locale.ROOT))
                        && (state.isEmpty() || f.status.equals(state)))
            .sorted(
                sort.equals("title")
                    ? Comparator.comparing(f -> f.title)
                    : Comparator.comparing((FormDefinition f) -> f.id).reversed())
            .toList();
    return page(rows, page, size);
  }

  <T> Map<String, Object> page(List<T> rows, int page, int size) {
    int a = Math.min(rows.size(), (page - 1) * size);
    return Map.of(
        "items",
        rows.subList(a, Math.min(rows.size(), a + size)),
        "total",
        rows.size(),
        "page",
        page);
  }

  /** 草稿与发布版本分开返回，编辑不会改变已发布内容。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> detail(long id) {
    var f = form(id, "forms");
    var m = new LinkedHashMap<String, Object>();
    m.put("form", f);
    m.put("fields", json.readValue(f.schemaJson, List.class));
    m.put(
        "revisions",
        db.query(FormRevision.class, "from FormRevision where formId=?1", id).stream()
            .map(this::revision)
            .toList());
    return m;
  }

  /** 新建或更新草稿，防止并发覆盖；组织身份不可搬迁。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FormDefinition save(Long id, Map<String, Object> b) {
    admin.lock();
    access.require("forms_manage");
    var f = id == null ? new FormDefinition() : form(id, "forms_manage");
    if (id != null) {
      version(f.version, b.get("version"));
      Rules.check(!f.status.equals("ARCHIVED"), "FORM_ARCHIVED");
    }
    Long dept = Rules.id(b.get("departmentId"));
    access.library(dept, "forms_manage");
    Rules.check(db.get(Department.class, dept).enabled, "DEPARTMENT_DISABLED");
    if (id != null) Rules.check(f.departmentId.equals(dept), "IDENTITY_LOCKED");
    String category = Rules.text(b.get("category"), 60, true);
    Rules.check(
        db
            .query(
                DictionaryEntry.class,
                "from DictionaryEntry where type='CATEGORY' and code=?1",
                category)
            .stream()
            .anyMatch(x -> x.enabled),
        "CATEGORY_DISABLED");
    f.departmentId = dept;
    f.title = Rules.text(b.get("title"), 160, true);
    f.description = Rules.paragraph(b.get("description"), 6000, false);
    f.category = category;
    f.schemaJson = encode(FormRules.schema(b.get("fields")));
    f.consentText = Rules.paragraph(b.get("consentText"), 3000, true);
    if (id == null) {
      f.creatorId = access.current().id;
      f.status = "DRAFT";
      f.version = 1;
      f.publicKey = key();
      f.createdAt = clock.instant();
      db.save(f);
    } else f.version++;
    access.audit("FORM_SAVE", f.id, dept);
    return f;
  }

  /** 创建不可变发布版本；截止时间只由明确发布变更，不随草稿保存改变。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public FormDefinition action(long id, String action, Map<String, Object> b) {
    admin.lock();
    var f = form(id, "forms_manage");
    version(f.version, b.get("version"));
    switch (action) {
      case "publish" -> {
        Rules.check(!f.status.equals("ARCHIVED"), "FORM_ARCHIVED");
        Rules.check(db.get(Department.class, f.departmentId).enabled, "DEPARTMENT_DISABLED");
        String end = Rules.text(b.get("closesAt"), 40, false);
        Instant closes = end.isEmpty() ? null : Instant.parse(end);
        Rules.check(closes == null || closes.isAfter(clock.instant()), "DEADLINE_INVALID");
        var r = new FormRevision();
        r.formId = f.id;
        r.number = db.query(FormRevision.class, "from FormRevision where formId=?1", id).size() + 1;
        r.title = f.title;
        r.description = f.description;
        r.schemaJson = f.schemaJson;
        r.consentText = f.consentText;
        r.createdAt = clock.instant();
        db.save(r);
        f.publishedId = r.id;
        f.closesAt = closes;
        f.status = "PUBLISHED";
      }
      case "close" -> {
        Rules.check(f.status.equals("PUBLISHED"), "STATE_INVALID");
        f.status = "CLOSED";
      }
      case "archive" -> {
        Rules.check(!f.status.equals("PUBLISHED") && !f.status.equals("ARCHIVED"), "STATE_INVALID");
        f.status = "ARCHIVED";
      }
      case "delete" -> {
        Rules.check(
            f.status.equals("DRAFT")
                && db.query(Submission.class, "from Submission where formId=?1", id).isEmpty(),
            "RECORD_REFERENCED");
        db.delete(f);
        access.audit("FORM_DELETE", id, f.departmentId);
        return f;
      }
      default -> throw new Problem(404, "NOT_FOUND");
    }
    f.version++;
    access.audit("FORM_" + action.toUpperCase(Locale.ROOT), id, f.departmentId);
    return f;
  }

  /** 匿名页面只读当前发布快照，不包含账号、组织目录或提交结果。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> publicForm(String k) {
    var rows = db.query(FormDefinition.class, "from FormDefinition where publicKey=?1", k);
    if (rows.isEmpty()) throw new Problem(404, "NOT_FOUND");
    var f = rows.getFirst();
    open(f);
    return revision(db.get(FormRevision.class, f.publishedId));
  }

  /** 草稿建立时发放256位一次性恢复密钥，数据库仅保留散列。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> begin(String k) {
    admin.lock();
    var rows = db.query(FormDefinition.class, "from FormDefinition where publicKey=?1", k);
    if (rows.isEmpty()) throw new Problem(404, "NOT_FOUND");
    var f = rows.getFirst();
    open(f);
    Rules.check(
        db.query(Submission.class, "from Submission where formId=?1", f.id).size()
            < admin.setting("max_responses"),
        "RESPONSE_LIMIT");
    var s = new Submission();
    s.formId = f.id;
    s.revisionId = f.publishedId;
    String secret = key();
    s.tokenHash = hash(secret);
    s.answersJson = "{}";
    s.status = "DRAFT";
    s.version = 1;
    s.requestNote = "";
    s.createdAt = clock.instant();
    s.updatedAt = s.createdAt;
    db.save(s);
    event(s, "DRAFT_CREATED", "", "RESPONDENT");
    return Map.of("token", secret, "response", response(s, false));
  }

  /** 恢复不要求账号；密钥只能通过专用头传递，不写URL查询或日志。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> resume(String k) {
    return response(token(k), false);
  }

  /** 保存进度或提交；提交验证条件、必填、附件、同意及合法状态。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> respond(String k, Map<String, Object> b, boolean submit) {
    admin.lock();
    var s = token(k);
    editable(s);
    version(s.version, b.get("version"));
    var r = db.get(FormRevision.class, s.revisionId);
    var values = FormRules.answers(fields(r), b.get("answers"), submit, fileCounts(s));
    boolean consent = Rules.flag(b.get("consented"));
    if (submit && !consent) throw new Problem(400, "CONSENT_REQUIRED");
    s.answersJson = encode(values);
    s.consented = consent;
    if (submit) {
      s.status = s.status.equals("NEEDS_INFO") ? "IN_PROGRESS" : "SUBMITTED";
      s.requestNote = "";
    }
    s.version++;
    event(s, submit ? "SUBMITTED" : "DRAFT_SAVED", "", "RESPONDENT");
    return response(s, false);
  }

  /** 读取本人分派/组织内记录，草稿由筛选明确展示。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> submissions(Long formId, String state, String q, int page, int size) {
    access.require("submissions");
    Rules.integer(page, 1, 10000);
    Rules.integer(size, 1, 100);
    var rows =
        db.all(Submission.class).stream()
            .filter(this::canSee)
            .filter(
                s ->
                    (formId == null || s.formId.equals(formId))
                        && (state.isEmpty() || s.status.equals(state))
                        && (q.isBlank()
                            || s.id.toString().contains(q)
                            || answers(s)
                                .toString()
                                .toLowerCase(Locale.ROOT)
                                .contains(q.toLowerCase(Locale.ROOT))))
            .sorted(Comparator.comparing((Submission s) -> s.id).reversed())
            .map(
                s -> {
                  var m = new LinkedHashMap<String, Object>();
                  m.put("id", s.id);
                  m.put("formId", s.formId);
                  m.put("title", db.get(FormRevision.class, s.revisionId).title);
                  m.put("revision", db.get(FormRevision.class, s.revisionId).number);
                  m.put("status", s.status);
                  m.put("assigneeId", s.assigneeId);
                  m.put("updatedAt", s.updatedAt);
                  return m;
                })
            .toList();
    return page(rows, page, size);
  }

  /** 完整记录详情只给有权限处理者，公开回执不返回内部备注。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> submission(long id) {
    return response(record(id, "submissions"), true);
  }

  /** 分派与状态流转使用版本锁；请求补充备注公开，其余备注内部可见。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> process(long id, Map<String, Object> b) {
    admin.lock();
    var s = record(id, "process");
    version(s.version, b.get("version"));
    String action = Rules.text(b.get("action"), 30, true),
        note = Rules.paragraph(b.get("note"), 3000, false);
    var f = db.get(FormDefinition.class, s.formId);
    if (action.equals("assign")) {
      Rules.check(
          !access.role().scope.equals("ASSIGNED")
              && !Set.of("DRAFT", "COMPLETED", "REJECTED").contains(s.status),
          "STATE_INVALID");
      Long assignee = Rules.id(b.get("assigneeId"));
      var a = db.get(Account.class, assignee);
      Rules.check(
          a.enabled
              && a.departmentId.equals(f.departmentId)
              && db.get(AccessRole.class, a.roleId).permissions.contains("process"),
          "ASSIGNEE_INVALID");
      s.assigneeId = a.id;
    } else if (action.equals("note")) {
      Rules.check(!s.status.equals("DRAFT") && !note.isEmpty(), "STATE_INVALID");
    } else {
      boolean valid =
          switch (action) {
            case "IN_PROGRESS" -> s.status.equals("SUBMITTED");
            case "NEEDS_INFO" ->
                Set.of("SUBMITTED", "IN_PROGRESS").contains(s.status) && !note.isEmpty();
            case "COMPLETED", "REJECTED" -> Set.of("SUBMITTED", "IN_PROGRESS").contains(s.status);
            case "reopen" -> Set.of("COMPLETED", "REJECTED").contains(s.status) && !note.isEmpty();
            default -> false;
          };
      Rules.check(valid, "STATE_INVALID");
      s.status = action.equals("reopen") ? "IN_PROGRESS" : action;
      s.requestNote = action.equals("NEEDS_INFO") ? note : "";
    }
    s.version++;
    event(s, action, note, access.current().username);
    access.audit("RESPONSE_" + action, id, f.departmentId);
    return response(s, true);
  }

  /** 明确删除记录及附件，保留不含答案的操作审计。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Boolean> purge(long id, Map<String, Object> b) {
    admin.lock();
    var s = record(id, "purge");
    version(s.version, b.get("version"));
    for (var a : files(s)) db.delete(a);
    for (var e : db.query(SubmissionEvent.class, "from SubmissionEvent where submissionId=?1", id))
      db.delete(e);
    Long dept = db.get(FormDefinition.class, s.formId).departmentId;
    db.delete(s);
    access.audit("RESPONSE_PURGE", id, dept);
    return Map.of("ok", true);
  }

  /** 附件必须归属当前私有回执；只接受PDF/JPEG/PNG，限制大小与数量。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> upload(String k, String field, MultipartFile file, Object wanted) {
    admin.lock();
    var s = token(k);
    editable(s);
    version(s.version, wanted);
    var fields = fields(db.get(FormRevision.class, s.revisionId));
    Rules.check(
        fields.stream().anyMatch(f -> f.get("key").equals(field) && f.get("type").equals("FILE")),
        "UNKNOWN_FIELD");
    Rules.check(files(s).size() < admin.setting("max_files"), "FILE_LIMIT");
    if (file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new Problem(413, "FILE_TOO_LARGE");
    byte[] content;
    try {
      content = file.getBytes();
    } catch (java.io.IOException e) {
      throw new Problem(400, "FILE_INVALID");
    }
    String name = Rules.text(file.getOriginalFilename(), 160, true);
    if (name.contains("/") || name.contains("\\") || name.contains(".."))
      throw new Problem(400, "FILE_INVALID");
    String lower = name.toLowerCase(Locale.ROOT), mime;
    if (lower.endsWith(".pdf")
        && content.length >= 5
        && new String(content, 0, 5, StandardCharsets.US_ASCII).equals("%PDF-"))
      mime = "application/pdf";
    else if (lower.endsWith(".png")
        && content.length >= 8
        && Arrays.equals(
            Arrays.copyOf(content, 8), new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10}))
      mime = "image/png";
    else if ((lower.endsWith(".jpg") || lower.endsWith(".jpeg"))
        && content.length >= 3
        && content[0] == (byte) 255
        && content[1] == (byte) 216
        && content[2] == (byte) 255) mime = "image/jpeg";
    else throw new Problem(400, "FILE_INVALID");
    var a = new Attachment();
    a.submissionId = s.id;
    a.fieldKey = field;
    a.filename = name;
    a.mediaType = mime;
    a.size = content.length;
    a.content = content;
    a.createdAt = clock.instant();
    db.save(a);
    s.version++;
    event(s, "FILE_ADDED", "", "RESPONDENT");
    return response(s, false);
  }

  /** 草稿/补充阶段删除自己附件，其他记录或锁定状态拒绝。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public Map<String, Object> removeFile(String k, long id, Object wanted) {
    admin.lock();
    var s = token(k);
    editable(s);
    version(s.version, wanted);
    var a = db.get(Attachment.class, id);
    if (!a.submissionId.equals(s.id)) throw new Problem(404, "NOT_FOUND");
    db.delete(a);
    s.version++;
    event(s, "FILE_REMOVED", "", "RESPONDENT");
    return response(s, false);
  }

  /** 下载按内部范围或回执归属检查，强制附件保存不执行文件。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Attachment attachment(long id, String token) {
    var a = db.get(Attachment.class, id);
    if (token != null) {
      if (!a.submissionId.equals(token(token).id)) throw new Problem(404, "NOT_FOUND");
    } else record(a.submissionId, "submissions");
    return a;
  }

  /** 汇总真实提交状态，不把草稿计为正式提交。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Map<String, Object> reports() {
    access.require("reports");
    var fs =
        db.all(FormDefinition.class).stream()
            .filter(f -> access.department(f.departmentId))
            .toList();
    var ss = db.all(Submission.class).stream().filter(this::canSee).toList();
    var counts = new LinkedHashMap<String, Long>();
    for (var status :
        List.of("DRAFT", "SUBMITTED", "IN_PROGRESS", "NEEDS_INFO", "COMPLETED", "REJECTED"))
      counts.put(status, ss.stream().filter(s -> s.status.equals(status)).count());
    return Map.of(
        "forms",
        fs.size(),
        "published",
        fs.stream().filter(f -> f.status.equals("PUBLISHED")).count(),
        "states",
        counts,
        "byForm",
        fs.stream()
            .map(
                f ->
                    Map.of(
                        "id",
                        f.id,
                        "title",
                        f.title,
                        "responses",
                        ss.stream()
                            .filter(s -> s.formId.equals(f.id) && !s.status.equals("DRAFT"))
                            .count()))
            .toList());
  }

  /** CSV使用固定版本的字段标签，按授权范围导出，公式前缀安全转义。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  public String export(long id) {
    var f = form(id, "export");
    var lines = new StringBuilder("\uFEFFid,revision,status,assignee,updated_at,field,value\r\n");
    for (var s : db.query(Submission.class, "from Submission where formId=?1", id)) {
      if (!canSee(s) || s.status.equals("DRAFT")) continue;
      var r = db.get(FormRevision.class, s.revisionId);
      var values = answers(s);
      for (var field : fields(r)) {
        String key = (String) field.get("key");
        Object value =
            field.get("type").equals("FILE")
                ? files(s).stream()
                    .filter(a -> a.fieldKey.equals(key))
                    .map(a -> a.filename)
                    .toList()
                : values.getOrDefault(key, "");
        lines
            .append(
                String.join(
                    ",",
                    List.of(
                        Rules.csv(s.id),
                        Rules.csv(r.number),
                        Rules.csv(s.status),
                        Rules.csv(s.assigneeId),
                        Rules.csv(s.updatedAt),
                        Rules.csv(field.get("label")),
                        Rules.csv(value))))
            .append("\r\n");
      }
    }
    access.audit("RESPONSE_EXPORT", id, f.departmentId);
    return lines.toString();
  }

  /** 范围内审计，不带答案、附件内容或恢复凭据。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  @Transactional(readOnly = true)
  public Object audit() {
    access.require("audit");
    return db.all(AuditEvent.class).stream()
        .filter(e -> access.department(e.departmentId))
        .sorted(Comparator.comparing((AuditEvent e) -> e.id).reversed())
        .toList();
  }
}
