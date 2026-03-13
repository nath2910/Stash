package backend.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;


import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;




import backend.dto.TopVenteProjection;
import backend.entity.SnkVente;


@Repository
public interface SnkVenteRepository extends JpaRepository<SnkVente, Integer> {


  public interface BrandCount {
        String getMarque();
        long getNb();
    }

    public interface TimePointRow {
  java.time.LocalDate getBucket();
  java.math.BigDecimal getCa();
  java.math.BigDecimal getProfit();
}

  public interface TimePointFullRow {
    java.time.LocalDate getBucket();
    java.math.BigDecimal getCa();
    java.math.BigDecimal getProfit();
    long getNb();
  }

  public interface AvgDaysRow {
    java.time.LocalDate getBucket();
    Double getAvgDays();
  }

  public interface LabelValueRow {
    String getLabel();
    java.math.BigDecimal getValue();
  }



  // Liste de tout les items dans la liste
  List<SnkVente> findByUser_IdOrderByDateAchatDesc(Long userId);
  List<SnkVente> findByUser_IdOrderByDateAchatDesc(Long userId, Pageable pageable);

  // Trouver des dernier ajout 
List<SnkVente> findByUser_IdOrderByCreatedAtDesc(Long userId, Pageable pageable);




  // Delete sÃ©curisÃ© : un user ne peut supprimer que ses ventes
    void deleteByIdAndUser_Id(Integer id, Long userId);

  @Modifying
  @Query("delete from SnkVente v where v.user.id = :userId and v.id in :ids")
  int deleteByUserAndIds(@Param("userId") Long userId, @Param("ids") List<Integer> ids);

  // Alias pour compatibilité (anciennes classes déjà chargées par devtools)
  @Modifying
  @Query("delete from SnkVente v where v.user.id = :userId and v.id in :ids")
  int deleteByUser_IdAndIdIn(@Param("userId") Long userId, @Param("ids") List<Integer> ids);

  // Delete toutes les ventes d'un user (suppression compte)
    void deleteByUser_Id(Long userId);

    
    // Total global par user
      @Query("""
    select coalesce(
      sum(
        case when v.prixResell is not null
          then coalesce(v.prixResell,0) - coalesce(v.prixRetail,0)
          else 0
        end
      ), 0)
    from SnkVente v
    where v.user.id = :userId
  """)
  BigDecimal totalBenef(@Param("userId") Long userId);


    // Total entre 2 dates par user
    @Query("""
      select coalesce( sum(v.prixResell - v.prixRetail), 0 )
      from SnkVente v
      where v.user.id = :userId
        and v.dateAchat between :start and :end
        and v.prixResell is not null
        and v.prixRetail is not null
    """)
    BigDecimal totalBenefBetween(
            @Param("userId") Long userId,
            @Param("start") java.time.LocalDate start,
            @Param("end")   java.time.LocalDate end
    );

    default BigDecimal totalBenefYear(Long userId, int year) {
        return totalBenefBetween(
                userId,
                java.time.LocalDate.of(year, 1, 1),
                java.time.LocalDate.of(year, 12, 31)
        );
    }

    // Somme des prixResell par user
   @Query("""
    select coalesce(sum(coalesce(v.prixResell, 0)), 0)
    from SnkVente v
    where v.user.id = :userId
  """)
  BigDecimal sumPrixResell(@Param("userId") Long userId);

    // Graph par marque par user
    @Query("""
      SELECT
        CASE
          WHEN lower(v.nomItem) LIKE '%nike%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%adidas%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%puma%' THEN 'Puma'
          WHEN lower(v.nomItem) LIKE '%new balance%' THEN 'New Balance'
          WHEN lower(v.nomItem) LIKE '%asics%' THEN 'ASICS'
          WHEN lower(v.nomItem) LIKE '%pokemon%' THEN 'Pokemon'
          WHEN lower(v.nomItem) LIKE '%air%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%jordan%' THEN 'Jordan'
          WHEN lower(v.nomItem) LIKE '%dunk%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%yeezy%' THEN 'Yeezy'
          WHEN lower(v.nomItem) LIKE '%samba%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%spezial%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%gazelle%' THEN 'Adidas'
          ELSE 'Autre'
        END AS marque,
        COUNT(v) AS nb
      FROM SnkVente v
      WHERE v.user.id = :userId
      GROUP BY
        CASE
          WHEN lower(v.nomItem) LIKE '%nike%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%adidas%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%puma%' THEN 'Puma'
          WHEN lower(v.nomItem) LIKE '%new balance%' THEN 'New Balance'
          WHEN lower(v.nomItem) LIKE '%asics%' THEN 'ASICS'
          WHEN lower(v.nomItem) LIKE '%pokemon%' THEN 'Pokemon'
          WHEN lower(v.nomItem) LIKE '%air%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%jordan%' THEN 'Jordan'
          WHEN lower(v.nomItem) LIKE '%dunk%' THEN 'Nike'
          WHEN lower(v.nomItem) LIKE '%yeezy%' THEN 'Yeezy'
          WHEN lower(v.nomItem) LIKE '%samba%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%spezial%' THEN 'Adidas'
          WHEN lower(v.nomItem) LIKE '%gazelle%' THEN 'Adidas'
          ELSE 'Autre'
        END
      ORDER BY nb DESC
    """)
    List<BrandCount> graphMarque(@Param("userId") Long userId);

    

    // Top ventes par nom dâ€™item sur une pÃ©riode + user
@Query("""
    SELECT 
      v.nomItem AS nomItem,
      SUM( COALESCE(v.prixResell, 0) - COALESCE(v.prixRetail, 0) ) AS benefice
    FROM SnkVente v
    WHERE v.user.id = :userId
      AND v.dateVente IS NOT NULL
      AND v.dateVente BETWEEN :start AND :end
      AND (:categoriesAll = true OR COALESCE(NULLIF(trim(v.categorie), ''), 'Autre') IN (:categories))
      AND v.prixResell IS NOT NULL
      AND v.prixRetail IS NOT NULL
    GROUP BY v.nomItem
    ORDER BY benefice DESC
    """)
List<TopVenteProjection> topVentesBetween(
        @Param("userId") Long userId,
        @Param("start") LocalDate start,
        @Param("end") LocalDate end,
        @Param("categories") List<String> categories,
        @Param("categoriesAll") boolean categoriesAll
);
    
default List<TopVenteProjection> topVentesYear(Long userId, int year) {
    return topVentesBetween(
            userId,
            LocalDate.of(year, 1, 1),
            LocalDate.of(year, 12, 31),
            List.of("__all__"),
            true
    );
}
 
 interface LabelCount {
    String getLabel();
    long getNb();
  }

  @Query("""
    SELECT v.categorie AS label, COUNT(v) AS nb
    FROM SnkVente v
    WHERE v.user.id = :userId
    GROUP BY v.categorie
    ORDER BY nb DESC
  """)
  List<LabelCount> topCategories(@Param("userId") Long userId, PageRequest pageable);

@Query("""
  SELECT v.nomItem AS label, COUNT(v) AS nb
  FROM SnkVente v
  WHERE v.user.id = :userId
    AND v.categorie = :categorie
  GROUP BY v.nomItem
  ORDER BY nb DESC
""")
List<LabelCount> topItemsByCategorie(
    @Param("userId") Long userId,
    @Param("categorie") String categorie,
    PageRequest pageable
);

@Query(value = """
  SELECT COALESCE(SUM(t.prix_resell), 0)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
""", nativeQuery = true)
BigDecimal caBetween(@Param("userId") Long userId,
                     @Param("start") LocalDate start,
                     @Param("end") LocalDate end,
                     @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT COALESCE(SUM(t.prix_resell - t.prix_retail), 0)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
    AND t.prix_retail IS NOT NULL
""", nativeQuery = true)
BigDecimal profitBetween(@Param("userId") Long userId,
                         @Param("start") LocalDate start,
                         @Param("end") LocalDate end,
                         @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT COUNT(*)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
long countSoldBetween(@Param("userId") Long userId,
                      @Param("start") LocalDate start,
                      @Param("end") LocalDate end,
                      @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT COUNT(*)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente IS NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
long countInStock(@Param("userId") Long userId,
                  @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT COALESCE(SUM(COALESCE(t.prix_retail, 0)), 0)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente IS NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
BigDecimal stockValue(@Param("userId") Long userId,
                      @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT MIN(t.date_achat)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_achat IS NOT NULL
""", nativeQuery = true)
LocalDate minAchatDate(@Param("userId") Long userId);

@Query(value = """
  SELECT MIN(t.date_vente)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente IS NOT NULL
""", nativeQuery = true)
LocalDate minVenteDate(@Param("userId") Long userId);

@Query(value = """
  SELECT COUNT(*)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_achat IS NOT NULL
    AND t.date_achat <= :asOf
    AND (t.date_vente IS NULL OR t.date_vente > :asOf)
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
long countInStockAt(@Param("userId") Long userId,
                    @Param("asOf") LocalDate asOf,
                    @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT COALESCE(SUM(COALESCE(t.prix_retail, 0)), 0)
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_achat IS NOT NULL
    AND t.date_achat <= :asOf
    AND (t.date_vente IS NULL OR t.date_vente > :asOf)
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
BigDecimal stockValueAt(@Param("userId") Long userId,
                        @Param("asOf") LocalDate asOf,
                        @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('day', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointRow> timeseriesDay(@Param("userId") Long userId,
                                 @Param("start") LocalDate start,
                                 @Param("end") LocalDate end,
                                 @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('week', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointRow> timeseriesWeek(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('month', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointRow> timeseriesMonth(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end,
                                   @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('day', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit,
         COUNT(*) AS nb
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointFullRow> timeseriesDayFull(@Param("userId") Long userId,
                                         @Param("start") LocalDate start,
                                         @Param("end") LocalDate end,
                                         @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('week', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit,
         COUNT(*) AS nb
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointFullRow> timeseriesWeekFull(@Param("userId") Long userId,
                                          @Param("start") LocalDate start,
                                          @Param("end") LocalDate end,
                                          @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('month', t.date_vente))::date AS bucket,
         COALESCE(SUM(COALESCE(t.prix_resell,0)),0) AS ca,
         COALESCE(SUM(COALESCE(t.prix_resell,0) - COALESCE(t.prix_retail,0)),0) AS profit,
         COUNT(*) AS nb
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
    AND t.prix_resell IS NOT NULL
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<TimePointFullRow> timeseriesMonthFull(@Param("userId") Long userId,
                                           @Param("start") LocalDate start,
                                           @Param("end") LocalDate end,
                                           @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('day', t.date_vente))::date AS bucket,
         AVG(t.date_vente - t.date_achat) AS avg_days
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND t.date_vente IS NOT NULL
    AND t.date_achat IS NOT NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<AvgDaysRow> avgDaysToSellDay(@Param("userId") Long userId,
                                  @Param("start") LocalDate start,
                                  @Param("end") LocalDate end,
                                  @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('week', t.date_vente))::date AS bucket,
         AVG(t.date_vente - t.date_achat) AS avg_days
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND t.date_vente IS NOT NULL
    AND t.date_achat IS NOT NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<AvgDaysRow> avgDaysToSellWeek(@Param("userId") Long userId,
                                   @Param("start") LocalDate start,
                                   @Param("end") LocalDate end,
                                   @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT (date_trunc('month', t.date_vente))::date AS bucket,
         AVG(t.date_vente - t.date_achat) AS avg_days
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND t.date_vente IS NOT NULL
    AND t.date_achat IS NOT NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
  GROUP BY bucket
  ORDER BY bucket
""", nativeQuery = true)
List<AvgDaysRow> avgDaysToSellMonth(@Param("userId") Long userId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT AVG(t.date_vente - t.date_achat) AS avg_days
  FROM public.tableauventes t
  WHERE t.user_id = :userId
    AND t.date_vente BETWEEN :start AND :end
    AND t.date_vente IS NOT NULL
    AND t.date_achat IS NOT NULL
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
""", nativeQuery = true)
Double avgDaysToSellBetween(@Param("userId") Long userId,
                            @Param("start") LocalDate start,
                            @Param("end") LocalDate end,
                            @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query(value = """
  SELECT
    s.label AS label,
    COUNT(*)::numeric AS value
  FROM (
    SELECT
      CASE
        WHEN (CURRENT_DATE - t.date_achat) <= 30 THEN '0-30'
        WHEN (CURRENT_DATE - t.date_achat) <= 90 THEN '31-90'
        WHEN (CURRENT_DATE - t.date_achat) <= 180 THEN '91-180'
        ELSE '180+'
      END AS label
    FROM public.tableauventes t
    WHERE t.user_id = :userId
      AND t.date_vente IS NULL
      AND t.date_achat IS NOT NULL
      AND (:categoriesAll = true OR COALESCE(NULLIF(trim(t.categorie), ''), 'Autre') = ANY(:categories))
  ) s
  GROUP BY s.label
  ORDER BY
    CASE s.label
      WHEN '0-30' THEN 1
      WHEN '31-90' THEN 2
      WHEN '91-180' THEN 3
      ELSE 4
    END
""", nativeQuery = true)
List<LabelValueRow> deathPileAge(@Param("userId") Long userId,
                                 @Param("categories") String[] categories,
                      @Param("categoriesAll") boolean categoriesAll);

@Query("""
  SELECT
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END AS label,
    SUM(COALESCE(v.prixResell,0) - COALESCE(v.prixRetail,0)) AS value
  FROM SnkVente v
  WHERE v.user.id = :userId
    AND v.dateVente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(v.categorie), ''), 'Autre') IN (:categories))
    AND v.prixResell IS NOT NULL
    AND v.prixRetail IS NOT NULL
  GROUP BY
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
  ORDER BY value DESC
""")
List<LabelValueRow> topBrandsProfit(@Param("userId") Long userId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("categories") List<String> categories,
                                    @Param("categoriesAll") boolean categoriesAll);

@Query("""
  SELECT
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END AS label,
    SUM(COALESCE(v.prixResell,0) - COALESCE(v.prixRetail,0)) AS value
  FROM SnkVente v
  WHERE v.user.id = :userId
    AND v.dateVente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(v.categorie), ''), 'Autre') IN (:categories))
    AND v.prixResell IS NOT NULL
    AND v.prixRetail IS NOT NULL
  GROUP BY
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
  ORDER BY value DESC
""")
List<LabelValueRow> topCategoriesProfit(@Param("userId") Long userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end,
                                        @Param("categories") List<String> categories,
                                    @Param("categoriesAll") boolean categoriesAll);


@Query("""
  SELECT
    CASE
      WHEN lower(v.nomItem) LIKE '%nike%' THEN 'Nike'
      WHEN lower(v.nomItem) LIKE '%adidas%' THEN 'Adidas'
      WHEN lower(v.nomItem) LIKE '%jordan%' THEN 'Jordan'
      WHEN lower(v.nomItem) LIKE '%pokemon%' THEN 'Pokemon'
      ELSE 'Autre'
    END AS label,
    COUNT(v) AS nb
  FROM SnkVente v
  WHERE v.user.id = :userId
    AND v.dateVente BETWEEN :start AND :end
    AND (:categoriesAll = true OR COALESCE(NULLIF(trim(v.categorie), ''), 'Autre') IN (:categories))
  GROUP BY
    CASE
      WHEN lower(v.nomItem) LIKE '%nike%' THEN 'Nike'
      WHEN lower(v.nomItem) LIKE '%adidas%' THEN 'Adidas'
      WHEN lower(v.nomItem) LIKE '%jordan%' THEN 'Jordan'
      WHEN lower(v.nomItem) LIKE '%pokemon%' THEN 'Pokemon'
      ELSE 'Autre'
    END
  ORDER BY nb DESC
""")
List<LabelCount> brandBreakdownSales(@Param("userId") Long userId,
                                    @Param("start") LocalDate start,
                                    @Param("end") LocalDate end,
                                    @Param("categories") List<String> categories,
                                    @Param("categoriesAll") boolean categoriesAll);

@Query("""
  SELECT DISTINCT
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
  FROM SnkVente v
  WHERE v.user.id = :userId
  ORDER BY
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
""")
List<String> distinctCategories(@Param("userId") Long userId);

@Query("""
  SELECT DISTINCT
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
  FROM SnkVente v
  WHERE v.user.id = :userId
    AND (
      (v.dateVente BETWEEN :from AND :to)
      OR (v.dateAchat BETWEEN :from AND :to)
    )
  ORDER BY
    CASE
      WHEN v.categorie IS NULL OR trim(v.categorie) = '' THEN 'Autre'
      ELSE v.categorie
    END
""")
List<String> distinctCategoriesBetween(
    @Param("userId") Long userId,
    @Param("from") LocalDate from,
    @Param("to") LocalDate to
);
                                


   
}









