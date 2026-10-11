// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.persistence.*;

/** 填写记录和私有恢复凭据散列。官网 https://www.zhuatech.cn/；商业咨询微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "submission")
public class Submission {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "form_id", nullable = false)
  public Long formId;

  @Column(name = "revision_id", nullable = false)
  public Long revisionId;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(name = "token_hash", nullable = false, length = 64)
  public String tokenHash;

  @Column(name = "answers_json", columnDefinition = "longtext", nullable = false)
  public String answersJson;

  @Column(name = "status", nullable = false, length = 30)
  public String status;

  @Column(name = "assignee_id", nullable = true)
  public Long assigneeId;

  @Column(name = "request_note", columnDefinition = "text", nullable = false)
  public String requestNote;

  @Column(name = "consented", nullable = false)
  public boolean consented;

  @Column(name = "version", nullable = false)
  public long version;

  @Column(name = "created_at", nullable = false)
  public java.time.Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  public java.time.Instant updatedAt;
}
