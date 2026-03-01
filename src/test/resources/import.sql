/*
 * Eclipse Public License - v 2.0
 *
 *   THE ACCOMPANYING PROGRAM IS PROVIDED UNDER THE TERMS OF THIS ECLIPSE
 *   PUBLIC LICENSE ("AGREEMENT"). ANY USE, REPRODUCTION OR DISTRIBUTION
 *   OF THE PROGRAM CONSTITUTES RECIPIENT'S ACCEPTANCE OF THIS AGREEMENT.
 */

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'admin', 'admin@mnemosyne-systems.ai', 'admin', '$2b$12$FskCWeYhIunkWrF0WXZVc.PUuxK0RAjw6Xir0gHg7hBu3t5qohzca')
;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'support1', 'support1@mnemosyne-systems.ai', 'support', '$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'support2', 'support2@mnemosyne-systems.ai', 'support', '$2b$12$Z8QAXEmmgoprqsDg4Om4SeE0xWnCiAN86QaWntvoV5bd3mrP0ViEW')
;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'user', 'user@mnemosyne-systems.ai', 'user', '$2b$12$jik3uV5QEO43S7nS3p9o/.OBR0RWEU3RQ4B/XGlPnjyh0NB7Mm3Y.')
;

INSERT INTO users (id, name, email, user_type, password_hash)
VALUES (nextval('user_seq'), 'tam1', 'tam1@mnemosyne-systems.ai', 'tam', '$2b$12$OPDXvPZTCdwgSBbhyxOlmedXUriIH8kV4gFVRmIu/iK4qGuMcbIve')
;

INSERT INTO companies (id, name)
VALUES (nextval('company_seq'), 'mnemosyne systems')
;

INSERT INTO company_users (company_id, user_id)
SELECT c.id, u.id
FROM companies c, users u
WHERE c.name = 'mnemosyne systems' AND u.email = 'admin@mnemosyne-systems.ai'
;
