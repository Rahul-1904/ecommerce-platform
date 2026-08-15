# E-Commerce Platform

A full-stack e-commerce application: a Spring Boot REST API backend and a
React admin panel for managing it.

<p>
  <img src="docs/screenshots/products.png" alt="Admin panel — Products page" width="49%">
  <img src="docs/screenshots/categories.png" alt="Admin panel — Categories page" width="49%">
</p>

| | |
|---|---|
| **[`/backend`](backend)** | Spring Boot 3.3.4 · Java 17 · PostgreSQL · JWT auth · Swagger UI |
| **[`/frontend`](frontend)** | React 19 · Vite · admin panel for products/categories/orders |

## What it does

- **Auth** — registration/login issuing JWTs, BCrypt-hashed passwords,
  stateless sessions
- **Catalog** — products and categories, public to browse, admin-only to
  manage (via the admin panel or directly against the API)
- **Cart & orders** — per-user cart with stock checks, order placement,
  admin order-status updates
- **Role-based authorization** — `CUSTOMER` vs `ADMIN`, enforced at the API
  layer

## Architecture at a glance

```
Browser (React admin panel, :5173)
        │  /api/* proxied in dev
        ▼
Spring Boot API (:8080)
   Controller → Service → Repository → Entity
        │
        ▼
   PostgreSQL (ecommerce_db)
```

Every request that isn't `/api/auth/**` or a public `GET` on products/
categories passes through a JWT filter that verifies the token's signature
and rebuilds Spring Security's authorization context — there's no
server-side session store to check against.

## Running it locally

Both halves need to run at once. See each project's own README for full
detail:

1. **[Backend setup](backend/README.md)** — create the Postgres database,
   configure env vars if needed, `mvn spring-boot:run`
2. **[Frontend setup](frontend/README.md)** — `npm install && npm run dev`,
   with the backend already running

## Known limitations

See the backend and frontend READMEs for the full list — the headline ones:
no endpoint to list orders across all customers (admin can only act on a
specific order by ID), test coverage is backend service-layer only, and no
production CORS policy (the dev setup only avoids it via Vite's proxy).
