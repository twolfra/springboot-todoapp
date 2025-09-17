-- Users table
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL
);

-- Roles table (managed by @ElementCollection)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(255) NOT NULL,
    CONSTRAINT fk_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Default admin user
INSERT INTO users (username, password) VALUES (
  'admin',
  '$2a$10$Dow1OtMT2EHpM3YcP/Ej7uKw2D/8nYwWqB7Y7puQJbGJfVj2E.Q9u' -- "admin"
);

INSERT INTO user_roles (user_id, role)
SELECT id, 'ROLE_ADMIN' FROM users WHERE username = 'admin';
