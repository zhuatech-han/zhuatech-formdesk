// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.persistence.*;

/** 表单草稿及当前发布状态。官网 https://www.zhuatech.cn/；商业咨询微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "form_definition")
public class FormDefinition {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "department_id", nullable = false)
  public Long departmentId;

  @Column(name = "creator_id", nullable = false)
  public Long creatorId;

  @Column(name = "title", nullable = false, length = 160)
  public String title;

  @Column(name = "description", columnDefinition = "text", nullable = false)
  public String description;

  @Column(name = "category", nullable = false, length = 60)
  public String category;

  @Column(name = "schema_json", columnDefinition = "longtext", nullable = false)
  public String schemaJson;

  @Column(name = "consent_text", columnDefinition = "text", nullable = false)
  public String consentText;

  @Column(name = "public_key", nullable = false, length = 80)
  public String publicKey;

  @Column(name = "status", nullable = false, length = 30)
  public String status;

  @Column(name = "published_id", nullable = true)
  public Long publishedId;

  @Column(name = "closes_at", nullable = true)
  public java.time.Instant closesAt;

  @Column(name = "version", nullable = false)
  public long version;

  @Column(name = "created_at", nullable = false)
  public java.time.Instant createdAt;
}
