/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

-- =====================
-- COUNTRIES
-- =====================
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'United States', 'US');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'United Kingdom', 'GB');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Canada', 'CA');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Australia', 'AU');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Germany', 'DE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'France', 'FR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Japan', 'JP');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'China', 'CN');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'India', 'IN');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Brazil', 'BR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Egypt', 'EG');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'South Africa', 'ZA');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Mexico', 'MX');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Spain', 'ES');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Italy', 'IT');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Netherlands', 'NL');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Sweden', 'SE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Norway', 'NO');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Denmark', 'DK');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Finland', 'FI');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Poland', 'PL');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Russia', 'RU');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'South Korea', 'KR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Singapore', 'SG');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'United Arab Emirates', 'AE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Saudi Arabia', 'SA');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Turkey', 'TR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Argentina', 'AR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Chile', 'CL');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Colombia', 'CO');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Peru', 'PE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'New Zealand', 'NZ');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Ireland', 'IE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Switzerland', 'CH');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Austria', 'AT');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Belgium', 'BE');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Portugal', 'PT');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Greece', 'GR');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Czech Republic', 'CZ');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Romania', 'RO');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Hungary', 'HU');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Ukraine', 'UA');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Thailand', 'TH');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Vietnam', 'VN');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Indonesia', 'ID');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Malaysia', 'MY');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Philippines', 'PH');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Pakistan', 'PK');
INSERT INTO countries (id, name, code) VALUES (nextval('country_seq'), 'Bangladesh', 'BD');

-- =====================
-- TIMEZONES
-- =====================
-- United States
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/New_York', (SELECT id FROM countries WHERE code = 'US'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Chicago', (SELECT id FROM countries WHERE code = 'US'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Denver', (SELECT id FROM countries WHERE code = 'US'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Los_Angeles', (SELECT id FROM countries WHERE code = 'US'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Anchorage', (SELECT id FROM countries WHERE code = 'US'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Pacific/Honolulu', (SELECT id FROM countries WHERE code = 'US'));

-- United Kingdom
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/London', (SELECT id FROM countries WHERE code = 'GB'));

-- Canada
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Toronto', (SELECT id FROM countries WHERE code = 'CA'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Vancouver', (SELECT id FROM countries WHERE code = 'CA'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Edmonton', (SELECT id FROM countries WHERE code = 'CA'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Halifax', (SELECT id FROM countries WHERE code = 'CA'));

-- Australia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Australia/Sydney', (SELECT id FROM countries WHERE code = 'AU'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Australia/Melbourne', (SELECT id FROM countries WHERE code = 'AU'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Australia/Brisbane', (SELECT id FROM countries WHERE code = 'AU'));
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Australia/Perth', (SELECT id FROM countries WHERE code = 'AU'));

-- Germany
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Berlin', (SELECT id FROM countries WHERE code = 'DE'));

-- France
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Paris', (SELECT id FROM countries WHERE code = 'FR'));

-- Japan
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Tokyo', (SELECT id FROM countries WHERE code = 'JP'));

-- China
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Shanghai', (SELECT id FROM countries WHERE code = 'CN'));

-- India
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Kolkata', (SELECT id FROM countries WHERE code = 'IN'));

-- Brazil
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Sao_Paulo', (SELECT id FROM countries WHERE code = 'BR'));

-- Egypt
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Africa/Cairo', (SELECT id FROM countries WHERE code = 'EG'));

-- South Africa
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Africa/Johannesburg', (SELECT id FROM countries WHERE code = 'ZA'));

-- Mexico
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Mexico_City', (SELECT id FROM countries WHERE code = 'MX'));

-- Spain
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Madrid', (SELECT id FROM countries WHERE code = 'ES'));

-- Italy
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Rome', (SELECT id FROM countries WHERE code = 'IT'));

-- Netherlands
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Amsterdam', (SELECT id FROM countries WHERE code = 'NL'));

-- Sweden
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Stockholm', (SELECT id FROM countries WHERE code = 'SE'));

-- Norway
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Oslo', (SELECT id FROM countries WHERE code = 'NO'));

-- Denmark
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Copenhagen', (SELECT id FROM countries WHERE code = 'DK'));

-- Finland
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Helsinki', (SELECT id FROM countries WHERE code = 'FI'));

-- Poland
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Warsaw', (SELECT id FROM countries WHERE code = 'PL'));

-- Russia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Moscow', (SELECT id FROM countries WHERE code = 'RU'));

-- South Korea
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Seoul', (SELECT id FROM countries WHERE code = 'KR'));

-- Singapore
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Singapore', (SELECT id FROM countries WHERE code = 'SG'));

-- United Arab Emirates
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Dubai', (SELECT id FROM countries WHERE code = 'AE'));

-- Saudi Arabia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Riyadh', (SELECT id FROM countries WHERE code = 'SA'));

-- Israel
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Jerusalem', (SELECT id FROM countries WHERE code = 'IL'));

-- Turkey
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Istanbul', (SELECT id FROM countries WHERE code = 'TR'));

-- Argentina
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Buenos_Aires', (SELECT id FROM countries WHERE code = 'AR'));

-- Chile
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Santiago', (SELECT id FROM countries WHERE code = 'CL'));

-- Colombia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Bogota', (SELECT id FROM countries WHERE code = 'CO'));

-- Peru
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'America/Lima', (SELECT id FROM countries WHERE code = 'PE'));

-- New Zealand
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Pacific/Auckland', (SELECT id FROM countries WHERE code = 'NZ'));

-- Ireland
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Dublin', (SELECT id FROM countries WHERE code = 'IE'));

-- Switzerland
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Zurich', (SELECT id FROM countries WHERE code = 'CH'));

-- Austria
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Vienna', (SELECT id FROM countries WHERE code = 'AT'));

-- Belgium
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Brussels', (SELECT id FROM countries WHERE code = 'BE'));

-- Portugal
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Lisbon', (SELECT id FROM countries WHERE code = 'PT'));

-- Greece
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Athens', (SELECT id FROM countries WHERE code = 'GR'));

-- Czech Republic
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Prague', (SELECT id FROM countries WHERE code = 'CZ'));

-- Romania
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Bucharest', (SELECT id FROM countries WHERE code = 'RO'));

-- Hungary
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Budapest', (SELECT id FROM countries WHERE code = 'HU'));

-- Ukraine
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Europe/Kiev', (SELECT id FROM countries WHERE code = 'UA'));

-- Thailand
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Bangkok', (SELECT id FROM countries WHERE code = 'TH'));

-- Vietnam
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Ho_Chi_Minh', (SELECT id FROM countries WHERE code = 'VN'));

-- Indonesia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Jakarta', (SELECT id FROM countries WHERE code = 'ID'));

-- Malaysia
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Kuala_Lumpur', (SELECT id FROM countries WHERE code = 'MY'));

-- Philippines
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Manila', (SELECT id FROM countries WHERE code = 'PH'));

-- Pakistan
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Karachi', (SELECT id FROM countries WHERE code = 'PK'));

-- Bangladesh
INSERT INTO timezones (id, name, country_id) VALUES (nextval('timezone_seq'), 'Asia/Dhaka', (SELECT id FROM countries WHERE code = 'BD'));

-- =====================
-- USERS
-- =====================
INSERT INTO users (id, name, full_name, email, phone_number, phone_extension, country_id, timezone_id, user_type, password_hash)
VALUES (nextval('user_seq'), 'admin', 'System Administrator', 'admin@mnemosyne-systems.ai', '+1-555-0100', NULL,
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/New_York'),
        'admin', '\$2b$12$FskCWeYhIunkWrF0WXZVc.PUuxK0RAjw6Xir0gHg7hBu3t5qohzca')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, full_name, email, phone_number, phone_extension, country_id, timezone_id, user_type, password_hash)
VALUES (nextval('user_seq'), 'support1', 'Sarah Johnson', 'support1@mnemosyne-systems.ai', '+1-555-0101', '101',
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/New_York'),
        'support', '\$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, full_name, email, phone_number, phone_extension, country_id, timezone_id, user_type, password_hash)
VALUES (nextval('user_seq'), 'support2', 'Michael Chen', 'support2@mnemosyne-systems.ai', '+1-555-0102', '102',
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/Los_Angeles'),
        'support', '\$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, full_name, email, phone_number, phone_extension, country_id, timezone_id, user_type, password_hash)
VALUES (nextval('user_seq'), 'user', 'Demo User', 'user@mnemosyne-systems.ai', '+1-555-0200', NULL,
        (SELECT id FROM countries WHERE code = 'GB'),
        (SELECT id FROM timezones WHERE name = 'Europe/London'),
        'user', '\$2b$12$jik3uV5QEO43S7nS3p9o/.OBR0RWEU3RQ4B/XGlPnjyh0NB7Mm3Y.')
ON CONFLICT (email) DO NOTHING;

INSERT INTO users (id, name, full_name, email, phone_number, phone_extension, country_id, timezone_id, user_type, password_hash)
VALUES (nextval('user_seq'), 'tam', 'Technical Account Manager', 'tam@mnemosyne-systems.ai', '+1-555-0300', '300',
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/Chicago'),
        'tam', '\$2b$12$raOKDcLqK.0gZ.HG0yMd2udJFYwr1vZxngYTrHKUM9ihp/yF54Z2a')
ON CONFLICT (email) DO NOTHING;

-- =====================
-- ENTITLEMENTS
-- =====================
INSERT INTO entitlements (id, name, description)
VALUES (nextval('entitlement_seq'), 'Starter', 'Email support with 2 business day response');

INSERT INTO entitlements (id, name, description)
VALUES (nextval('entitlement_seq'), 'Business', 'Priority support with 1 business day response');

INSERT INTO entitlements (id, name, description)
VALUES (nextval('entitlement_seq'), 'Enterprise', '24/7 support with SLA and dedicated TAM');

-- =====================
-- LEVELS
-- =====================
INSERT INTO support_levels (id, name, description, level, color, from_day, from_time, to_day, to_time, country_id, timezone_id)
VALUES (nextval('support_level_seq'), 'Critical', 'Critical response level', 60, 'Red',
        1, 0, 7, 23,
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/New_York'));

INSERT INTO support_levels (id, name, description, level, color, from_day, from_time, to_day, to_time, country_id, timezone_id)
VALUES (nextval('support_level_seq'), 'Eslacate', 'Escalation response level', 120, 'Yellow',
        1, 0, 7, 23,
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/New_York'));

INSERT INTO support_levels (id, name, description, level, color, from_day, from_time, to_day, to_time, country_id, timezone_id)
VALUES (nextval('support_level_seq'), 'Normal', 'Normal response level', 1440, 'White',
        1, 0, 7, 23,
        (SELECT id FROM countries WHERE code = 'US'),
        (SELECT id FROM timezones WHERE name = 'America/New_York'));

-- =====================
-- ENTITLEMENT LEVELS
-- =====================
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Starter' AND s.name = 'Critical';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Starter' AND s.name = 'Eslacate';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Starter' AND s.name = 'Normal';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Business' AND s.name = 'Critical';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Business' AND s.name = 'Eslacate';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Business' AND s.name = 'Normal';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Enterprise' AND s.name = 'Critical';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Enterprise' AND s.name = 'Eslacate';
INSERT INTO entitlement_support_levels (entitlement_id, support_level_id)
SELECT e.id, s.id
FROM entitlements e, support_levels s
WHERE e.name = 'Enterprise' AND s.name = 'Normal';

-- =====================
-- CATEGORIES
-- =====================
INSERT INTO categories (id, name, is_default) VALUES (nextval('category_seq'), 'Feature', false);
INSERT INTO categories (id, name, is_default) VALUES (nextval('category_seq'), 'Bug', false);
INSERT INTO categories (id, name, is_default) VALUES (nextval('category_seq'), 'Question', true);
