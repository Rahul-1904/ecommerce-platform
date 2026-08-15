# ecommerce-backend

A REST API for an e-commerce platform, built with Spring Boot. Handles user
auth, product/category catalog management, shopping carts, and order
placement, with JWT-based stateless authentication and role-based
authorization (`CUSTOMER` / `ADMIN`).

A companion React admin panel (`ecommerce-admin-panel`) consumes this API to
manage products, categories, and orders.

## Tech stack

| Layer | Choice |
|---|---|
| Language / runtime | Java 17 |
| Framework | Spring Boot 3.3.4 |
| Persistence | Spring Data JPA + Hibernate |
| Database | PostgreSQL |
| Auth | Spring Security + JWT ([jjwt](https://github.com/jwtk/jjwt)) |
| Build | Maven |

## Features

- Registration/login issuing signed JWTs; passwords hashed with BCrypt
- Product & category catalog — public read access, admin-only writes
- Per-user shopping cart (add/update/remove items, stock-checked)
- Order placement from cart with stock decrement, and admin order-status updates
- Centralized validation and error handling with typed exceptions
- Pagination on product and order listings

## Getting started

### Prerequisites

- Java 17 (JDK)
- Maven 3.9+
- PostgreSQL running locally (or reachable) with a database created:
  ```sql
  CREATE DATABASE ecommerce_db;
  ```

### Configuration

The app reads its database credentials and JWT secret from environment
variables, falling back to local-dev defaults if unset (see
[`application.properties`](src/main/resources/application.properties)):

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ecommerce_db` | JDBC connection string |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `JWT_SECRET` | *(dev-only placeholder)* | HMAC signing key for JWTs — **set a real one outside local dev** |
| `JWT_EXPIRATION_MS` | `86400000` (24h) | Token lifetime |

For a persistent local override, add an `application-local.properties` file
(already git-ignored) instead of exporting environment variables each time.

### Run it

```bash
mvn clean install
mvn spring-boot:run
```

The API is served at `http://localhost:8080`. Schema is created/updated
automatically on startup via `spring.jpa.hibernate.ddl-auto=update` — no
manual migrations needed.

## API overview

All endpoints are under `/api`. Public endpoints need no token; everything
else requires `Authorization: Bearer <token>` from `/api/auth/login`.

| Endpoint | Method | Access |
|---|---|---|
| `/api/auth/register`, `/api/auth/login` | POST | public |
| `/api/products`, `/api/products/{id}` | GET | public |
| `/api/products`, `/api/products/{id}` | POST / PUT / DELETE | ADMIN |
| `/api/categories`, `/api/categories/{id}` | GET | public |
| `/api/categories`, `/api/categories/{id}` | POST / PUT / DELETE | ADMIN |
| `/api/cart` | GET / POST / PUT / DELETE | authenticated |
| `/api/orders` | GET / POST | authenticated (own orders only) |
| `/api/orders/{id}` | GET | authenticated (own orders, or any order as ADMIN) |
| `/api/orders/{id}/status` | PUT | ADMIN |

New registrations are always created as `CUSTOMER`. There is intentionally no
self-service way to become `ADMIN` — promote a user directly in the database:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'someone@example.com';
```

## Project structure

```
src/main/java/com/ecommerce/backend/
├── config/       Spring configuration (security filter chain, etc.)
├── controller/   REST endpoints — HTTP layer only
├── service/      Business logic and transaction boundaries
├── repository/   Spring Data JPA query interfaces
├── entity/       JPA-mapped domain classes (one per table)
├── dto/          Request/response records — the API's public shape
├── security/     JWT issuance, verification, user loading
└── exception/    Typed exceptions + centralized error handling
```

## Known limitations

- No endpoint lists orders across *all* customers — an admin can only act on
  a specific order by ID.
- No automated tests yet.
- No CORS configuration — a frontend hosted on a different origin in
  production will need one added.
