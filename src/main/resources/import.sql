/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'admin', 'admin@mnemosyne-systems.ai', 'admin', '$2b$12$FskCWeYhIunkWrF0WXZVc.PUuxK0RAjw6Xir0gHg7hBu3t5qohzca')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    user_type = EXCLUDED.user_type,
    password_hash = EXCLUDED.password_hash;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'support1', 'support1@mnemosyne-systems.ai', 'support', '$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    user_type = EXCLUDED.user_type,
    password_hash = EXCLUDED.password_hash;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'support2', 'support2@mnemosyne-systems.ai', 'support', '$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    user_type = EXCLUDED.user_type,
    password_hash = EXCLUDED.password_hash;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'user', 'user@mnemosyne-systems.ai', 'user', '$2b$12$jik3uV5QEO43S7nS3p9o/.OBR0RWEU3RQ4B/XGlPnjyh0NB7Mm3Y.')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    user_type = EXCLUDED.user_type,
    password_hash = EXCLUDED.password_hash;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'tam', 'tam@mnemosyne-systems.ai', 'tam', '$2b$12$raOKDcLqK.0gZ.HG0yMd2udJFYwr1vZxngYTrHKUM9ihp/yF54Z2a')
ON CONFLICT (email) DO UPDATE
SET name = EXCLUDED.name,
    user_type = EXCLUDED.user_type,
    password_hash = EXCLUDED.password_hash;

INSERT INTO entitlements (id, name, description, price)
VALUES (nextval('entitlement_seq'), 'Starter', 'Email support with 2 business day response', 99);

INSERT INTO entitlements (id, name, description, price)
VALUES (nextval('entitlement_seq'), 'Business', 'Priority support with 1 business day response', 249);

INSERT INTO entitlements (id, name, description, price)
VALUES (nextval('entitlement_seq'), 'Enterprise', '24/7 support with SLA and dedicated TAM', 499);

INSERT INTO support_levels (id, name, description, critical, critical_color, escalate, escalate_color, normal, normal_color)
VALUES (nextval('support_level_seq'), 'Low', 'Standard response window', 60, 'Red', 720, 'Yellow', 1440, 'White');

INSERT INTO support_levels (id, name, description, critical, critical_color, escalate, escalate_color, normal, normal_color)
VALUES (nextval('support_level_seq'), 'Normal', 'Default response window', 60, 'Red', 120, 'Yellow', 720, 'White');

INSERT INTO support_levels (id, name, description, critical, critical_color, escalate, escalate_color, normal, normal_color)
VALUES (nextval('support_level_seq'), 'High', 'Escalated response window', 60, 'Red', 90, 'Yellow', 120, 'White');
