package backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "tableauventes", schema = "public")
public class SnkVente {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "date_achat")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateAchat;

  @Column(name = "date_vente")
  @JsonFormat(pattern = "yyyy-MM-dd")
  private LocalDate dateVente;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false, nullable = false)
  private Instant createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private User user;

  @Column(name = "nom_item")
  private String nomItem;

  @Column(name = "prix_retail")
  private BigDecimal prixRetail;

  @Column(name = "prix_resell")
  private BigDecimal prixResell;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "categorie", length = 60)
  private String categorie;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", length = 30, nullable = false)
  @Builder.Default
  private ItemType type = ItemType.SNEAKER;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  @Builder.Default
  private Map<String, Object> metadata = new HashMap<>();

  @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Attachment> attachments = new ArrayList<>();
}
