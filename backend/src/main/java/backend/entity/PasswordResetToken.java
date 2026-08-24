package backend.entity;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "password_reset_tokens", schema = "public")
public class PasswordResetToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  // Stores the SHA-256 hash of the raw token sent by email.
  @Column(length = 200, nullable = false, unique = true)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "used_at")
  private Instant usedAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  public PasswordResetToken() {}

  public Long getId() { return id; }

  public String getToken() { return token; }
  public void setToken(String token) { this.token = token; }

  public User getUser() { return user; }
  public void setUser(User user) { this.user = user; }

  public Instant getExpiresAt() { return expiresAt; }
  public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

  public Instant getUsedAt() { return usedAt; }
  public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
