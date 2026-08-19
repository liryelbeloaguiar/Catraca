UPDATE employee_profiles
SET badge_code = 'CAT-' || substring(badge_code FROM 4), updated_at = now()
WHERE badge_code LIKE 'QF-%';

UPDATE organization_settings
SET establishment_name = 'Catraca', updated_at = now()
WHERE establishment_name = 'QueueFlow';
