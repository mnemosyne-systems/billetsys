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
VALUES (nextval('user_seq'), 'tam', 'tam@mnemosyne-systems.ai', 'tam', '$2b$12$raOKDcLqK.0gZ.HG0yMd2udJFYwr1vZxngYTrHKUM9ihp/yF54Z2a')
;
