package backend.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
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

  @Column(name = "type", length = 80, nullable = false)
  @Builder.Default
  private String type = "SNEAKER";

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "metadata", columnDefinition = "jsonb")
  @Builder.Default
  private Map<String, Object> metadata = new HashMap<>();

  @Column(name = "is_group_parent", nullable = false)
  @Builder.Default
  private boolean groupParent = false;

  @Column(name = "unit_index")
  private Integer unitIndex;

  @Column(name = "parent_id", insertable = false, updatable = false)
  private Integer parentId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  @JsonIgnore
  private SnkVente parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @OrderBy("unitIndex ASC, id ASC")
  @JsonIgnore
  @Builder.Default
  private List<SnkVente> children = new ArrayList<>();

  @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
  @JsonIgnore
  @Builder.Default
  private List<Attachment> attachments = new ArrayList<>();
}
