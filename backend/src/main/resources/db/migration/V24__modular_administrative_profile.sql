ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS administrative_legal_status VARCHAR(40),
  ADD COLUMN IF NOT EXISTS administrative_activities VARCHAR(500),
  ADD COLUMN IF NOT EXISTS administrative_vat_regime VARCHAR(40),
  ADD COLUMN IF NOT EXISTS administrative_declarations VARCHAR(500),
  ADD COLUMN IF NOT EXISTS business_name VARCHAR(240),
  ADD COLUMN IF NOT EXISTS owner_name VARCHAR(240),
  ADD COLUMN IF NOT EXISTS legal_form VARCHAR(80),
  ADD COLUMN IF NOT EXISTS fiscal_year_end_month INTEGER,
  ADD COLUMN IF NOT EXISTS fiscal_year_end_day INTEGER,
  ADD COLUMN IF NOT EXISTS declaration_periodicity VARCHAR(40),
  ADD COLUMN IF NOT EXISTS urssaf_category VARCHAR(120),
  ADD COLUMN IF NOT EXISTS default_vat_rate NUMERIC(5, 2);

UPDATE public.users
SET administrative_legal_status = CASE
  WHEN legal_profile_type = 'MICRO_ENTREPRISE' THEN 'micro'
  WHEN legal_profile_type = 'MICRO_ENTREPRISE_UNDER_200K_YEAR' THEN 'micro'
  WHEN legal_profile_type = 'PARTICULIER' THEN 'personal'
  WHEN legal_profile_type = 'INDIVIDUAL_UNDER_5K_MONTH' THEN 'personal'
  WHEN legal_profile_type IS NULL THEN 'none'
  ELSE lower(legal_profile_type)
END
WHERE administrative_legal_status IS NULL;

UPDATE public.users
SET administrative_activities = COALESCE(administrative_activities, 'goods_resale'),
    administrative_declarations = COALESCE(
      administrative_declarations,
      CASE
        WHEN declaration_frequency = 'MONTHLY' THEN 'urssaf_monthly,cfe'
        ELSE 'urssaf_quarterly,cfe'
      END
    ),
    administrative_vat_regime = COALESCE(
      administrative_vat_regime,
      CASE
        WHEN vat_franchise = 'YES' THEN 'franchise_base'
        ELSE 'unknown'
      END
    ),
    declaration_periodicity = COALESCE(
      declaration_periodicity,
      CASE
        WHEN declaration_frequency = 'MONTHLY' THEN 'urssaf_monthly'
        WHEN declaration_frequency = 'QUARTERLY' THEN 'urssaf_quarterly'
        ELSE NULL
      END
    ),
    urssaf_category = COALESCE(urssaf_category, 'Vente de marchandises'),
    legal_form = COALESCE(legal_form, 'Micro-entreprise'),
    fiscal_year_end_month = COALESCE(fiscal_year_end_month, 12),
    fiscal_year_end_day = COALESCE(fiscal_year_end_day, 31)
WHERE administrative_legal_status = 'micro';

UPDATE public.users
SET administrative_activities = COALESCE(administrative_activities, 'second_hand'),
    administrative_vat_regime = COALESCE(administrative_vat_regime, 'unknown'),
    fiscal_year_end_month = COALESCE(fiscal_year_end_month, 12),
    fiscal_year_end_day = COALESCE(fiscal_year_end_day, 31)
WHERE administrative_legal_status IN ('none', 'personal');
