-- V2__seed_roles_users_and_members.sql
-- Seeds roles, users, and sample member data.

-- ========================
-- Insert roles
-- ========================
INSERT INTO surestDB.role (id, name)
VALUES
  (gen_random_uuid(), 'ROLE_ADMIN'),
  (gen_random_uuid(), 'ROLE_USER')
ON CONFLICT (name) DO NOTHING;

-- ========================
-- Insert users
-- ========================
-- Example bcrypt password_hash hashes:
-- Admin: Admin@123 -> $2b$12$A/v67jBCWcMaXYRAGlc.F.vcrVXRWHiFelfZKEOxUAtMNWf8YxB/u
-- User : User@123  -> $2b$12$ZAufyDFKolDdk.V76iJ2I.KD/3PblKqLyMm4LetGPxVayfKfoWuDS

-- Admin user
INSERT INTO surestDB.users (id, username, password_hash, role_id)
SELECT gen_random_uuid(), 'admin',
       '$2b$12$A/v67jBCWcMaXYRAGlc.F.vcrVXRWHiFelfZKEOxUAtMNWf8YxB/u',
       r.id
FROM surestDB.role r
WHERE r.name = 'ROLE_ADMIN'
  AND NOT EXISTS (SELECT 1 FROM surestDB.users u WHERE u.username = 'admin');

-- Regular user
INSERT INTO surestDB.users (id, username, password_hash, role_id)
SELECT gen_random_uuid(), 'user',
       '$2b$12$ZAufyDFKolDdk.V76iJ2I.KD/3PblKqLyMm4LetGPxVayfKfoWuDS',
       r.id
FROM surestDB.role r
WHERE r.name = 'ROLE_USER'
  AND NOT EXISTS (SELECT 1 FROM surestDB.users u WHERE u.username = 'user');

-- ========================
-- Insert sample members
-- ========================
INSERT INTO surestDB.member (id, first_name, last_name, email, date_of_birth)
VALUES
  (gen_random_uuid(), 'Alice', 'Singh', 'alice.singh@example.com', '1990-05-12'),
  (gen_random_uuid(), 'Rahul', 'Kumar', 'rahul.kumar@example.com', '1985-11-03'),
  (gen_random_uuid(), 'Priya', 'Patel', 'priya.patel@example.com', '1992-08-25')
ON CONFLICT (email) DO NOTHING;
