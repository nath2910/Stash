package backend.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "discord_allowed_guild")
public class DiscordAllowedGuild {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "guild_id", length = 32, nullable = false, unique = true)
  private String guildId;

  @Column(name = "premium_role_id", length = 32)
  private String premiumRoleId;

  @Column(name = "label", length = 128)
  private String label;

  public DiscordAllowedGuild() {}

  public DiscordAllowedGuild(String guildId, String premiumRoleId, String label) {
    this.guildId = guildId;
    this.premiumRoleId = premiumRoleId;
    this.label = label;
  }

  public Long getId() { return id; }
  public String getGuildId() { return guildId; }
  public void setGuildId(String guildId) { this.guildId = guildId; }
  public String getPremiumRoleId() { return premiumRoleId; }
  public void setPremiumRoleId(String premiumRoleId) { this.premiumRoleId = premiumRoleId; }
  public String getLabel() { return label; }
  public void setLabel(String label) { this.label = label; }
}
