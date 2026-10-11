// Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ · 商业咨询微信 zhuatech / zhuatech2
package cn.zhuatech.formdesk;

import jakarta.persistence.*;

/** 受授权下载的附件，数据库持久化并随记录删除。官网 https://www.zhuatech.cn/；商业咨询微信 zhuatech / zhuatech2。 */
@Entity
@Table(name = "attachment")
public class Attachment {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  public Long id;

  @Column(name = "submission_id", nullable = false)
  public Long submissionId;

  @Column(name = "field_key", nullable = false, length = 40)
  public String fieldKey;

  @Column(name = "filename", nullable = false, length = 160)
  public String filename;

  @Column(name = "media_type", nullable = false, length = 80)
  public String mediaType;

  @Column(name = "size", nullable = false)
  public int size;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(name = "content", columnDefinition = "longblob", nullable = false)
  @Lob
  public byte[] content;

  @Column(name = "created_at", nullable = false)
  public java.time.Instant createdAt;
}
