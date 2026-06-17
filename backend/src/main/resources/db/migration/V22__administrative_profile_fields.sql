ALTER TABLE public.users
  ADD COLUMN IF NOT EXISTS siren VARCHAR(9),
  ADD COLUMN IF NOT EXISTS administrative_display_name VARCHAR(240),
  ADD COLUMN IF NOT EXISTS trade_name VARCHAR(240),
  ADD COLUMN IF NOT EXISTS administrative_address VARCHAR(500),
  ADD COLUMN IF NOT EXISTS main_activity VARCHAR(160),
  ADD COLUMN IF NOT EXISTS fiscal_regime VARCHAR(120),
  ADD COLUMN IF NOT EXISTS withholding_tax_option VARCHAR(20),
  ADD COLUMN IF NOT EXISTS vat_franchise VARCHAR(20),
  ADD COLUMN IF NOT EXISTS activity_start_date DATE,
  ADD COLUMN IF NOT EXISTS administrative_notes TEXT,
  ADD COLUMN IF NOT EXISTS administrative_verification_status VARCHAR(80),
  ADD COLUMN IF NOT EXISTS admin_uses_online_platforms BOOLEAN DEFAULT false NOT NULL,
  ADD COLUMN IF NOT EXISTS admin_buys_for_resale BOOLEAN DEFAULT false NOT NULL;

UPDATE public.users
SET legal_profile_type = 'PARTICULIER'
WHERE legal_profile_type = 'INDIVIDUAL_UNDER_5K_MONTH';

UPDATE public.users
SET legal_profile_type = 'MICRO_ENTREPRISE'
WHERE legal_profile_type = 'MICRO_ENTREPRISE_UNDER_200K_YEAR';

UPDATE public.users
SET siren = substring(siret from 1 for 9)
WHERE siret ~ '^[0-9]{14}$'
  AND (siren IS NULL OR siren = '');

UPDATE public.users
SET
  main_activity = COALESCE(main_activity, 'Achat-revente de marchandises'),
  fiscal_regime = COALESCE(fiscal_regime, 'Micro-BIC achat-revente'),
  withholding_tax_option = COALESCE(withholding_tax_option, 'UNKNOWN'),
  vat_franchise = COALESCE(vat_franchise, 'UNKNOWN'),
  administrative_verification_status = COALESCE(administrative_verification_status, 'A_VERIFIER')
WHERE legal_profile_type = 'MICRO_ENTREPRISE';

CREATE INDEX IF NOT EXISTS idx_users_administrative_profile_type
  ON public.users(legal_profile_type);
