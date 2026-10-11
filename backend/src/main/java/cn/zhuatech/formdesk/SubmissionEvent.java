// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.persistence.*;

/** 状态、分派及处理备注；不保存完整填写内容。官网 https://www.zhuatech.cn/；商业咨询微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "submission_event")
public class SubmissionEvent {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "submission_id", nullable = false)
  public Long submissionId;

  @Column(name = "actor", nullable = false, length = 80)
  public String actor;

  @Column(name = "action", nullable = false, length = 60)
  public String action;

  @Column(name = "note", columnDefinition = "text", nullable = false)
  public String note;

  @Column(name = "created_at", nullable = false)
  public java.time.Instant createdAt;
}
