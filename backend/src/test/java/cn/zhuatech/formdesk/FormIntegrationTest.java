// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.*;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 真实HTTP、Security、JPA与Flyway验证版本、恢复、处理、附件及权限。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。
 */
@SpringBootTest
@AutoConfigureMockMvc(print = MockMvcPrint.NONE)
@Import(FormIntegrationTest.TimeConfig.class)
class FormIntegrationTest {
  static final String PASSWORD = "Aa9" + UUID.randomUUID();

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add(
        "spring.datasource.url",
        () -> "jdbc:h2:mem:forms;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1");
    r.add("spring.datasource.username", () -> "sa");
    r.add("spring.datasource.password", () -> "");
    r.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.H2Dialect");
    r.add("formdesk.admin-password", () -> PASSWORD);
  }

  /** 可控UTC时钟用于截止边界。官网 https://www.zhuatech.cn/；微信 zhuatech / zhuatech2。 */
  static class TestClock extends Clock {
    volatile Instant now = Instant.parse("2026-10-11T08:00:00Z");

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return Clock.fixed(now, z);
    }

    public Instant instant() {
      return now;
    }
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    TestClock testClock() {
      return new TestClock();
    }
  }

  @Autowired MockMvc mvc;
  @Autowired TestClock clock;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
  final JsonMapper json = JsonMapper.builder().build();
  MockHttpSession admin;
  long formId;
  String publicKey;
  long version;
  String suffix;

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
            type.equals("SELECT") || type.equals("MULTI") ? List.of("A", "B") : List.of(),
            "whenKey",
            "",
            "whenValue",
            ""));
  }

  Map<String, Object> definition() {
    return new LinkedHashMap<>(
        Map.of(
            "title",
            "TEST form " + suffix,
            "departmentId",
            1,
            "category",
            "ENQUIRY",
            "description",
            "TEST collection",
            "consentText",
            "TEST consent",
            "fields",
            List.of(
                field("name", "TEXT", true),
                field("email", "EMAIL", false),
                field("file", "FILE", false))));
  }

  JsonNode read(MvcResult r) {
    return r.getResponse().getContentAsByteArray().length == 0
        ? json.createObjectNode()
        : json.readTree(r.getResponse().getContentAsByteArray());
  }

  MvcResult call(MockHttpSession session, String method, String path, Object body, int status)
      throws Exception {
    MockHttpServletRequestBuilder b =
        switch (method) {
          case "GET" -> get(path);
          case "POST" -> post(path);
          case "PUT" -> put(path);
          case "DELETE" -> delete(path);
          default -> throw new IllegalArgumentException();
        };
    if (session != null) b.session(session);
    if (!method.equals("GET")) b.with(csrf());
    if (body != null) b.contentType("application/json").content(json.writeValueAsString(body));
    var r = mvc.perform(b).andReturn();
    assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return r;
  }

  JsonNode request(MockHttpSession s, String m, String p, Object b, int status) throws Exception {
    return read(call(s, m, p, b, status));
  }

  MockHttpSession login(String user, String password) throws Exception {
    return (MockHttpSession)
        call(null, "POST", "/api/auth/login", Map.of("username", user, "password", password), 200)
            .getRequest()
            .getSession();
  }

  @BeforeEach
  void setup() throws Exception {
    suffix = UUID.randomUUID().toString().substring(0, 8);
    clock.now = Instant.parse("2026-10-11T08:00:00Z");
    admin = login("admin", PASSWORD);
    var f = request(admin, "POST", "/api/forms", definition(), 200);
    formId = f.get("id").asLong();
    version = f.get("version").asLong();
    publicKey = f.get("publicKey").asText();
  }

  JsonNode publish() throws Exception {
    return request(
        admin,
        "POST",
        "/api/forms/" + formId + "/publish",
        Map.of("version", version, "closesAt", ""),
        200);
  }

  JsonNode begin() throws Exception {
    return request(null, "POST", "/api/public/forms/" + publicKey + "/begin", Map.of(), 200);
  }

  JsonNode respond(String token, String method, Object body, int status) throws Exception {
    var b =
        method.equals("GET")
            ? get("/api/public/response")
            : method.equals("PUT")
                ? put("/api/public/response")
                : post("/api/public/response/submit");
    b.header("X-Receipt-Key", token);
    if (!method.equals("GET"))
      b.with(csrf()).contentType("application/json").content(json.writeValueAsString(body));
    var r = mvc.perform(b).andReturn();
    assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return read(r);
  }

  Map<String, Object> answer(long v, String name, boolean consent) {
    return Map.of("version", v, "answers", Map.of("name", name), "consented", consent);
  }

  JsonNode submitted() throws Exception {
    publish();
    var b = begin();
    return respond(b.get("token").asText(), "POST", answer(1, "TEST name", true), 200);
  }

  JsonNode process(long id, long v, String action, String note) throws Exception {
    return request(
        admin,
        "POST",
        "/api/submissions/" + id + "/process",
        Map.of("version", v, "action", action, "note", note),
        200);
  }

  MockHttpSession user(long role, long dept, String user) throws Exception {
    request(
        admin,
        "POST",
        "/api/admin/users",
        Map.of(
            "username",
            user,
            "displayName",
            "TEST " + user,
            "roleId",
            role,
            "departmentId",
            dept,
            "enabled",
            true,
            "password",
            PASSWORD),
        200);
    return login(user, PASSWORD);
  }

  JsonNode upload(String token, long v, String field, String name, byte[] data, int status)
      throws Exception {
    var b =
        multipart("/api/public/response/files")
            .file(new MockMultipartFile("file", name, "application/octet-stream", data))
            .param("field", field)
            .param("version", String.valueOf(v))
            .header("X-Receipt-Key", token)
            .with(csrf());
    var r = mvc.perform(b).andReturn();
    assertEquals(status, r.getResponse().getStatus(), r.getResponse().getContentAsString());
    return read(r);
  }

  @Test
  void anonymousAdminDenied() throws Exception {
    request(null, "GET", "/api/forms", null, 401);
    request(null, "GET", "/api/admin/users", null, 401);
  }

  @Test
  void csrfRequiredForAnonymousAndAdminWrites() throws Exception {
    assertEquals(
        403,
        mvc.perform(post("/api/public/forms/" + publicKey + "/begin"))
            .andReturn()
            .getResponse()
            .getStatus());
    assertEquals(
        403,
        mvc.perform(post("/api/forms").session(admin).contentType("application/json").content("{}"))
            .andReturn()
            .getResponse()
            .getStatus());
  }

  @Test
  void draftNotPublic() throws Exception {
    request(null, "GET", "/api/public/forms/" + publicKey, null, 410);
  }

  @Test
  void publicSnapshotHasNoPrivateDirectories() throws Exception {
    publish();
    var p = request(null, "GET", "/api/public/forms/" + publicKey, null, 200);
    assertEquals("TEST form " + suffix, p.get("title").asText());
    assertFalse(p.has("departmentId"));
    assertFalse(p.has("accounts"));
  }

  @Test
  void saveResumeAndSubmit() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    respond(key, "PUT", answer(1, "TEST name", false), 200);
    var r = respond(key, "GET", null, 200);
    assertEquals("TEST name", r.get("answers").get("name").asText());
    r = respond(key, "POST", answer(2, "TEST revised", true), 200);
    assertEquals("SUBMITTED", r.get("status").asText());
    assertFalse(r.has("tokenHash"));
    assertFalse(r.has("events"));
  }

  @Test
  void publishedVersionImmutable() throws Exception {
    var f = publish();
    var b = begin();
    var body = definition();
    body.put("title", "TEST new title");
    body.put("version", f.get("version").asLong());
    request(admin, "PUT", "/api/forms/" + formId, body, 200);
    assertEquals(
        "TEST form " + suffix,
        request(null, "GET", "/api/public/forms/" + publicKey, null, 200).get("title").asText());
    request(
        admin,
        "POST",
        "/api/forms/" + formId + "/publish",
        Map.of("version", 3, "closesAt", ""),
        200);
    assertEquals(
        "TEST new title",
        request(null, "GET", "/api/public/forms/" + publicKey, null, 200).get("title").asText());
    assertEquals(
        1,
        respond(b.get("token").asText(), "GET", null, 200).get("revision").get("number").asInt());
  }

  @Test
  void requiredRejectedAndDraftAllowsBlank() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    respond(key, "POST", answer(1, "", true), 400);
    respond(key, "PUT", answer(1, "", false), 200);
  }

  @Test
  void consentRequired() throws Exception {
    publish();
    var b = begin();
    respond(b.get("token").asText(), "POST", answer(1, "TEST", false), 400);
  }

  @Test
  void unknownAndInvalidFieldsRejected() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    respond(
        key, "PUT", Map.of("version", 1, "answers", Map.of("other", "x"), "consented", false), 400);
    respond(
        key,
        "PUT",
        Map.of("version", 1, "answers", Map.of("email", "bad"), "consented", false),
        400);
  }

  @Test
  void staleVersionAndDoubleSubmissionRejected() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    respond(key, "PUT", answer(1, "TEST", true), 200);
    respond(key, "POST", answer(1, "TEST", true), 409);
    respond(key, "POST", answer(2, "TEST", true), 200);
    respond(key, "POST", answer(3, "TEST", true), 409);
  }

  @Test
  void receiptCannotBeGuessedOrDiscoverOtherRecords() throws Exception {
    publish();
    var b = begin();
    respond("x".repeat(43), "GET", null, 404);
    assertEquals(
        64,
        jdbc.queryForObject(
            "select length(token_hash) from submission where id=?",
            Integer.class,
            b.get("response").get("id").asLong()));
  }

  @Test
  void fullProcessingAndSupplement() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    var s = respond(key, "POST", answer(1, "TEST", true), 200);
    long id = s.get("id").asLong();
    process(id, 2, "IN_PROGRESS", "");
    process(id, 3, "NEEDS_INFO", "Please add detail");
    var r = respond(key, "GET", null, 200);
    assertEquals("Please add detail", r.get("requestNote").asText());
    r = respond(key, "POST", answer(4, "TEST supplemented", true), 200);
    assertEquals("IN_PROGRESS", r.get("status").asText());
    process(id, 5, "COMPLETED", "Complete");
    process(id, 6, "reopen", "Check again");
    process(id, 7, "REJECTED", "Not accepted");
  }

  @Test
  void internalNotesNotInPublicReceipt() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    var s = respond(key, "POST", answer(1, "TEST", true), 200);
    process(s.get("id").asLong(), 2, "note", "TEST internal confidential");
    assertFalse(respond(key, "GET", null, 200).toString().contains("confidential"));
  }

  @Test
  void illegalStateTransitionRejected() throws Exception {
    var s = submitted();
    request(
        admin,
        "POST",
        "/api/submissions/" + s.get("id").asLong() + "/process",
        Map.of("version", 2, "action", "reopen", "note", "reason"),
        409);
    request(
        admin,
        "POST",
        "/api/submissions/" + s.get("id").asLong() + "/process",
        Map.of("version", 2, "action", "NEEDS_INFO", "note", ""),
        409);
  }

  @Test
  void closeBlocksSaveAndNewButReceiptReadable() throws Exception {
    var f = publish();
    var b = begin();
    request(
        admin,
        "POST",
        "/api/forms/" + formId + "/close",
        Map.of("version", f.get("version").asLong()),
        200);
    respond(b.get("token").asText(), "PUT", answer(1, "TEST", false), 410);
    request(null, "POST", "/api/public/forms/" + publicKey + "/begin", Map.of(), 410);
    assertFalse(respond(b.get("token").asText(), "GET", null, 200).get("accepting").asBoolean());
  }

  @Test
  void deadlineExactBoundary() throws Exception {
    request(
        admin,
        "POST",
        "/api/forms/" + formId + "/publish",
        Map.of("version", 1, "closesAt", "2026-10-11T08:01:00Z"),
        200);
    clock.now = Instant.parse("2026-10-11T08:00:59Z");
    request(null, "GET", "/api/public/forms/" + publicKey, null, 200);
    clock.now = Instant.parse("2026-10-11T08:01:00Z");
    request(null, "GET", "/api/public/forms/" + publicKey, null, 410);
  }

  @Test
  void departmentIsolation() throws Exception {
    var d =
        request(
            admin,
            "POST",
            "/api/admin/departments",
            Map.of("name", "TEST other " + suffix, "zone", "UTC", "enabled", true),
            200);
    var other = user(2, d.get("id").asLong(), "other-" + suffix);
    request(other, "GET", "/api/forms/" + formId, null, 403);
    assertEquals(0, request(other, "GET", "/api/forms", null, 200).get("total").asInt());
    request(other, "GET", "/api/admin/users", null, 403);
  }

  @Test
  void assignedProcessorCannotReadUnassignedOrReassign() throws Exception {
    var p = user(3, 1, "processor-" + suffix);
    var s = submitted();
    long id = s.get("id").asLong();
    request(p, "GET", "/api/submissions/" + id, null, 403);
    var accounts = request(admin, "GET", "/api/admin/users", null, 200);
    long pid = 0;
    for (var a : accounts)
      if (a.get("username").asText().equals("processor-" + suffix)) pid = a.get("id").asLong();
    request(
        admin,
        "POST",
        "/api/submissions/" + id + "/process",
        Map.of("version", 2, "action", "assign", "assigneeId", pid, "note", ""),
        200);
    request(p, "GET", "/api/submissions/" + id, null, 200);
    request(
        p,
        "POST",
        "/api/submissions/" + id + "/process",
        Map.of("version", 3, "action", "assign", "assigneeId", pid, "note", ""),
        409);
    request(p, "POST", "/api/forms/" + formId + "/export", Map.of(), 403);
  }

  @Test
  void viewRoleCannotReadAnswersOrModify() throws Exception {
    var viewer = user(4, 1, "viewer-" + suffix);
    var s = submitted();
    request(viewer, "GET", "/api/reports", null, 200);
    request(viewer, "GET", "/api/submissions/" + s.get("id").asLong(), null, 403);
    request(viewer, "POST", "/api/forms/" + formId + "/close", Map.of("version", 2), 403);
  }

  @Test
  void revokedAccountSessionDenied() throws Exception {
    String name = "disabled-" + suffix;
    var p = user(2, 1, name);
    JsonNode a = null;
    for (var x : request(admin, "GET", "/api/admin/users", null, 200))
      if (x.get("username").asText().equals(name)) a = x;
    var body = json.convertValue(a, Map.class);
    body.put("enabled", false);
    request(admin, "PUT", "/api/admin/users/" + a.get("id").asLong(), body, 200);
    request(p, "GET", "/api/forms", null, 401);
  }

  @Test
  void lastAdministratorProtected() throws Exception {
    JsonNode a = request(admin, "GET", "/api/admin/users", null, 200).get(0);
    var body = json.convertValue(a, Map.class);
    body.put("enabled", false);
    request(admin, "PUT", "/api/admin/users/" + a.get("id").asLong(), body, 409);
    request(admin, "GET", "/api/forms", null, 200);
  }

  @Test
  void validAttachmentPrivateAndDeletedWithRecord() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    var r = upload(key, 1, "file", "test.pdf", "%PDF-1.4 TEST".getBytes(), 200);
    long file = r.get("attachments").get(0).get("id").asLong();
    assertFalse(r.get("attachments").get(0).has("content"));
    var d =
        mvc.perform(get("/api/public/response/files/" + file).header("X-Receipt-Key", key))
            .andReturn();
    assertEquals(200, d.getResponse().getStatus());
    assertTrue(d.getResponse().getHeader("Content-Disposition").startsWith("attachment;"));
    respond(key, "POST", answer(2, "TEST", true), 200);
    request(
        admin,
        "POST",
        "/api/submissions/" + r.get("id").asLong() + "/purge",
        Map.of("version", 3),
        200);
    respond(key, "GET", null, 404);
    request(admin, "GET", "/api/attachments/" + file, null, 404);
  }

  @Test
  void attachmentOwnershipAndForgeryDenied() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    var r = upload(key, 1, "file", "test.pdf", "%PDF-1.4 TEST".getBytes(), 200);
    var other = begin();
    long id = r.get("attachments").get(0).get("id").asLong();
    var out =
        mvc.perform(
                get("/api/public/response/files/" + id)
                    .header("X-Receipt-Key", other.get("token").asText()))
            .andReturn();
    assertEquals(404, out.getResponse().getStatus());
    upload(key, 2, "file", "fake.pdf", "not pdf".getBytes(), 400);
    upload(key, 2, "name", "test.pdf", "%PDF-1.4".getBytes(), 409);
    upload(key, 2, "file", "../test.pdf", "%PDF-1.4".getBytes(), 400);
  }

  @Test
  void submittedAttachmentCannotBeChanged() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    respond(key, "POST", answer(1, "TEST", true), 200);
    upload(key, 2, "file", "test.pdf", "%PDF-1.4".getBytes(), 409);
  }

  @Test
  void csvIsScopedFormulaSafeAndAudited() throws Exception {
    publish();
    var b = begin();
    respond(b.get("token").asText(), "POST", answer(1, "=HYPERLINK(TEST)", true), 200);
    var r = call(admin, "POST", "/api/forms/" + formId + "/export", Map.of(), 200);
    assertTrue(r.getResponse().getContentAsString().contains("'=HYPERLINK"));
    assertTrue(
        request(admin, "GET", "/api/audit", null, 200).toString().contains("RESPONSE_EXPORT"));
  }

  @Test
  void draftDeleteAndHistoricalArchive() throws Exception {
    request(admin, "POST", "/api/forms/" + formId + "/delete", Map.of("version", 1), 200);
    request(admin, "GET", "/api/forms/" + formId, null, 404);
  }

  @Test
  void responseQuotaIncludesDrafts() throws Exception {
    JsonNode setting = null;
    for (var x : request(admin, "GET", "/api/admin/settings", null, 200))
      if (x.get("code").asText().equals("max_responses")) setting = x;
    request(
        admin,
        "PUT",
        "/api/admin/settings/" + setting.get("id").asLong(),
        Map.of("code", "max_responses", "value", "1"),
        200);
    try {
      publish();
      begin();
      request(null, "POST", "/api/public/forms/" + publicKey + "/begin", Map.of(), 409);
    } finally {
      request(
          admin,
          "PUT",
          "/api/admin/settings/" + setting.get("id").asLong(),
          Map.of("code", "max_responses", "value", "1000"),
          200);
    }
  }

  @Test
  void concurrentSubmitHasOneWinner() throws Exception {
    publish();
    var b = begin();
    String key = b.get("token").asText();
    try (var pool = Executors.newFixedThreadPool(2)) {
      var jobs =
          List.<Callable<Integer>>of(
              () ->
                  mvc.perform(
                          post("/api/public/response/submit")
                              .with(csrf())
                              .header("X-Receipt-Key", key)
                              .contentType("application/json")
                              .content(json.writeValueAsString(answer(1, "TEST", true))))
                      .andReturn()
                      .getResponse()
                      .getStatus(),
              () ->
                  mvc.perform(
                          post("/api/public/response/submit")
                              .with(csrf())
                              .header("X-Receipt-Key", key)
                              .contentType("application/json")
                              .content(json.writeValueAsString(answer(1, "TEST", true))))
                      .andReturn()
                      .getResponse()
                      .getStatus());
      var statuses = new ArrayList<Integer>();
      for (var f : pool.invokeAll(jobs)) statuses.add(f.get());
      Collections.sort(statuses);
      assertEquals(List.of(200, 409), statuses);
    }
  }

  @Test
  void initialMenuIsEnabledAndComplete() throws Exception {
    var p = request(admin, "GET", "/api/auth/me", null, 200);
    assertEquals(7, p.get("menus").size());
    for (var m : p.get("menus")) assertTrue(m.get("enabled").asBoolean());
  }

  @Test
  void passwordChangeRevokesSessionAndDetectsStaleAdminUpdate() throws Exception {
    String name = "password-" + suffix;
    var session = user(2, 1, name);
    JsonNode before = null;
    for (var a : request(admin, "GET", "/api/admin/users", null, 200))
      if (a.get("username").asText().equals(name)) before = a;
    String next = "Aa9" + UUID.randomUUID();
    request(
        session,
        "POST",
        "/api/auth/password",
        Map.of("oldPassword", PASSWORD, "newPassword", next),
        200);
    request(session, "GET", "/api/forms", null, 401);
    request(null, "POST", "/api/auth/login", Map.of("username", name, "password", PASSWORD), 401);
    login(name, next);
    request(
        admin,
        "PUT",
        "/api/admin/users/" + before.get("id").asLong(),
        json.convertValue(before, Map.class),
        409);
  }

  @Test
  void administratorResetRevokesExistingSession() throws Exception {
    String name = "reset-" + suffix;
    var session = user(2, 1, name);
    JsonNode before = null;
    for (var a : request(admin, "GET", "/api/admin/users", null, 200))
      if (a.get("username").asText().equals(name)) before = a;
    var body = json.convertValue(before, Map.class);
    body.put("password", "Aa9" + UUID.randomUUID());
    request(admin, "PUT", "/api/admin/users/" + before.get("id").asLong(), body, 200);
    request(session, "GET", "/api/forms", null, 401);
  }
}
