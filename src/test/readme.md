## 🧪 Testing

This project includes **integration tests** (`TodoAppIntegrationTest`) that verify the full flow:

- **Auth Tests**
    - Register new user
    - Login with JWT cookie
    - Logout (cookie cleared)
    - Invalid login → 401 Unauthorized with JSON error

- **Task Tests (Basic)**
    - Create a task
    - Fetch tasks (user only sees their own)
    - Admin sees all tasks across users

- **Task Tests (Security)**
    - Update task:
        - ✅ Owner can update
        - ❌ Non-owner cannot (403 Forbidden)
        - ✅ Admin can update any task
    - Delete task:
        - ❌ Normal user cannot delete (403 Forbidden)
        - ✅ Admin can delete any task

### Run all tests

```bash
mvn clean test

```
## Run only the integration tests
mvn -Dtest=TodoAppIntegrationTest test

## Example Output
[INFO] Running com.example.todoapp.TodoAppIntegrationTest
[INFO] Tests run: 7, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS

## Notes
- Tests use H2 in-memory database (no need for PostgreSQL running).
- Tests use Spring Security’s CSRF + MockMvc to simulate real requests.
- JWT is stored in an HttpOnly cookie during tests, just like in production.

