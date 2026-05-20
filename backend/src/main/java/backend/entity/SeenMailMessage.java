package backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "seen_mail_messages", schema = "public")
@Getter
@Setter
public class SeenMailMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "mail_account_id", nullable = false)
  private MailAccount mailAccount;

  @Column(name = "provider_message_id", nullable = false, length = 255)
  private String providerMessageId;

  @Column(name = "received_at")
  private OffsetDateTime receivedAt;

  @Column(name = "parsed_at", nullable = false)
  private OffsetDateTime parsedAt;
}
