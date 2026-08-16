# Eternal Protocol

Athlete-branded merchandise storefront, a full-stack e-commerce platform where
customers shop for products, optionally checking out under an athlete's referral
code (which earns that athlete a commission), and admins manage the catalog, orders,
athletes, and commission payouts.

![ ](git-content/[]home-page-ss.png)

## Repository Layout

```
eternal-protocol/
├── backend/    Spring Boot REST API (Java 17)        → see backend/README.md
└── frontend/   React + TypeScript + Vite storefront    → see frontend/README.md
```

Each half has its own detailed README covering setup, running, testing, and
environment/credential configuration.

## Tech stack at a glance

| | |
|---|---|
| **Backend** | Java 17, Spring Boot 3.3.4, Spring Web/Data JPA/Security, JWT auth, H2 (dev) / MySQL (persistent), PayHere sandbox checkout, Cloudinary image hosting |
| **Frontend** | React 19, TypeScript, Vite, Tailwind CSS v4, Zustand, TanStack Query, React Router |

## Quick start (both services)

You need both running at once for the app to work end-to-end.

```bash
# Terminal 1, backend, defaults to an in-memory DB, no install needed
cd backend
cp .env.example .env      # fill in credentials, see backend/README.md
mvn spring-boot:run       # → http://localhost:8080

# Terminal 2, frontend
cd frontend
npm install
cp .env.example .env.local
npm run dev                # → http://localhost:5173
```

To get the full picture, check out:

- **[`backend/README.md`](./backend/README.md)**
- **[`frontend/README.md`](./frontend/README.md)**
- **[`backend/BACKEND_API_SPEC.md`](./backend/BACKEND_API_SPEC.md)**

