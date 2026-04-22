# Organizer Key Registration System

## Overview

The Organizer Key Registration System is a secure, admin-controlled workflow for creating organizer accounts in UniEvent. Instead of allowing anyone to self-register as an organizer, this system requires administrators to generate single-use invitation keys that can only be redeemed to create organizer accounts.

**Key Features:**
- ✅ Admin-only key generation with role verification
- ✅ Single-use keys with 24-hour expiration
- ✅ Confirmation token-based verification
- ✅ Cryptographically secure random key generation
- ✅ Email address validation at every step
- ✅ Hardcoded organizer role (cannot be escalated by client)

---

## Architecture

### User Registration Flows

```
┌─────────────────────────────────────────────────────────────────┐
│                    UniEvent User Registration                    │
└─────────────────────────────────────────────────────────────────┘

FLOW 1: Regular User Self-Registration
────────────────────────────────────────────────────
POST /api/auth/register
├─ username, email, password
├─ Role: ALWAYS "user" (hardcoded, no client input)
└─ Response: JWT tokens

FLOW 2: Organizer Registration (Admin-Controlled)
────────────────────────────────────────────────────
Step 1: Admin generates key
  POST /api/auth/organizer-key/generate
  ├─ Requires: @PreAuthorize("hasRole('ADMIN')")
  ├─ Input: email, organizationName
  ├─ Output: 32-char random key, 24-hour expiration
  └─ Key stored in database with admin ID

Step 2: Organizer verifies key
  POST /api/auth/organizer-key/verify
  ├─ No authentication required
  ├─ Input: key
  ├─ Validation: key exists, not expired, not already used
  └─ Output: confirmation token (10-minute JWT)

Step 3: Organizer completes registration
  POST /api/auth/register-with-key
  ├─ No authentication required
  ├─ Input: confirmationToken, username, password, email
  ├─ Validation: token valid, key not already redeemed
  ├─ Role: ALWAYS "organizer" (hardcoded)
  └─ Response: JWT tokens
```

---

## Security Mechanisms

### 1. Admin-Only Key Generation

**Endpoint:** `POST /api/auth/organizer-key/generate`

**Security Layer 1: Spring Security Annotation**
```java
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<GenerateOrganizerKeyResponse> generateOrganizerKey(...)
```

**How it works:**
1. Client sends request with admin's JWT token in `Authorization: Bearer <token>` header
2. [JwtAuthenticationFilter](src/main/java/dk/unievent/app/infrastructure/filter/JwtAuthenticationFilter.java) extracts the token and verifies signature
3. Extracts `roles` claim from JWT payload
4. Creates Spring Security `Authentication` object with extracted authorities
5. Spring Security framework checks `@PreAuthorize("hasRole('ADMIN')")` before method execution
6. If user doesn't have ADMIN role → **403 Forbidden**

**JWT Token Structure:**
```json
{
  "jti": "unique-token-id",
  "sub": "admin@example.com",
  "type": "access",
  "iat": 1713229415,
  "exp": 1713230315,
  "roles": ["ADMIN"]  // ← This is checked by @PreAuthorize
}
```

---

### 2. Single-Use Keys with Expiration

**Entity:** [OrganizerKeyEntity](src/main/java/dk/unievent/app/db/model/OrganizerKeyEntity.java)

**Fields:**
- `keyValue`: 32-character random alphanumeric string (cryptographically secure)
- `email`: Target email for organizer account (immutable)
- `createdBy`: Admin user ID who generated the key
- `expiresAt`: Instant (24 hours from creation)
- `usedAt`: Instant or null (tracks if key has been redeemed)

**Validation in `OrganizerKeyService.verifyOrganizerKey()`:**
```java
// Check 1: Key exists
OrganizerKeyEntity keyEntity = organizerKeyRepository.findByKeyValue(keyValue)
    .orElseThrow(() -> new IllegalArgumentException("Invalid organizer key"));

// Check 2: Not already used
if (keyEntity.getUsedAt() != null) {
    throw new IllegalStateException("Organizer key has already been used");
}

// Check 3: Not expired
if (Instant.now().isAfter(keyEntity.getExpiresAt())) {
    throw new IllegalStateException("Organizer key has expired");
}
```

---

### 3. Confirmation Token Verification

**Why needed:** Prevent replay attacks where someone intercepts the key and tries to register with different credentials.

**Process:**
1. After key verification → generate JWT confirmation token (10-minute expiration)
2. Confirmation token contains: `keyId`, `email`, `type="organizer-registration-confirmation"`
3. Token is signed with server's JWT secret
4. Token is returned to client (stored temporarily)
5. During registration, token is re-validated to ensure:
   - Signature is valid (hasn't been tampered with)
   - Token hasn't expired (< 10 minutes old)
   - Token type is correct
   - Key ID still exists and hasn't been redeemed

---

### 4. Hardcoded Organizer Role

**Endpoint:** `POST /api/auth/register-with-key`

**DTO Definition:**
```java
public record OrganizerRegisterWithKeyRequest(
    @NotBlank String confirmationToken,
    @NotBlank @Size(3-50) String username,
    @NotBlank @Size(12-100) String password,
    @NotBlank String email
)
// NOTE: No "role" field - client cannot specify it
```

**Server-Side Role Assignment:**
```java
UserEntity organizer = UserEntity.builder()
    .username(username)           // ← from client
    .email(email)                 // ← from client
    .password(passwordEncoder.encode(password))  // ← from client
    .role("organizer")            // ← HARDCODED - client cannot override!
    .build();
```

**Why this is secure:**
- Confirmation token contains the keyId
- KeyId is used to load the original key from database
- Server verifies email matches original key's email
- Role is set by server logic, not by client input
- Even if JSON payload includes `"role": "admin"`, it's ignored during deserialization

---

### 5. Regular User Registration Protection

**Original Issue:** The `/register` endpoint allowed anyone to self-register as an organizer.

**Original Vulnerable Code:**
```java
// BEFORE - VULNERABLE
if ("user".equalsIgnoreCase(role) || "organizer".equalsIgnoreCase(role)) {
    return role.toLowerCase();  // ← Anyone could become an organizer!
}
```

**Fixed Code:**
```java
// AFTER - SECURE
public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    // No role parameter - hardcode "user"
    UserEntity user = userService.register(
        new UserDTO(request.username(), request.email(), request.password(), "user")
    );
    // ...
}

// In validateRegistrationRole():
if ("organizer".equalsIgnoreCase(role)) {
    throw new IllegalArgumentException(
        "Organizer role cannot be self-registered. Contact an admin for an invitation key."
    );
}
```

**Updated DTO (no role field):**
```java
public record RegisterRequest(
    @NotBlank @Size(3-50) String username,
    @NotBlank @Email String email,
    @NotBlank @Size(12-100) String password
    // No role field!
) {}
```

---

## API Endpoints

### 1. Generate Organizer Key (Admin Only)

```http
POST /api/auth/organizer-key/generate
Authorization: Bearer <admin-jwt-token>
Content-Type: application/json

{
  "email": "organizer@example.com",
  "organizationName": "My Event Venue"
}
```

**Response (200 OK):**
```json
{
  "message": "Invitation key has been sent to organizer@example.com",
  "expiresIn": 86400
}
```

**Possible Responses:**
- `200 OK` - Key successfully generated
- `400 Bad Request` - Invalid request body
- `401 Unauthorized` - No JWT token provided
- `403 Forbidden` - JWT token doesn't have ADMIN role

---

### 2. Verify Organizer Key

```http
POST /api/auth/organizer-key/verify
Content-Type: application/json

{
  "key": "aBcDeFgHiJkLmNoPqRsTuVwXyZ123456"
}
```

**Response (200 OK):**
```json
{
  "confirmationToken": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI...",
  "expiresIn": 600,
  "email": "organizer@example.com"
}
```

**Possible Responses:**
- `200 OK` - Key verified, confirmation token issued
- `400 Bad Request` - Invalid request body
- `404 Not Found` - Key not found or invalid
- `410 Gone` - Key already used
- `401 Unauthorized` - Key expired

---

### 3. Register with Confirmation Token

```http
POST /api/auth/register-with-key
Content-Type: application/json

{
  "confirmationToken": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI...",
  "username": "john_organizer",
  "password": "SecurePassword123!@#",
  "email": "organizer@example.com"
}
```

**Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI...",
  "refreshToken": "eyJhbGciOiJIUzM4NCJ9.eyJqdGkiOiI...",
  "username": "john_organizer",
  "email": "organizer@example.com",
  "accessTokenExpiresInMs": 900000,
  "refreshTokenExpiresInMs": 604800000
}
```

**Possible Responses:**
- `201 Created` - Organizer registered successfully
- `400 Bad Request` - Invalid request body or password too weak
- `401 Unauthorized` - Invalid or expired confirmation token
- `409 Conflict` - Username or email already exists
- `422 Unprocessable Entity` - Key already redeemed

---

## Data Flow Example

```
1. ADMIN GENERATES KEY
   ────────────────────
   Admin (JWT: roles=[ADMIN])
        │
        ├─→ POST /api/auth/organizer-key/generate
        │   {email: "jane@org.com", organizationName: "Venue Co"}
        │
        └─→ [AuthController]
            ├─ Check: @PreAuthorize("hasRole('ADMIN')") ✓
            ├─ Extract admin ID from JWT
            ├─ Generate random key: "aB3cD7fG9iK2lM4nP6rS8tU0vW2xY4z6"
            ├─ Save to DB: OrganizerKeyEntity
            │   {keyValue, email="jane@org.com", createdBy=admin_id, expiresAt=now+24h, usedAt=null}
            │
            └─→ Response: {message: "...", expiresIn: 86400}


2. ORGANIZER VERIFIES KEY
   ──────────────────────
   Organizer (no JWT)
        │
        ├─→ POST /api/auth/organizer-key/verify
        │   {key: "aB3cD7fG9iK2lM4nP6rS8tU0vW2xY4z6"}
        │
        └─→ [OrganizerKeyService.verifyOrganizerKey()]
            ├─ Query DB for key
            ├─ Validate: not used, not expired
            ├─ Generate JWT confirmation token
            │   {type: "organizer-registration-confirmation", keyId: 123, email: "jane@org.com"}
            │
            └─→ Response: {confirmationToken: "...", expiresIn: 600, email: "jane@org.com"}


3. ORGANIZER REGISTERS
   ────────────────────
   Organizer (no JWT)
        │
        ├─→ POST /api/auth/register-with-key
        │   {confirmationToken: "...", username: "jane_organizer", 
        │    password: "SecurePass123!@#", email: "jane@org.com"}
        │
        └─→ [OrganizerKeyService.completeOrganizerRegistration()]
            ├─ Validate confirmation token
            ├─ Query DB: OrganizerKeyEntity by keyId
            ├─ Verify email matches
            ├─ Check key not already used
            ├─ Create UserEntity
            │   {username: "jane_organizer", email: "jane@org.com", 
            │    password: bcrypt("SecurePass123!@#"), role: "organizer"}
            ├─ Save user to DB
            ├─ Mark key as used: keyEntity.usedAt = now
            ├─ Issue JWT tokens
            │   {roles: ["organizer"], sub: "jane@org.com"}
            │
            └─→ Response: {token: "...", refreshToken: "...", ...}


4. ORGANIZER USES ACCOUNT
   ──────────────────────
   Organizer (JWT: roles=[organizer])
        │
        ├─→ Any authenticated endpoint
        │   Authorization: Bearer <token>
        │
        └─→ [JwtAuthenticationFilter]
            ├─ Extract token
            ├─ Verify signature ✓
            ├─ Extract roles: ["organizer"]
            ├─ Create Authentication with authority: "ROLE_organizer"
            │
            └─→ Endpoint can check: @PreAuthorize("hasRole('ORGANIZER')")
```

---

## Database Schema

### OrganizerKeyEntity
```sql
CREATE TABLE organizer_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key_value VARCHAR(32) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_by BIGINT NOT NULL,          -- Admin user ID
    expires_at TIMESTAMP NOT NULL,        -- 24 hours from creation
    used_at TIMESTAMP NULL,               -- NULL until redeemed
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    FOREIGN KEY (created_by) REFERENCES user(id),
    INDEX idx_key_value (key_value),
    INDEX idx_email (email),
    INDEX idx_expires_at (expires_at)
);
```

---

## Security Considerations

### ✅ What's Protected

| Attack | Protection |
|--------|-----------|
| Non-admin generating keys | `@PreAuthorize("hasRole('ADMIN')")` JWT validation |
| Self-registering as organizer | `validateRegistrationRole()` rejects organizer for `/register` |
| Using expired key | `Instant.now().isAfter(expiresAt)` check |
| Reusing key twice | `usedAt != null` check |
| Email mismatch | Email comparison between key and registration request |
| Confirmation token replay | JWT signature validation + 10-minute expiration |
| Privilege escalation in token | Client cannot control role field in DTO |
| Key guessing | 32-character cryptographically random (2^190 combinations) |

### ⚠️ Future Enhancements

1. **Email Verification:** Send invitation link via email instead of exposing raw key
2. **Rate Limiting:** Limit key generation attempts per admin
3. **Audit Logging:** Log all key generation and registration events
4. **Key Revocation:** Allow admins to revoke unused keys
5. **Organization Association:** Link keys and organizers to organizations in database
6. **Email Delivery:** Integrate email service for sending invitation keys

---

## Configuration

**Environment Variables** (in `.env`):
```bash
UNIEVENT_ORGANIZER_KEY_EXPIRATION_HOURS=24
UNIEVENT_ORGANIZER_KEY_CONFIRMATION_TOKEN_EXPIRATION_MINUTES=10
```

**Default Values** (in `application.yaml`):
```yaml
unievent:
  organizer-key:
    expiration-hours: ${UNIEVENT_ORGANIZER_KEY_EXPIRATION_HOURS:24}
    confirmation-token-expiration-minutes: ${UNIEVENT_ORGANIZER_KEY_CONFIRMATION_TOKEN_EXPIRATION_MINUTES:10}
```

---

## Testing

**Test Script:** [test-organizer-key.ps1](test-organizer-key.ps1)

Runs the complete workflow:
1. Login as admin
2. Generate organizer key
3. Verify key
4. Register organizer account
5. Verify new account can login

```bash
./test-organizer-key.ps1
```

---

## Implementation Files

### Core Services
- [OrganizerKeyService](src/main/java/dk/unievent/app/application/service/OrganizerKeyService.java) - Business logic
- [OrganizerKeyRepository](src/main/java/dk/unievent/app/db/repository/OrganizerKeyRepository.java) - Database queries
- [UserService](src/main/java/dk/unievent/app/application/service/UserService.java) - User management & validation

### Controllers & DTOs
- [AuthController](src/main/java/dk/unievent/app/api/controller/AuthController.java) - Endpoints
- [OrganizerKeyEntity](src/main/java/dk/unievent/app/db/model/OrganizerKeyEntity.java) - JPA entity
- Request/Response DTOs in `src/main/java/dk/unievent/app/api/dto/`

### Security
- [JwtAuthenticationFilter](src/main/java/dk/unievent/app/infrastructure/filter/JwtAuthenticationFilter.java) - Token extraction
- [SecurityConfig](src/main/java/dk/unievent/app/infrastructure/config/SecurityConfig.java) - Filter chain

---

## Summary

The Organizer Key System provides **multi-layered security** for organizer account creation:

1. **Layer 1:** JWT token validation with role claims (`@PreAuthorize`)
2. **Layer 2:** Database schema preventing reuse and enforcing expiration
3. **Layer 3:** Email validation ensuring key recipients are registered with correct email
4. **Layer 4:** Confirmation token preventing replay attacks
5. **Layer 5:** Server-side role hardcoding preventing client-side escalation
6. **Layer 6:** Removal of role field from regular registration DTO

This ensures that **only authorized admins can create organizers**, and **organizers can only be created through the designated flow**.
