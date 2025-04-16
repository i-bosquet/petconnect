-- data.sql for PetConnect initial data seeding

-- =============================================
-- INSERT ROLES
-- =============================================
-- Assuming RoleEnum values are 'OWNER', 'VET', 'ADMIN', 'SUPERUSER'
-- IDs will be generated automatically (1, 2, 3, 4 if table is empty)
INSERT INTO roles (role_name) VALUES ('OWNER') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('VET') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('ADMIN') ON CONFLICT (role_name) DO NOTHING;
INSERT INTO roles (role_name) VALUES ('SUPERUSER') ON CONFLICT (role_name) DO NOTHING;

-- =============================================
-- INSERT PERMISSIONS
-- =============================================
-- IDs will be generated automatically
INSERT INTO permissions (name, description) VALUES ('USER_READ_PROFILE_OWN', 'Allows user to read their own profile information.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('USER_UPDATE_PROFILE_OWN', 'Allows user to update their own editable profile information.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_READ_PUBLIC', 'Allows any user (including guests) to search and view public clinic information.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_UPDATE_OWN', 'Allows an Admin user to update the information of their own clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_STAFF_CREATE', 'Allows an Admin user to create new Vet or Admin accounts for their own clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_STAFF_READ_OWN_CLINIC', 'Allows Admin and Vet users to view the staff list of their own clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_STAFF_UPDATE_OWN_CLINIC', 'Allows an Admin user to update information of staff members in their own clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CLINIC_STAFF_TOGGLE_ACTIVE_OWN_CLINIC', 'Allows an Admin user to activate or deactivate staff accounts in their own clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_CREATE_OWN', 'Allows an Owner user to register a new pet with basic information.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_READ_OWN', 'Allows an Owner user to view the list and details of their own pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_READ_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to view the list and details of pets associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_UPDATE_BASIC_OWN', 'Allows an Owner user to update basic editable information (like name, image) of their own pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_UPDATE_CLINICAL_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to update clinical information of pets associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_ACTIVATE_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to activate a pet (status PENDING to ACTIVE) associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_DEACTIVATE_OWN', 'Allows an Owner user to mark their own pet as inactive.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('PET_MANAGE_VET_ASSOCIATION_OWN', 'Allows an Owner user to associate or disassociate vets with their own pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_CREATE_OWN_INFORMATIVE', 'Allows an Owner user to create non-clinical, informative medical records for their own pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_READ_OWN', 'Allows an Owner user to view the medical history of their own pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_READ_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to view the medical history of pets associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_UPDATE_OWN_INFORMATIVE', 'Allows an Owner user to edit the informative medical records they created.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_DELETE_OWN_INFORMATIVE', 'Allows an Owner user to delete the informative medical records they created.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_CREATE_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to create clinical medical records for pets associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_SIGN_OWN', 'Allows a Vet user to digitally sign a medical record they created.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_UPDATE_UNSIGNED_OWN_CLINIC', 'Allows Admin and Vet users to edit unsigned medical records created by staff in their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_DELETE_UNSIGNED_OWN_CLINIC', 'Allows Admin and Vet users to delete unsigned medical records created by staff in their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('RECORD_DELETE_SIGNED_OWN', 'Allows a Vet user to delete a signed medical record they created, if not linked to a certificate.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CERTIFICATE_REQUEST_OWN', 'Allows an Owner user to request a digital certificate for their pet from an associated vet.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CERTIFICATE_GENERATE_ASSOCIATED_CLINIC', 'Allows a Vet user to generate a digital certificate for a pet associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CERTIFICATE_READ_OWN', 'Allows an Owner user to view the digital certificates issued for their pets.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CERTIFICATE_READ_ASSOCIATED_CLINIC', 'Allows Admin and Vet users to view digital certificates for pets associated with their clinic.') ON CONFLICT (name) DO NOTHING;
INSERT INTO permissions (name, description) VALUES ('CERTIFICATE_SHARE_QR_OWN', 'Allows an Owner user to generate and share a QR code for verifying a digital certificate.') ON CONFLICT (name) DO NOTHING;

-- =============================================
-- ASSOCIATE PERMISSIONS WITH ROLES (role_permission table)
-- =============================================
-- Assigning permissions - Modify based on your exact requirements
-- NOTE: This assumes roles OWNER, VET, ADMIN have IDs 1, 2, 3 respectively after insertion. Adjust if needed.

-- OWNER Permissions (Role ID 1)
INSERT INTO role_permission (role_id, permission_id) VALUES
                                                         (1, (SELECT id FROM permissions WHERE name = 'USER_READ_PROFILE_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'USER_UPDATE_PROFILE_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'CLINIC_READ_PUBLIC')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'PET_CREATE_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'PET_READ_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'PET_UPDATE_BASIC_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'PET_DEACTIVATE_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'PET_MANAGE_VET_ASSOCIATION_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'RECORD_CREATE_OWN_INFORMATIVE')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'RECORD_READ_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'RECORD_UPDATE_OWN_INFORMATIVE')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'RECORD_DELETE_OWN_INFORMATIVE')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_REQUEST_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_READ_OWN')),
                                                         (1, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_SHARE_QR_OWN'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- VET Permissions (Role ID 2) - Includes reading own profile, clinic staff, pets, records, certificates + Vet actions
INSERT INTO role_permission (role_id, permission_id) VALUES
                                                         (2, (SELECT id FROM permissions WHERE name = 'USER_READ_PROFILE_OWN')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'USER_UPDATE_PROFILE_OWN')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'CLINIC_READ_PUBLIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'CLINIC_STAFF_READ_OWN_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'PET_READ_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'PET_UPDATE_CLINICAL_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'PET_ACTIVATE_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_READ_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_CREATE_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_SIGN_OWN')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_UPDATE_UNSIGNED_OWN_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_DELETE_UNSIGNED_OWN_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'RECORD_DELETE_SIGNED_OWN')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_GENERATE_ASSOCIATED_CLINIC')),
                                                         (2, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_READ_ASSOCIATED_CLINIC'))
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- ADMIN Permissions (Role ID 3) - Includes Vet permissions (except signing/deleting signed records) + Admin actions
INSERT INTO role_permission (role_id, permission_id) VALUES
                                                         (3, (SELECT id FROM permissions WHERE name = 'USER_READ_PROFILE_OWN')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'USER_UPDATE_PROFILE_OWN')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_READ_PUBLIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_UPDATE_OWN')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_STAFF_CREATE')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_STAFF_READ_OWN_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_STAFF_UPDATE_OWN_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CLINIC_STAFF_TOGGLE_ACTIVE_OWN_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'PET_READ_ASSOCIATED_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'PET_UPDATE_CLINICAL_ASSOCIATED_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'PET_ACTIVATE_ASSOCIATED_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'RECORD_READ_ASSOCIATED_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'RECORD_CREATE_ASSOCIATED_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'RECORD_UPDATE_UNSIGNED_OWN_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'RECORD_DELETE_UNSIGNED_OWN_CLINIC')),
                                                         (3, (SELECT id FROM permissions WHERE name = 'CERTIFICATE_READ_ASSOCIATED_CLINIC'))
-- Note: Admin does NOT get RECORD_SIGN_OWN, RECORD_DELETE_SIGNED_OWN, CERTIFICATE_GENERATE_ASSOCIATED_CLINIC by default
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- SUPERUSER Permissions (Role ID 4) - Grant all permissions for simplicity, or select specific admin ones
-- Example: Granting all permissions
INSERT INTO role_permission (role_id, permission_id)
SELECT 4, id FROM permissions
ON CONFLICT (role_id, permission_id) DO NOTHING;



-- =============================================
-- INSERT CLINICS AND THEIR ADMINS (Using unique consecutive IDs from entity_id_sequence implicitly)
-- =============================================
-- Pre-hashed password for 'password' is $2a$10$f9WJgO/vnifQCKzS3UAbAeehX78zXqRjA5cRA7L5wO9F0jKjdQ9.e

-- Clinic 1 (ID=1) & Admin 1 (ID=2)
INSERT INTO clinic (id, name, address, city, country, phone, public_key, created_at, created_by) VALUES
    (1, 'The London Vet Clinic', '123 Regent Street', 'London', 'UNITED_KINGDOM', '+44 20 1234 5678', 'LON_PUB_KEY_123', NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, username, email, password, avatar, is_enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, created_by) VALUES
    (2, 'admin_london', 'admin.london@petconnect.dev', '$2a$10$H9Lb.tMSwYl.Fa3F/aTqeuL8zP6racB694g49wkPrnm5pRqzDyX/e', 'images/avatars/users/admin.png', true, true, true, true, NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO clinic_staff (user_id, name, surname, is_active, clinic_id) VALUES
    (2, 'John', 'Smith', true, 1)
ON CONFLICT (user_id) DO NOTHING;

-- Clinic 2 (ID=3) & Admin 2 (ID=4)
INSERT INTO clinic (id, name, address, city, country, phone, public_key, created_at, created_by) VALUES
    (3, 'Manchester Pet Hospital', '45 Market Street', 'Manchester', 'UNITED_KINGDOM', '+44 161 987 6543', 'MAN_PUB_KEY_456', NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, username, email, password, avatar, is_enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, created_by) VALUES
    (4, 'admin_manchester', 'admin.manchester@petconnect.dev', '$2a$10$H9Lb.tMSwYl.Fa3F/aTqeuL8zP6racB694g49wkPrnm5pRqzDyX/e', 'images/avatars/users/admin.png', true, true, true, true, NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO clinic_staff (user_id, name, surname, is_active, clinic_id) VALUES
    (4, 'Emily', 'Jones', true, 3) -- User ID 4, Clinic ID 3
ON CONFLICT (user_id) DO NOTHING;

-- Clinic 3 (ID=5) & Admin 3 (ID=6) - Barcelona
INSERT INTO clinic (id, name, address, city, country, phone, public_key, created_at, created_by) VALUES
    (5, 'Clinica Veterinaria Barcelona Gracia', 'Carrer Gran de Gràcia 70', 'Barcelona', 'SPAIN', '+34 93 111 4455', 'BCN_PUB_KEY_789', NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, username, email, password, avatar, is_enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, created_by) VALUES
    (6, 'admin_barcelona', 'admin.barcelona@petconnect.dev', '$2a$10$H9Lb.tMSwYl.Fa3F/aTqeuL8zP6racB694g49wkPrnm5pRqzDyX/e', 'images/avatars/users/admin.png', true, true, true, true, NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO clinic_staff (user_id, name, surname, is_active, clinic_id) VALUES
    (6, 'Jordi', 'Vila', true, 5) -- User ID 6, Clinic ID 5
ON CONFLICT (user_id) DO NOTHING;

-- Clinic 4 (ID=7) & Admin 4 (ID=8) - Paris
INSERT INTO clinic (id, name, address, city, country, phone, public_key, created_at, created_by) VALUES
    (7, 'Clinique Vétérinaire Paris Étoile', '10 Avenue des Champs-Élysées', 'Paris', 'FRANCE', '+33 1 8888 9900', 'PAR_PUB_KEY_012', NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, username, email, password, avatar, is_enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, created_by) VALUES
    (8, 'admin_paris', 'admin.paris@petconnect.dev', '$2a$10$H9Lb.tMSwYl.Fa3F/aTqeuL8zP6racB694g49wkPrnm5pRqzDyX/e', 'images/avatars/users/admin.png', true, true, true, true, NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO clinic_staff (user_id, name, surname, is_active, clinic_id) VALUES
    (8, 'Sophie', 'Martin', true, 7) -- User ID 8, Clinic ID 7
ON CONFLICT (user_id) DO NOTHING;

-- Clinic 5 (ID=9) & Admin 5 (ID=10) - Berlin
INSERT INTO clinic (id, name, address, city, country, phone, public_key, created_at, created_by) VALUES
    (9, 'Tierklinik Berlin Mitte', 'Friedrichstraße 100', 'Berlin', 'GERMANY', '+49 30 555 6677', 'BER_PUB_KEY_345', NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO users (id, username, email, password, avatar, is_enabled, account_non_expired, account_non_locked, credentials_non_expired, created_at, created_by) VALUES
    (10, 'admin_berlin', 'admin_berlin@petconnect.dev', '$2a$10$H9Lb.tMSwYl.Fa3F/aTqeuL8zP6racB694g49wkPrnm5pRqzDyX/e', 'images/avatars/users/admin.png', true, true, true, true, NOW(), 'system')
ON CONFLICT (id) DO NOTHING;
INSERT INTO clinic_staff (user_id, name, surname, is_active, clinic_id) VALUES
    (10, 'Lukas', 'Schmidt', true, 9) -- User ID 10, Clinic ID 9
ON CONFLICT (user_id) DO NOTHING;

-- =============================================
-- ASSOCIATE ADMIN ROLE WITH ADMIN USERS (user_roles table)
-- =============================================
-- NOTE: Assumes ADMIN role is ID 3. User IDs are now 2, 4, 6, 8, 10.
INSERT INTO user_roles (user_id, role_id) VALUES
                                              (2, 3), -- admin_london -> ADMIN
                                              (4, 3), -- admin_manchester -> ADMIN
                                              (6, 3), -- admin_barcelona -> ADMIN
                                              (8, 3), -- admin_paris -> ADMIN
                                              (10, 3) -- admin_berlin -> ADMIN
ON CONFLICT (user_id, role_id) DO NOTHING;

-- =============================================
-- INSERT BREEDS (Based on image names)
-- =============================================
-- IDs generated by breed_id_sequence

-- CAT Breeds
INSERT INTO breed (id, name, specie, image_url) VALUES (1, 'Angora', 'CAT', 'images/avatars/pets/cat_angora.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (2, 'Black', 'CAT', 'images/avatars/pets/cat_black.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (3, 'Bombay', 'CAT', 'images/avatars/pets/cat_bombay.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (4, 'Common European', 'CAT', 'images/avatars/pets/cat_common_european.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (5, 'Himalayan', 'CAT', 'images/avatars/pets/cat_himalayan.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (6, 'Norwegian Forest', 'CAT', 'images/avatars/pets/cat_norwegian_forest.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (7, 'Persian', 'CAT', 'images/avatars/pets/cat_persian.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (8, 'Ragdoll', 'CAT', 'images/avatars/pets/cat_ragdoll.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (9, 'Russian Blue', 'CAT', 'images/avatars/pets/cat_russian_blue.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (10, 'Siamese', 'CAT', 'images/avatars/pets/cat_siamese.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (11, 'Siberian', 'CAT', 'images/avatars/pets/cat_siberian.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (12, 'Tabby', 'CAT', 'images/avatars/pets/cat_tabby.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (13, 'White', 'CAT', 'images/avatars/pets/cat_write.png') ON CONFLICT (name, specie) DO NOTHING;

-- DOG Breeds
INSERT INTO breed (id, name, specie, image_url) VALUES (14, 'Argentine Dogo', 'DOG', 'images/avatars/pets/dog_argentine_dogo.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (15, 'Beagle', 'DOG', 'images/avatars/pets/dog_beagle.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (16, 'Boxer', 'DOG', 'images/avatars/pets/dog_boxer.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (17, 'Bulldog', 'DOG', 'images/avatars/pets/dog_bulldog.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (18, 'Chihuahua', 'DOG', 'images/avatars/pets/dog_chihuahua.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (19, 'Dachshund', 'DOG', 'images/avatars/pets/dog_dachshund.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (20, 'Dalmatian', 'DOG', 'images/avatars/pets/dog_dalmatian.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (21, 'Doberman', 'DOG', 'images/avatars/pets/dog_doberman.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (22, 'French Bulldog', 'DOG', 'images/avatars/pets/dog_french_bulldog.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (23, 'German Shepherd', 'DOG', 'images/avatars/pets/dog_german_shepherd.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (24, 'Golden', 'DOG', 'images/avatars/pets/dog_golden.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (25, 'Golden Retriever', 'DOG', 'images/avatars/pets/dog_golden_retriever.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (26, 'Great Dane', 'DOG', 'images/avatars/pets/dog_great_dane.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (27, 'Labrador Retriever', 'DOG', 'images/avatars/pets/dog_labrador_retriever.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (28, 'Pekingese', 'DOG', 'images/avatars/pets/dog_pekingese.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (29, 'Pincher', 'DOG', 'images/avatars/pets/dog_pincher.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (30, 'Poodle', 'DOG', 'images/avatars/pets/dog_poodle.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (31, 'Pug', 'DOG', 'images/avatars/pets/dog_pug.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (32, 'Rottweiler', 'DOG', 'images/avatars/pets/dog_rottweiler.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (33, 'Saint Bernard', 'DOG', 'images/avatars/pets/dog_saint_bernard.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (34, 'Shih Tzu', 'DOG', 'images/avatars/pets/dog_shih_tzu.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (35, 'Siberian Husky', 'DOG', 'images/avatars/pets/dog_siberian_husky.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (36, 'Yorkshire Terrier', 'DOG', 'images/avatars/pets/dog_yorkshire_terrier.png') ON CONFLICT (name, specie) DO NOTHING;

-- FERRET Breeds/Types
INSERT INTO breed (id, name, specie, image_url) VALUES (37, 'Cinnamon', 'FERRET', 'images/avatars/pets/ferret_cinnamon.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (38, 'Sable Mask', 'FERRET', 'images/avatars/pets/ferret_sable_mask.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (39, 'Siamese', 'FERRET', 'images/avatars/pets/ferret_siamese.png') ON CONFLICT (name, specie) DO NOTHING;

-- RABBIT Breeds/Types
INSERT INTO breed (id, name, specie, image_url) VALUES (40, 'Brown', 'RABBIT', 'images/avatars/pets/rabbit_brown.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (41, 'Grey', 'RABBIT', 'images/avatars/pets/rabbit_grey.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (42, 'Lop', 'RABBIT', 'images/avatars/pets/rabbit_lop.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (43, 'White', 'RABBIT', 'images/avatars/pets/rabbit_white.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (44, 'Black', 'RABBIT', 'images/avatars/pets/rabbit_blak.png') ON CONFLICT (name, specie) DO NOTHING;

-- Generic 'Breed' entries
INSERT INTO breed (id, name, specie, image_url) VALUES (45, 'Mixed/Other', 'CAT', 'images/avatars/pets/cat.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (56, 'Mixed/Other', 'DOG', 'images/avatars/pets/dog.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (47, 'Mixed/Other', 'FERRET', 'images/avatars/pets/ferret.png') ON CONFLICT (name, specie) DO NOTHING;
INSERT INTO breed (id, name, specie, image_url) VALUES (48, 'Mixed/Other', 'RABBIT', 'images/avatars/pets/rabbit.png') ON CONFLICT (name, specie) DO NOTHING;

-- =============================================
-- UPDATE SEQUENCES
-- =============================================
SELECT setval('entity_id_sequence', COALESCE(GREATEST(
                                                     (SELECT MAX(id) FROM users),
                                                     (SELECT MAX(id) FROM clinic)
                                             ), 1), true);

SELECT setval('breed_id_sequence', COALESCE((SELECT MAX(id) FROM breed), 1), true);