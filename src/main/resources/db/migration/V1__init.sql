-- Drop old tables if they exist
DROP TABLE IF EXISTS tasks CASCADE;
DROP TABLE IF EXISTS users CASCADE;

-- Users table (with single role column)
CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(255) NOT NULL
);

-- Tasks table
CREATE TABLE tasks (
    id SERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    done BOOLEAN DEFAULT FALSE,
    user_id INT NOT NULL,
    CONSTRAINT fk_task_owner FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Default admin user (password = "admin")
INSERT INTO users (username, password, roles)
VALUES (
  'admin',
  '$2a$10$Dow1OtMT2EHpM3YcP/Ej7uKw2D/8nYwWqB7Y7puQJbGJfVj2E.Q9u',
  'ROLE_ADMIN'
);
