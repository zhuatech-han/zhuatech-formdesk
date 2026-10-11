// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.persistence.*;

/** 不可变发布快照，不随草稿编辑改变。官网 https://www.zhuatech.cn/；商业咨询微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "form_revision")
public class FormRevision {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "form_id", nullable = false)
  public Long formId;

  @Column(name = "number", nullable = false)
  public int number;

  @Column(name = "title", nullable = false, length = 160)
  public String title;

  @Column(name = "description", columnDefinition = "text", nullable = false)
  public String description;

  @Column(name = "schema_json", columnDefinition = "longtext", nullable = false)
  public String schemaJson;

  @Column(name = "consent_text", columnDefinition = "text", nullable = false)
  public String consentText;

  @Column(name = "created_at", nullable = false)
  public java.time.Instant createdAt;
}
