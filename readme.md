# TodoApp

A full-stack Todo application with:

Spring Boot backend (/auth, /tasks)

React + Vite frontend

Postgres database

Flyway for migrations

Docker Compose for deployment

### 1. Start Postgres

docker compose up db

### 2. Start Backend

Run Spring Boot app locally (outside Docker):

source .env.dev && ./mvnw spring-boot:run

#### 3. Start Frontend

Inside todo-frontend/

npm install
npm run dev

By default, Vite runs at http://localhost:5173
.
The Vite proxy (vite.config.js) ensures calls like /auth/login are forwarded to http://localhost:8080.

### Production (Docker Compose)

Everything runs in containers:

docker compose --env-file .env.prod up --build

Services

Frontend (Nginx) → http://localhost

Backend (Spring Boot) → http://backend:8080
(internal)

Database (Postgres) → db:5432 (internal)

### Networking

- Frontend only talks to backend via Nginx proxy.

- Backend only talks to Postgres.

### Nginx config

location /auth {
proxy_pass http://backend:8080;
}
location /tasks {
proxy_pass http://backend:8080;
}

### Notes
- Use VITE_API_URL only if running frontend without proxy.

- JWT secrets & DB creds are set via environment variables in docker-compose.yml.

- For schema updates, add Flyway migrations under backend/src/main/resources/db/migration.