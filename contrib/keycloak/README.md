# Keycloak

## Authentication Flow

```
[ User Browser ]
       |
       | 1. Accesses Frontend App
       v
[ React App (keycloak-js) ]
       |
       | 2. Check Authentication (check-sso)
       +---> [ Not Authenticated ] ---> Redirect to Keycloak Login Screen
       |                                (Registration / Reset allowed if enabled)
       |
       | 3. User Logged In
       v
[ Keycloak Issues JWT Token ]
       |
       | 4. Frontend receives Access Token
       | 5. Global fetch interceptor injects header on /api/*:
       |    Authorization: Bearer <keycloak.token>
       v
[ Quarkus Backend (quarkus-oidc) ]
       |
       | 6. Validates JWT Signature & Claims
       | 7. JIT Provisioning & Role Mapping via UserProvisioningService
       v
[ PostgreSQL DB (users table) ]
```

### Detailed Flow Steps:

1. **Accessing the App**: The user accesses the React frontend application URL.
2. **SSO Check**: The React app initializes `keycloak-js` on load using `onLoad: "check-sso"`.
3. **Redirection to Keycloak**: If the user is not authenticated, visiting `/login` or protected areas triggers `keycloak.login()`, redirecting them to Keycloak. If user registration is enabled in Keycloak realm settings, users can also register a new account on this page.
4. **Token Issuance**: After successful authentication, Keycloak redirects the user back to the React app with a valid JWT access token.
5. **Automatic Token Attachment**: In `AuthProvider.tsx`, a global `window.fetch` interceptor automatically appends the Bearer token to all local API requests:
   ```typescript
   headers.set("Authorization", `Bearer ${keycloak.token}`);
   ```
6. **Background Token Refresh**: A 15-second interval loop continuously runs `keycloak.updateToken(30)` to refresh tokens before expiration without disturbing user experience.

---

## User Synchronization & Just-In-Time (JIT) Provisioning

### The Challenge
When a user registers or logs in via Keycloak, their account exists in Keycloak's database, but does not yet exist in the application's PostgreSQL `users` table.

### The Solution: Just-In-Time (JIT) Provisioning

#### **In Frontend**
On each session load, the frontend fetches application state and session data:
```typescript
GET /api/app/session
Headers: {
  Authorization: "Bearer " + keycloak.token
}
```

#### **In Backend**
1. **Token Extraction**: Quarkus `quarkus-oidc` validates the incoming Bearer JWT token.
2. **Context Resolution (`CurrentUser.java`)**: The JAX-RS endpoint invokes `@Inject CurrentUser currentUser`.
3. **JIT Synchronization (`UserProvisioningService.java`)**:
   * Reads JWT claims: `sub` (`keycloakId`), `email`, `preferred_username`, `name` (`fullName`), and `realm_access.roles`.
   * Searches the database for an existing user matching `keycloakId` (or `email`).
   * **If the user exists**: Updates/syncs their profile fields and maps Keycloak realm roles (`admin`, `support`, `tam`, `superuser`, `user`) to internal application types (`User.type`).
   * **If the user does NOT exist**: Automatically provisions and persists a new `User` record in PostgreSQL.
4. **Request Caching**: The `@RequestScoped` `CurrentUser` bean caches the resolved user for the lifetime of the request, eliminating duplicate database lookups.

---

## Configuration

Authentication behavior can be customized via Keycloak realm settings in `contrib/keycloak/.env-keycloak`:

### 1. Realm Feature Options
* **User Registration**: `KEYCLOAK_ENABLE_REGISTRATION=true` (Enables or disables self-registration on Keycloak's login page).
* **Email Verification**: `KEYCLOAK_ENABLE_VERIFY_EMAIL=true`.
* **Password Reset**: `KEYCLOAK_ENABLE_RESET_PASSWORD=true`.

> [!NOTE]
> Enabling **Email Verification** or **Password Reset** requires configuring valid SMTP server credentials in `.env-keycloak`:
> ```env
> KEYCLOAK_MAIL_HOST=smtp.example.com
> KEYCLOAK_MAIL_PORT=587
> KEYCLOAK_MAIL_FROM=no-reply@example.com
> KEYCLOAK_MAIL_USERNAME=example@example.com
> KEYCLOAK_MAIL_PASSWORD="your-password"
> KEYCLOAK_MAIL_START_TLS=true
> ```

### 2. Backend OIDC Properties (`src/backend/main/resources/application.properties`)
```properties
quarkus.oidc.auth-server-url=${KEYCLOAK_SERVER_URL:http://localhost:8180}/realms/${KEYCLOAK_REALM_NAME:billetsys}
quarkus.oidc.client-id=${KEYCLOAK_BACKEND_CLIENT_ID:billetsys-backend}
quarkus.oidc.credentials.secret=${KEYCLOAK_BACKEND_CLIENT_SECRET:123456}
quarkus.oidc.application-type=service
quarkus.oidc.roles.role-claim-path=realm_access/roles
```

### 3. Frontend Client Configuration (`src/frontend/src/auth/keycloak.ts`)
```typescript
const keycloak = new Keycloak({
  url: import.meta.env.VITE_KEYCLOAK_URL || "http://localhost:8180",
  realm: import.meta.env.VITE_KEYCLOAK_REALM || "billetsys",
  clientId: import.meta.env.VITE_KEYCLOAK_CLIENT_ID || "billetsys-frontend",
});
```

---

## Running the keycloak

### 1. Set up Environment & Generate Realm
```bash
cd contrib/keycloak
cp .env-keycloak.example .env-keycloak
python generate_realm.py
cd ../..
```

### 2. Start Services via Docker Compose
```bash
docker compose up -d keycloak
```

Access keycloak at `http://localhost:8180`.

---

## Helper Scripts & Testing

```bash
# 1. Create a Keycloak user
./contrib/keycloak/create-user.sh <username> <email> <password> <role> <firstName> <lastName>

# 2. Get a JWT token
./contrib/keycloak/login.sh <username> <password>

# 3. Call REST API endpoint with Bearer token
TOKEN=$(./contrib/keycloak/login.sh user1 user1)
curl -s "http://localhost:8080/api/user/tickets" -H "Authorization: Bearer $TOKEN" | jq .
```

### Role → Endpoint Mapping Reference

| Role | Access Level | Example Protected Endpoint |
| :--- | :--- | :--- |
| **`admin`** | Administrator | `GET /api/admin/users`, `GET /api/companies` |
| **`support`** | Support Engineer | `GET /api/support/tickets`, `GET /api/support/users` |
| **`superuser`** | Superuser | `GET /api/superuser/tickets`, `GET /api/superuser/users` |
| **`tam`** | Technical Account Manager | `GET /api/user/tickets`, `GET /api/tam/users` |
| **`user`** | Standard User | `GET /api/user/tickets`, `GET /api/user/externals` |
| **`any / none`** | Public | `GET /api/app/session`, `GET /health` |
